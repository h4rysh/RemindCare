package com.aistudio.remindcare.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.aistudio.remindcare.data.AppDatabase
import com.aistudio.remindcare.data.Reminder
import com.aistudio.remindcare.receiver.ReminderReceiver
import java.util.Calendar

object ReminderScheduler {

    private const val TAG = "ReminderScheduler"
    // 2-minute safety buffer to prevent early-trigger infinite rescheduling loops
    private const val EARLY_TRIGGER_BUFFER_MS = 120_000L

    fun scheduleReminder(context: Context, reminder: Reminder) {
        if (!reminder.isActive || reminder.isDeleted) {
            cancelReminder(context, reminder.id)
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_TRIGGER_REMINDER
            putExtra(ReminderReceiver.EXTRA_REMINDER_ID, reminder.id)
        }

        val requestCode = ReminderReceiver.getRequestCode(reminder.id, ReminderReceiver.CODE_TRIGGER)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            val timeParts = reminder.time.split(":")
            if (timeParts.size >= 2) {
                val hours = timeParts[0].toIntOrNull() ?: 8
                val minutes = timeParts[1].toIntOrNull() ?: 0
                set(Calendar.HOUR_OF_DAY, hours)
                set(Calendar.MINUTE, minutes)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
        }

        val now = System.currentTimeMillis()
        // If scheduled time is in the past or within early-trigger buffer, advance to next cycle
        if (calendar.timeInMillis <= (now + EARLY_TRIGGER_BUFFER_MS)) {
            when (reminder.repeat) {
                "Weekly" -> calendar.add(Calendar.DAY_OF_YEAR, 7)
                "Daily" -> calendar.add(Calendar.DAY_OF_YEAR, 1)
                else -> {
                    // For "Once", if scheduled time has passed, do not schedule in the past
                    if (calendar.timeInMillis <= now) {
                        Log.d(TAG, "Not scheduling past 'Once' reminder ${reminder.id}")
                        return
                    }
                }
            }
        }

        val triggerTime = calendar.timeInMillis
        Log.d(TAG, "Scheduling alarm for reminder ${reminder.id} (${reminder.title}) at ${calendar.time} (epoch: $triggerTime)")

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException scheduling exact alarm, falling back to inexact", e)
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        }
    }

    fun cancelReminder(context: Context, reminderId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_TRIGGER_REMINDER
            putExtra(ReminderReceiver.EXTRA_REMINDER_ID, reminderId)
        }

        val requestCode = ReminderReceiver.getRequestCode(reminderId, ReminderReceiver.CODE_TRIGGER)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )

        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d(TAG, "Cancelled scheduled alarm for reminder $reminderId")
        }
    }

    suspend fun rescheduleAll(context: Context) {
        val db = AppDatabase.getDatabase(context)
        val reminderDao = db.reminderDao()
        try {
            val activeList = reminderDao.getActiveRemindersDirect()
            Log.d(TAG, "Rescheduling ${activeList.size} active reminders")
            activeList.forEach { reminder ->
                scheduleReminder(context, reminder)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to reschedule reminders", e)
        }
    }
}
