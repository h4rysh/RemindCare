package com.aistudio.remindcare.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import com.aistudio.remindcare.R
import com.aistudio.remindcare.data.AppDatabase
import com.aistudio.remindcare.data.Reminder
import com.aistudio.remindcare.receiver.ReminderReceiver
import com.aistudio.remindcare.util.AlarmWakeLockManager
import com.aistudio.remindcare.util.GlobalAlarmState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class ReminderAlarmService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        val reminderId = intent?.getLongExtra(EXTRA_REMINDER_ID, -1L) ?: -1L
        Log.d(TAG, "onStartCommand action: $action, reminderId: $reminderId")

        when (action) {
            ACTION_STOP_ALARM -> {
                stopAlarmAndSelf()
            }
            ACTION_START_ALARM -> {
                if (reminderId != -1L) {
                    startForegroundAlert(reminderId)
                } else {
                    stopSelf()
                }
            }
            else -> {
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    private fun startForegroundAlert(reminderId: Long) {
        AlarmWakeLockManager.acquire(this)

        serviceScope.launch {
            try {
                val db = AppDatabase.getDatabase(applicationContext)
                val reminder = db.reminderDao().getReminderById(reminderId)
                if (reminder == null || !reminder.isActive) {
                    Log.w(TAG, "Reminder $reminderId not found or inactive, stopping service")
                    stopAlarmAndSelf()
                    return@launch
                }

                // 1. Build and post foreground notification
                val notification = buildForegroundNotification(reminder)
                startForeground(NOTIFICATION_ID_OFFSET + reminder.id.toInt(), notification)

                // 2. Trigger audio, TTS, and active alert state
                GlobalAlarmState.triggerAlert(applicationContext, reminder)

                // 3. Launch the full screen alert activity
                val activityIntent = Intent(applicationContext, ReminderAlarmActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra(EXTRA_REMINDER_ID, reminder.id)
                }
                startActivity(activityIntent)
            } catch (e: Exception) {
                Log.e(TAG, "Error starting foreground alarm", e)
                stopAlarmAndSelf()
            }
        }
    }

    private fun buildForegroundNotification(reminder: Reminder): Notification {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createChannel(notificationManager)

        val fullScreenIntent = Intent(this, ReminderAlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_REMINDER_ID, reminder.id)
        }
        val mainPendingIntent = PendingIntent.getActivity(
            this,
            reminder.id.toInt(),
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val completeIntent = Intent(this, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_COMPLETE_REMINDER
            putExtra(EXTRA_REMINDER_ID, reminder.id)
        }
        val completePendingIntent = PendingIntent.getBroadcast(
            this,
            ReminderReceiver.getRequestCode(reminder.id, 1),
            completeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val snoozeIntent = Intent(this, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_SNOOZE_REMINDER
            putExtra(EXTRA_REMINDER_ID, reminder.id)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            this,
            ReminderReceiver.getRequestCode(reminder.id, 2),
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val detailText = buildString {
            if (!reminder.medicineName.isNullOrBlank()) append("Med: ${reminder.medicineName}. ")
            if (!reminder.medicineQuantity.isNullOrBlank()) append("Qty: ${reminder.medicineQuantity}. ")
            if (!reminder.medicineLocation.isNullOrBlank()) append("Location: ${reminder.medicineLocation}.")
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("RemindCare: ${reminder.title}")
            .setContentText(detailText.ifBlank { "Time to perform task!" })
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setContentIntent(mainPendingIntent)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            if (notificationManager.canUseFullScreenIntent()) {
                builder.setFullScreenIntent(mainPendingIntent, true)
            } else {
                val permissionIntent = Intent(android.provider.Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                    data = "package:$packageName".toUri()
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                val permissionPendingIntent = PendingIntent.getActivity(
                    this,
                    ReminderReceiver.getRequestCode(reminder.id, 3),
                    permissionIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
                builder.addAction(R.drawable.ic_settings, "Grant Permission", permissionPendingIntent)
            }
        } else {
            builder.setFullScreenIntent(mainPendingIntent, true)
        }

        builder.addAction(R.drawable.ic_check, "Complete", completePendingIntent)
            .addAction(R.drawable.ic_snooze, "Snooze (5m)", snoozePendingIntent)

        return builder.build()
    }

    private fun createChannel(notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "RemindCare Urgent Alarms",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Continuous high priority alarms for critical user reminders."
                enableLights(true)
                enableVibration(true)
                setBypassDnd(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun stopAlarmAndSelf() {
        GlobalAlarmState.clearAlert(applicationContext)
        AlarmWakeLockManager.release()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        serviceScope.cancel()
        AlarmWakeLockManager.release()
        super.onDestroy()
    }

    companion object {
        const val TAG = "ReminderAlarmService"
        const val CHANNEL_ID = "remindcare_alarms"
        const val EXTRA_REMINDER_ID = "reminder_id"

        const val ACTION_START_ALARM = "com.aistudio.remindcare.action.START_ALARM"
        const val ACTION_STOP_ALARM = "com.aistudio.remindcare.action.STOP_ALARM"

        private const val NOTIFICATION_ID_OFFSET = 100000

        fun createNotificationChannel(context: Context) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            if (notificationManager != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "RemindCare Urgent Alarms",
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    description = "Continuous high priority alarms for critical user reminders."
                    enableLights(true)
                    enableVibration(true)
                    setBypassDnd(true)
                }
                notificationManager.createNotificationChannel(channel)
            }
        }

        fun start(context: Context, reminderId: Long) {
            val intent = Intent(context, ReminderAlarmService::class.java).apply {
                action = ACTION_START_ALARM
                putExtra(EXTRA_REMINDER_ID, reminderId)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, ReminderAlarmService::class.java).apply {
                action = ACTION_STOP_ALARM
            }
            context.startService(intent)
        }
    }
}
