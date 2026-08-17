package com.wikzo.todo.data.repository

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.MetadataChanges
import com.wikzo.todo.data.model.Priority
import com.wikzo.todo.data.model.Task
import com.wikzo.todo.notifications.ReminderScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A snapshot of the task list plus its sync state, straight from Firestore's
 * listener metadata -- lets the UI show an "offline" / "syncing" indicator
 * without the offline-first behavior being a silent, invisible cache.
 */
data class TasksSnapshot(
    val tasks: List<Task>,
    val isFromCache: Boolean,
    val hasPendingWrites: Boolean,
)

/**
 * Firestore CRUD against `syncGroups/{groupId}/tasks`, per /docs/data-model.md.
 *
 * Firestore's automatic POJO mapping (`toObject()`) doesn't fit [Task] cleanly: the
 * document id isn't a stored field, and `priority` is stored as a lowercase string
 * ("high"/"medium"/"low") rather than the enum constant name Firestore's default enum
 * mapping would expect. So reads/writes are mapped by hand via [toTask] and the
 * per-method field maps below, using [Priority.firestoreValue] / [Priority.fromFirestoreValue].
 *
 * This is also the single choke point for every task mutation (add/update/complete/
 * delete), which is deliberately where local due-date reminders are kept in sync via
 * [ReminderScheduler] -- rather than duplicating that bookkeeping in every ViewModel
 * that can change a task, one call site covers the add-screen save, the list screen's
 * checkbox toggle, and delete alike. [Context] is injected the same way
 * [com.wikzo.todo.data.local.DeviceGroupStore] already does it elsewhere in this
 * layer, via Hilt's `@ApplicationContext`.
 */
@Singleton
class TaskRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    @ApplicationContext private val context: Context,
) {

    private fun tasksCollection(groupId: String) =
        firestore.collection("syncGroups").document(groupId).collection("tasks")

    /**
     * Live view of every task in the group plus sync metadata, updated on every
     * remote/local change (including local writes that haven't reached the server
     * yet, and the initial emission from the offline cache before a listener is
     * established with the backend).
     */
    fun observeTasks(groupId: String): Flow<TasksSnapshot> = callbackFlow {
        val registration = tasksCollection(groupId)
            .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    trySend(
                        TasksSnapshot(
                            tasks = snapshot.documents.map { it.toTask() },
                            isFromCache = snapshot.metadata.isFromCache,
                            hasPendingWrites = snapshot.metadata.hasPendingWrites(),
                        ),
                    )
                }
            }
        awaitClose { registration.remove() }
    }

    /** One-shot fetch of a single task, used by the edit screen to seed its form. */
    suspend fun getTask(groupId: String, taskId: String): Task? {
        val snapshot = tasksCollection(groupId).document(taskId).get().await()
        return if (snapshot.exists()) snapshot.toTask() else null
    }

    /**
     * One-shot fetch of every task in the group -- as opposed to [observeTasks]'s
     * live listener -- used by [com.wikzo.todo.notifications.ReminderReconcileWorker],
     * which runs headless and briefly and has no need to stay subscribed.
     */
    suspend fun getAllTasks(groupId: String): List<Task> {
        val snapshot = tasksCollection(groupId).get().await()
        return snapshot.documents.map { it.toTask() }
    }

    suspend fun addTask(groupId: String, task: Task) {
        val uid = auth.currentUser?.uid.orEmpty()
        val data = hashMapOf<String, Any?>(
            "title" to task.title,
            "notes" to task.notes,
            "completed" to task.completed,
            "completedAt" to if (task.completed) FieldValue.serverTimestamp() else null,
            "dueDate" to task.dueDate,
            "priority" to task.priority.firestoreValue,
            "order" to task.order,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
            "createdByUid" to uid,
            "updatedByUid" to uid,
        )
        val docRef = tasksCollection(groupId).add(data).await()
        rescheduleReminder(task.copy(id = docRef.id))
    }

    suspend fun updateTask(groupId: String, task: Task) {
        require(task.id.isNotBlank()) { "Cannot update a task without an id" }
        val uid = auth.currentUser?.uid.orEmpty()
        val data = hashMapOf<String, Any?>(
            "title" to task.title,
            "notes" to task.notes,
            "completed" to task.completed,
            "completedAt" to task.completedAt,
            "dueDate" to task.dueDate,
            "priority" to task.priority.firestoreValue,
            "order" to task.order,
            "updatedAt" to FieldValue.serverTimestamp(),
            "updatedByUid" to uid,
        )
        tasksCollection(groupId).document(task.id).update(data).await()
        rescheduleReminder(task)
    }

    suspend fun deleteTask(groupId: String, taskId: String) {
        tasksCollection(groupId).document(taskId).delete().await()
        ReminderScheduler.cancelReminder(context, taskId)
    }

    suspend fun setCompleted(groupId: String, task: Task, completed: Boolean) {
        val uid = auth.currentUser?.uid.orEmpty()
        val data = hashMapOf<String, Any?>(
            "completed" to completed,
            "completedAt" to if (completed) FieldValue.serverTimestamp() else null,
            "updatedAt" to FieldValue.serverTimestamp(),
            "updatedByUid" to uid,
        )
        tasksCollection(groupId).document(task.id).update(data).await()
        rescheduleReminder(task.copy(completed = completed))
    }

    /**
     * Keeps [task]'s local reminder alarm in sync with its just-written state:
     * cancels it if the task is completed or has no (or a past) due date,
     * otherwise (re)schedules it -- [ReminderScheduler.scheduleReminder] uses the
     * same request code every time, so this transparently replaces any
     * previously-scheduled alarm for the same task rather than stacking one.
     */
    private fun rescheduleReminder(task: Task) {
        val dueMillis = task.dueDate?.toDate()?.time
        if (task.completed || dueMillis == null || dueMillis <= System.currentTimeMillis()) {
            ReminderScheduler.cancelReminder(context, task.id)
        } else {
            ReminderScheduler.scheduleReminder(context, task.id, task.title, dueMillis)
        }
    }

    private fun DocumentSnapshot.toTask(): Task = Task(
        id = id,
        title = getString("title") ?: "",
        notes = getString("notes"),
        completed = getBoolean("completed") ?: false,
        completedAt = getTimestamp("completedAt"),
        dueDate = getTimestamp("dueDate"),
        priority = Priority.fromFirestoreValue(getString("priority")),
        order = getDouble("order") ?: 0.0,
        createdAt = getTimestamp("createdAt"),
        updatedAt = getTimestamp("updatedAt"),
        createdByUid = getString("createdByUid") ?: "",
        updatedByUid = getString("updatedByUid") ?: "",
    )
}
