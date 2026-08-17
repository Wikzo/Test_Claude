package com.wikzo.todo.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Fired by [android.app.AlarmManager] at a task's exact due date (scheduled by
 * [ReminderScheduler]); shows that task's reminder notification.
 *
 * Registered in the manifest (not a dynamic registration), so it works even if
 * the app process has been killed -- the system starts a fresh one just to
 * deliver this broadcast.
 */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra(EXTRA_TASK_ID) ?: return
        val title = intent.getStringExtra(EXTRA_TITLE) ?: return
        NotificationHelper.showReminder(context, taskId, title)
    }

    companion object {
        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_TITLE = "extra_title"
    }
}
