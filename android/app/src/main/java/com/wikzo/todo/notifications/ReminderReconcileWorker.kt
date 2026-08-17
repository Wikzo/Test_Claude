package com.wikzo.todo.notifications

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.wikzo.todo.data.local.DeviceGroupStore
import com.wikzo.todo.data.repository.TaskRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * Periodically re-syncs local exact alarms against Firestore's current state
 * of tasks in this device's sync group.
 *
 * This is what catches a due-date edit (or a new task, or a completion) made
 * from another device -- e.g. the web app -- while this device's app wasn't in
 * the foreground to see the live Firestore listener update. It's a one-shot
 * fetch via [TaskRepository.getAllTasks], not the live [TaskRepository.observeTasks]
 * listener, since a Worker runs headless and briefly.
 *
 * Idempotent by construction: [ReminderScheduler.scheduleReminder] uses the
 * same PendingIntent request code for a given task every time, so
 * re-scheduling a task that already has a pending alarm just replaces it.
 * Safe to call for every incomplete task on every run.
 */
@HiltWorker
class ReminderReconcileWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val taskRepository: TaskRepository,
    private val deviceGroupStore: DeviceGroupStore,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val groupId = deviceGroupStore.groupId.first() ?: return Result.success()

        val tasks = try {
            taskRepository.getAllTasks(groupId)
        } catch (e: Exception) {
            return Result.retry()
        }

        val now = System.currentTimeMillis()
        tasks.forEach { task ->
            val dueMillis = task.dueDate?.toDate()?.time
            if (!task.completed && dueMillis != null && dueMillis > now) {
                ReminderScheduler.scheduleReminder(
                    context = applicationContext,
                    taskId = task.id,
                    title = task.title,
                    triggerAtMillis = dueMillis,
                )
            }
        }

        return Result.success()
    }

    companion object {
        private const val UNIQUE_PERIODIC_WORK_NAME = "reminder_reconcile_periodic"
        private const val IMMEDIATE_WORK_NAME = "reminder_reconcile_immediate"
        private const val PERIODIC_INTERVAL_MINUTES = 15L

        /**
         * Enqueues the ~15-minute periodic reconciliation pass. `KEEP` makes
         * this safe to call on every app start (see TodoApplication.onCreate)
         * without disturbing an already-scheduled run's timing.
         */
        fun enqueuePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<ReminderReconcileWorker>(
                PERIODIC_INTERVAL_MINUTES, TimeUnit.MINUTES,
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }

        /**
         * Runs one reconciliation pass immediately, without waiting for the
         * next periodic tick. Used right after boot, since alarms scheduled
         * before a reboot don't survive it.
         */
        fun enqueueImmediate(context: Context) {
            val request = OneTimeWorkRequestBuilder<ReminderReconcileWorker>().build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                IMMEDIATE_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request,
            )
        }
    }
}
