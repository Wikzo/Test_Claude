package com.wikzo.todo.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

/**
 * Schedules/cancels the exact-time alarm backing a single task's due-date
 * reminder.
 *
 * One alarm per task, keyed by [NotificationHelper.notificationIdFor] as the
 * PendingIntent request code -- so scheduling a task that already has a
 * pending alarm just replaces it in place (same request code =
 * `FLAG_UPDATE_CURRENT` overwrites the existing PendingIntent's extras), no
 * explicit cancel-then-reschedule needed. That's what makes
 * [ReminderReconcileWorker] safe to call redundantly for every incomplete
 * task on every periodic run.
 *
 * Exact-alarm permission: this app declares `SCHEDULE_EXACT_ALARM` (a normal
 * install-time-declared "special" permission) rather than `USE_EXACT_ALARM`.
 * `USE_EXACT_ALARM` is auto-granted with no user action on API 33+, but Google
 * Play policy restricts it to apps whose *core* function is alarms/timers/
 * calendars -- a todo app with due-date reminders is a borderline fit at best,
 * and misdeclaring it risks store rejection. `SCHEDULE_EXACT_ALARM` is
 * auto-granted on API 31-32 and requires one manual "Alarms & reminders"
 * toggle in system settings on API 33+, which is an acceptable one-time cost
 * for a personal app. [scheduleReminder] falls back to an inexact alarm if the
 * permission isn't (yet) granted, rather than crashing.
 */
object ReminderScheduler {

    /**
     * Schedules [taskId]'s reminder to fire at [triggerAtMillis]. A no-op if
     * that time has already passed -- callers don't need to check themselves.
     */
    fun scheduleReminder(context: Context, taskId: String, title: String, triggerAtMillis: Long) {
        if (triggerAtMillis <= System.currentTimeMillis()) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = reminderPendingIntent(context, taskId, title)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            // SCHEDULE_EXACT_ALARM hasn't been granted (the user hasn't flipped
            // the "Alarms & reminders" toggle on API 33+). Fall back to an
            // inexact-but-Doze-aware alarm instead of letting AlarmManager
            // throw a SecurityException -- the reminder still fires close to
            // on time, and every periodic ReminderReconcileWorker run retries
            // scheduleReminder() regardless, so it self-heals once the
            // permission is granted.
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    /** Cancels any pending alarm for [taskId]. A no-op if none is scheduled. */
    fun cancelReminder(context: Context, taskId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = reminderPendingIntent(context, taskId, title = "")
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    /**
     * [title] doesn't affect PendingIntent identity (only the wrapped Intent's
     * action/data/component + the request code do -- extras are ignored for
     * matching), so [cancelReminder] can pass an empty title and still resolve
     * to the same PendingIntent [scheduleReminder] created.
     */
    private fun reminderPendingIntent(context: Context, taskId: String, title: String): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_TASK_ID, taskId)
            putExtra(ReminderReceiver.EXTRA_TITLE, title)
        }
        return PendingIntent.getBroadcast(
            context,
            NotificationHelper.notificationIdFor(taskId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
