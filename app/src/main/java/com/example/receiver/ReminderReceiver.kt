package com.example.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.AppDatabase
import com.example.data.Reminder
import com.example.data.ReminderHistory
import com.example.util.GlobalAlarmState
import com.example.util.ReminderScheduler
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
        Log.d(TAG, "Received broadcast action: $action")

        if (Intent.ACTION_BOOT_COMPLETED == action) {
            // Device rebooted, reschedule all active alarms
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    ReminderScheduler.rescheduleAll(context)
                } catch (e: Exception) {
                    Log.e(TAG, "Error rescheduling alarms on boot", e)
                } finally {
                    pendingResult.finish()
                }
            }
            return
        }

        val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
        if (reminderId == -1L) {
            Log.e(TAG, "No reminder ID specified in alarm intent")
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val reminderDao = db.reminderDao()
                val reminder = reminderDao.getReminderById(reminderId)

                if (reminder == null) {
                    Log.e(TAG, "Reminder with ID $reminderId not found")
                    pendingResult.finish()
                    return@launch
                }

                when (action) {
                    ACTION_TRIGGER_REMINDER -> {
                        if (reminder.isActive) {
                            // 1. Trigger Full Screen alert state (sound, vibration, TTS, UI)
                            GlobalAlarmState.triggerAlert(context, reminder)

                            // 2. Show persistent notification
                            showReminderNotification(context, reminder)

                            // 3. Reschedule the next occurrence (e.g. if repeating)
                            if (reminder.repeat == "Daily") {
                                ReminderScheduler.scheduleReminder(context, reminder)
                            }
                        }
                    }
                    ACTION_COMPLETE_REMINDER -> {
                        // Mark as completed
                        val historyDao = db.historyDao()
                        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
                        
                        historyDao.insertHistory(
                            ReminderHistory(
                                reminderId = reminder.id,
                                title = reminder.title,
                                type = reminder.type,
                                scheduledTime = reminder.time,
                                completedTime = timestamp,
                                status = "COMPLETED",
                                notes = "Completed from notification"
                            )
                        )

                        // Clear active UI/Sound state
                        GlobalAlarmState.clearAlert(context)

                        // Cancel notification
                        cancelNotification(context, reminder.id.toInt())
                        Log.d(TAG, "Reminder completed from notification action")
                    }
                    ACTION_SNOOZE_REMINDER -> {
                        // Schedule snoozed reminder in 5 minutes
                        val calendar = Calendar.getInstance().apply {
                            add(Calendar.MINUTE, 5)
                        }
                        
                        // Temporarily schedule a snooze alarm
                        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? android.app.AlarmManager
                        if (alarmManager != null) {
                            val snoozeIntent = Intent(context, ReminderReceiver::class.java).apply {
                                this.action = ACTION_TRIGGER_REMINDER
                                putExtra(EXTRA_REMINDER_ID, reminder.id)
                            }
                            val pendingIntent = PendingIntent.getBroadcast(
                                context,
                                reminder.id.toInt() + SNOOZE_OFFSET,
                                snoozeIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                            )
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                alarmManager.setExactAndAllowWhileIdle(
                                    android.app.AlarmManager.RTC_WAKEUP,
                                    calendar.timeInMillis,
                                    pendingIntent
                                )
                            } else {
                                alarmManager.set(
                                    android.app.AlarmManager.RTC_WAKEUP,
                                    calendar.timeInMillis,
                                    pendingIntent
                                )
                            }
                            Log.d(TAG, "Scheduled snooze in 5m at ${calendar.time}")
                        }

                        // Log history as SNOOZED
                        val historyDao = db.historyDao()
                        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
                        historyDao.insertHistory(
                            ReminderHistory(
                                reminderId = reminder.id,
                                title = reminder.title,
                                type = reminder.type,
                                scheduledTime = reminder.time,
                                completedTime = timestamp,
                                status = "SNOOZED",
                                notes = "Snoozed from notification"
                            )
                        )

                        // Clear active UI/Sound state
                        GlobalAlarmState.clearAlert(context)

                        // Cancel notification
                        cancelNotification(context, reminder.id.toInt())
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing alarm intent", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun showReminderNotification(context: Context, reminder: Reminder) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        createNotificationChannel(context)

        // Intent to launch MainActivity (which opens Full-Screen Alert screen)
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_REMINDER_ID, reminder.id)
        }
        val mainPendingIntent = PendingIntent.getActivity(
            context,
            reminder.id.toInt(),
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Complete action pending intent
        val completeIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_COMPLETE_REMINDER
            putExtra(EXTRA_REMINDER_ID, reminder.id)
        }
        val completePendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id.toInt() + 10000,
            completeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Snooze action pending intent
        val snoozeIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_SNOOZE_REMINDER
            putExtra(EXTRA_REMINDER_ID, reminder.id)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id.toInt() + 20000,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val detailText = buildString {
            if (!reminder.medicineName.isNullOrBlank()) append("Med: ${reminder.medicineName}. ")
            if (!reminder.medicineQuantity.isNullOrBlank()) append("Qty: ${reminder.medicineQuantity}. ")
            if (!reminder.medicineLocation.isNullOrBlank()) append("Location: ${reminder.medicineLocation}.")
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("RemindCare: ${reminder.title}")
            .setContentText(if (detailText.isBlank()) "Time to perform task!" else detailText)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true) // Cannot be swiped away easily
            .setContentIntent(mainPendingIntent)
            .addAction(android.R.drawable.ic_menu_save, "Complete", completePendingIntent)
            .addAction(android.R.drawable.ic_menu_recent_history, "Snooze (5m)", snoozePendingIntent)
            .setAutoCancel(true)

        notificationManager.notify(reminder.id.toInt(), builder.build())
    }

    private fun cancelNotification(context: Context, notificationId: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.cancel(notificationId)
    }

    companion object {
        const val TAG = "ReminderReceiver"
        
        const val ACTION_TRIGGER_REMINDER = "com.example.action.TRIGGER_REMINDER"
        const val ACTION_COMPLETE_REMINDER = "com.example.action.COMPLETE_REMINDER"
        const val ACTION_SNOOZE_REMINDER = "com.example.action.SNOOZE_REMINDER"
        
        const val EXTRA_REMINDER_ID = "reminder_id"
        private const val SNOOZE_OFFSET = 50000
        private const val CHANNEL_ID = "remindcare_alarms"

        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                if (notificationManager != null) {
                    val channel = NotificationChannel(
                        CHANNEL_ID,
                        "RemindCare Urgent Alarms",
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description = "Continuous high priority alarms for critical user reminders."
                        enableLights(true)
                        enableVibration(true)
                        setBypassDnd(true)
                    }
                    notificationManager.createNotificationChannel(channel)
                }
            }
        }
    }
}
