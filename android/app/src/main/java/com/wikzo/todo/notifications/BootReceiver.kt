package com.wikzo.todo.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Re-establishes reminder reconciliation after a device reboot.
 *
 * All previously-scheduled [android.app.AlarmManager] alarms are wiped by a
 * reboot, and periodic WorkManager work is not guaranteed to survive one
 * either (behavior here varies by OEM/API level). So on `BOOT_COMPLETED` this:
 *  1. Re-enqueues the periodic reconcile work (a no-op if WorkManager already
 *     restored it itself, since `enqueuePeriodic` uses `KEEP`).
 *  2. Kicks off one immediate one-shot reconciliation pass, so due-date alarms
 *     are back in place right away rather than waiting up to 15 minutes for
 *     the next periodic tick.
 *
 * Requires `RECEIVE_BOOT_COMPLETED` and must be `exported="true"` with the
 * `BOOT_COMPLETED` intent filter for the system to deliver the broadcast --
 * see AndroidManifest.xml.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        ReminderReconcileWorker.enqueuePeriodic(context)
        ReminderReconcileWorker.enqueueImmediate(context)
    }
}
