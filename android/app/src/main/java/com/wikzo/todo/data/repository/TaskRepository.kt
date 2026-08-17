package com.wikzo.todo.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.wikzo.todo.data.model.Priority
import com.wikzo.todo.data.model.Task
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firestore CRUD against `syncGroups/{groupId}/tasks`, per /docs/data-model.md.
 *
 * Firestore's automatic POJO mapping (`toObject()`) doesn't fit [Task] cleanly: the
 * document id isn't a stored field, and `priority` is stored as a lowercase string
 * ("high"/"medium"/"low") rather than the enum constant name Firestore's default enum
 * mapping would expect. So reads/writes are mapped by hand via [toTask] and the
 * per-method field maps below, using [Priority.firestoreValue] / [Priority.fromFirestoreValue].
 */
@Singleton
class TaskRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
) {

    private fun tasksCollection(groupId: String) =
        firestore.collection("syncGroups").document(groupId).collection("tasks")

    /** Live view of every task in the group, updated on every remote/local change. */
    fun observeTasks(groupId: String): Flow<List<Task>> = callbackFlow {
        val registration = tasksCollection(groupId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.documents.orEmpty().map { it.toTask() })
            }
        awaitClose { registration.remove() }
    }

    /** One-shot fetch of a single task, used by the edit screen to seed its form. */
    suspend fun getTask(groupId: String, taskId: String): Task? {
        val snapshot = tasksCollection(groupId).document(taskId).get().await()
        return if (snapshot.exists()) snapshot.toTask() else null
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
        tasksCollection(groupId).add(data).await()
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
    }

    suspend fun deleteTask(groupId: String, taskId: String) {
        tasksCollection(groupId).document(taskId).delete().await()
    }

    suspend fun setCompleted(groupId: String, taskId: String, completed: Boolean) {
        val uid = auth.currentUser?.uid.orEmpty()
        val data = hashMapOf<String, Any?>(
            "completed" to completed,
            "completedAt" to if (completed) FieldValue.serverTimestamp() else null,
            "updatedAt" to FieldValue.serverTimestamp(),
            "updatedByUid" to uid,
        )
        tasksCollection(groupId).document(taskId).update(data).await()
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
