package com.aistudio.remindcare.util

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.aistudio.remindcare.data.AppDatabase
import java.util.concurrent.TimeUnit

class AlarmWatchdogWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        Log.d(TAG, "Watchdog running: verifying active reminder schedules")
        return try {
            val db = AppDatabase.getDatabase(applicationContext)
            val activeReminders = db.reminderDao().getActiveRemindersDirect()
            Log.d(TAG, "Found ${activeReminders.size} active reminders to audit")

            for (reminder in activeReminders) {
                // Re-verify and re-register alarm intent
                ReminderScheduler.scheduleReminder(applicationContext, reminder)
            }
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Watchdog execution failed", e)
            Result.retry()
        }
    }

    companion object {
        const val TAG = "AlarmWatchdogWorker"
        private const val WORK_NAME = "remindcare_alarm_watchdog"

        fun schedule(context: Context) {
            try {
                val constraints = Constraints.Builder().build()
                val request = PeriodicWorkRequestBuilder<AlarmWatchdogWorker>(12, TimeUnit.HOURS)
                    .setConstraints(constraints)
                    .build()

                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
                )
                Log.d(TAG, "Enqueued periodic alarm watchdog work")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to schedule alarm watchdog", e)
            }
        }
    }
}
