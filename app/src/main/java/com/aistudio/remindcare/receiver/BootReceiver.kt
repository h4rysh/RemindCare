package com.aistudio.remindcare.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.aistudio.remindcare.util.AlarmWakeLockManager
import com.aistudio.remindcare.util.AlarmWatchdogWorker
import com.aistudio.remindcare.util.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Dedicated exported BroadcastReceiver for OS system-level broadcasts:
 * device boot completion, clock adjustments, timezone changes, and app package updates.
 *
 * Separating this from ReminderReceiver preserves strict IPC security:
 * internal alarm actions remain unexported, while system re-anchoring broadcasts
 * are reliably delivered by the Android OS.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d(TAG, "BootReceiver received system broadcast: $action")

        if (action in SYSTEM_ACTIONS) {
            AlarmWakeLockManager.acquire(context, 30_000L)
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    Log.d(TAG, "System configuration changed ($action), rescheduling all active alarms")
                    ReminderScheduler.rescheduleAll(context)
                    AlarmWatchdogWorker.schedule(context)
                } catch (e: Exception) {
                    Log.e(TAG, "Error handling system change broadcast", e)
                } finally {
                    AlarmWakeLockManager.release()
                    pendingResult?.finish()
                }
            }
        }
    }

    companion object {
        private const val TAG = "BootReceiver"

        val SYSTEM_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED
        )
    }
}
