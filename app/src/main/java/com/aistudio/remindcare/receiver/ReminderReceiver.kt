package com.aistudio.remindcare.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.aistudio.remindcare.alarm.ReminderAlarmService
import com.aistudio.remindcare.data.AppDatabase
import com.aistudio.remindcare.data.ReminderHistory
import com.aistudio.remindcare.util.AlarmWakeLockManager
import com.aistudio.remindcare.util.AlarmWatchdogWorker
import com.aistudio.remindcare.util.GlobalAlarmState
import com.aistudio.remindcare.util.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d(TAG, "onReceive action: $action")


        val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
        if (reminderId == -1L) {
            Log.e(TAG, "No valid reminder ID specified in alarm intent")
            return
        }

        // 2. Alarm action processing
        AlarmWakeLockManager.acquire(context, 45_000L)
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val reminderDao = db.reminderDao()
                val reminder = reminderDao.getReminderById(reminderId)

                if (reminder == null || reminder.isDeleted) {
                    Log.w(TAG, "Reminder $reminderId not found or deleted, cancelling any running alert")
                    ReminderAlarmService.stop(context)
                    return@launch
                }

                when (action) {
                    ACTION_TRIGGER_REMINDER -> {
                        if (reminder.isActive) {
                            Log.d(TAG, "Triggering alarm for reminder: ${reminder.title}")
                            // Start foreground alert service (handles WakeLock, notification, audio, activity launch)
                            ReminderAlarmService.start(context, reminder.id)

                            // Schedule next occurrence if repeating, or deactivate if once
                            if (reminder.repeat == "Daily" || reminder.repeat == "Weekly") {
                                ReminderScheduler.scheduleReminder(context, reminder)
                            } else {
                                reminderDao.updateReminder(reminder.copy(isActive = false))
                            }

                            // Schedule missed check (15 minutes from now)
                            scheduleMissedCheck(context, reminder.id)
                        }
                    }

                    ACTION_COMPLETE_REMINDER -> {
                        cancelMissedCheck(context, reminder.id)
                        ReminderAlarmService.stop(context)

                        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
                        db.historyDao().insertHistory(
                            ReminderHistory(
                                reminderId = reminder.id,
                                title = reminder.title,
                                type = reminder.type,
                                scheduledTime = reminder.time,
                                completedTime = timestamp,
                                status = "COMPLETED",
                                notes = "Completed from notification",
                                updatedAtMillis = System.currentTimeMillis()
                            )
                        )
                        Log.d(TAG, "Reminder $reminderId marked as COMPLETED from notification")
                    }

                    ACTION_SNOOZE_REMINDER -> {
                        cancelMissedCheck(context, reminder.id)
                        ReminderAlarmService.stop(context)

                        // Schedule snooze in 5 minutes
                        scheduleSnoozeAlarm(context, reminder.id)

                        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
                        db.historyDao().insertHistory(
                            ReminderHistory(
                                reminderId = reminder.id,
                                title = reminder.title,
                                type = reminder.type,
                                scheduledTime = reminder.time,
                                completedTime = timestamp,
                                status = "SNOOZED",
                                notes = "Snoozed from notification",
                                updatedAtMillis = System.currentTimeMillis()
                            )
                        )
                        Log.d(TAG, "Reminder $reminderId snoozed for 5 minutes")
                    }

                    ACTION_CHECK_MISSED -> {
                        val historyDao = db.historyDao()
                        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                        val entries = historyDao.getHistoryForReminder(reminder.id).filter { it.completedTime?.startsWith(today) == true }

                        if (entries.none { it.status == "COMPLETED" }) {
                            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
                            historyDao.insertHistory(
                                ReminderHistory(
                                    reminderId = reminder.id,
                                    title = reminder.title,
                                    type = reminder.type,
                                    scheduledTime = reminder.time,
                                    completedTime = timestamp,
                                    status = "MISSED",
                                    notes = "Missed by patient (timed out)",
                                    updatedAtMillis = System.currentTimeMillis()
                                )
                            )
                            if (GlobalAlarmState.activeAlert.value?.id == reminder.id) {
                                ReminderAlarmService.stop(context)
                            }
                            Log.d(TAG, "Reminder $reminderId flagged as MISSED due to timeout")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error executing ReminderReceiver background task", e)
            } finally {
                AlarmWakeLockManager.release()
                pendingResult.finish()
            }
        }
    }

    private fun scheduleMissedCheck(context: Context, reminderId: Long) {
        val missedIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_CHECK_MISSED
            putExtra(EXTRA_REMINDER_ID, reminderId)
        }
        val requestCode = getRequestCode(reminderId, CODE_MISSED_CHECK)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            missedIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        alarmManager?.set(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + (15 * 60 * 1000L),
            pendingIntent
        )
    }

    private fun scheduleSnoozeAlarm(context: Context, reminderId: Long) {
        val calendar = Calendar.getInstance().apply {
            add(Calendar.MINUTE, 5)
        }
        val snoozeIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_TRIGGER_REMINDER
            putExtra(EXTRA_REMINDER_ID, reminderId)
        }
        val requestCode = getRequestCode(reminderId, CODE_SNOOZE_ALARM)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        alarmManager?.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingIntent
        )
    }

    companion object {
        const val TAG = "ReminderReceiver"

        const val ACTION_TRIGGER_REMINDER = "com.aistudio.remindcare.action.TRIGGER_REMINDER"
        const val ACTION_COMPLETE_REMINDER = "com.aistudio.remindcare.action.COMPLETE_REMINDER"
        const val ACTION_SNOOZE_REMINDER = "com.aistudio.remindcare.action.SNOOZE_REMINDER"
        const val ACTION_CHECK_MISSED = "com.aistudio.remindcare.action.CHECK_MISSED"

        const val EXTRA_REMINDER_ID = "reminder_id"

        // Collision-free request code categories
        const val CODE_TRIGGER = 0
        const val CODE_COMPLETE = 1
        const val CODE_SNOOZE = 2
        const val CODE_FULL_SCREEN = 3
        const val CODE_MISSED_CHECK = 4
        const val CODE_SNOOZE_ALARM = 5


        fun createNotificationChannel(context: Context) {
            ReminderAlarmService.createNotificationChannel(context)
        }

        /**
         * Generates deterministic, collision-free 31-bit integer request codes for PendingIntents
         */
        fun getRequestCode(reminderId: Long, actionType: Int): Int {
            val hash = (reminderId.hashCode() * 37 + actionType) and 0x7FFFFFFF
            return hash
        }

        fun cancelMissedCheck(context: Context, reminderId: Long) {
            val missedIntent = Intent(context, ReminderReceiver::class.java).apply {
                action = ACTION_CHECK_MISSED
                putExtra(EXTRA_REMINDER_ID, reminderId)
            }
            val requestCode = getRequestCode(reminderId, CODE_MISSED_CHECK)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                missedIntent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
                alarmManager?.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }
    }
}
