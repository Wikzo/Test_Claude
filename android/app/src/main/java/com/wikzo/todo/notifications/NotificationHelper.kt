package com.wikzo.todo.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.wikzo.todo.MainActivity

/**
 * Owns the "task_reminders" notification channel and is the single place that
 * actually shows a due-date reminder notification.
 *
 * This is a personal reminder app -- a due date firing is the whole point of the
 * notification, so the channel is IMPORTANCE_HIGH (heads-up + sound), not the
 * quieter DEFAULT.
 *
 * There's no res/drawable directory in this project yet -- even the launcher
 * icon is still the placeholder `@android:drawable/sym_def_app_icon` in the
 * manifest -- so [android.R.drawable.ic_popup_reminder] is used as the small
 * icon here for the same reason: it's a stand-in until the app has real icon
 * assets, not a deliberate design choice. Swap both when real assets exist.
 */
object NotificationHelper {

    const val CHANNEL_ID = "task_reminders"

    /**
     * Creates the notification channel. Call once, from
     * [com.wikzo.todo.TodoApplication.onCreate] -- channel creation is
     * idempotent, so it's safe to call on every app start.
     */
    fun createNotificationChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Task reminders",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Alerts you when one of your tasks is due"
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    /**
     * Builds and shows the due-date reminder for [taskId]. Uses
     * [notificationIdFor] as the Android notification id, so calling this again
     * for the same task (e.g. the reconcile worker re-scheduling the same
     * alarm) replaces the existing notification instead of stacking a
     * duplicate.
     *
     * Tapping the notification just opens [MainActivity] to the task list --
     * the nav graph (see TodoApp.kt) has no start-destination/deep-link support
     * for landing directly on a specific task, and adding one is out of scope
     * here.
     */
    fun showReminder(context: Context, taskId: String, title: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            // Not granted -- nothing to do. There's no UI to prompt from here:
            // this can run from a BroadcastReceiver with the app not even in
            // the foreground.
            return
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            notificationIdFor(taskId),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText("Due now")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()

        NotificationManagerCompat.from(context).notify(notificationIdFor(taskId), notification)
    }

    /**
     * A stable Int derived from the Firestore task doc id. Shared with
     * [ReminderScheduler] (as the PendingIntent request code) so that showing,
     * scheduling, and cancelling a given task's reminder all key off the same
     * identity.
     */
    fun notificationIdFor(taskId: String): Int = taskId.hashCode()
}
