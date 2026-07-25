package com.example.ui

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.AppSettings
import com.example.data.Reminder
import com.example.data.ReminderHistory
import com.example.data.RemindCareRepository
import com.example.receiver.ReminderReceiver
import com.example.util.GlobalAlarmState
import com.example.util.ReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.random.Random

class RemindCareViewModel(
    application: Application,
    private val repository: RemindCareRepository
) : AndroidViewModel(application) {

    private val context = application.applicationContext

    // State Flows
    val allReminders: StateFlow<List<Reminder>> = repository.allReminders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeReminders: StateFlow<List<Reminder>> = repository.activeReminders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val history: StateFlow<List<ReminderHistory>> = repository.allHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settings: StateFlow<AppSettings> = repository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    init {
        // Initialize with default reminders if database is empty
        viewModelScope.launch(Dispatchers.IO) {
            val currentReminders = repository.allReminders.first()
            if (currentReminders.isEmpty()) {
                prepopulateDefaultReminders()
            }
            // Ensure settings are initialized
            val currentSettings = repository.getSettingsDirect()
            if (currentSettings == null) {
                repository.saveSettings(AppSettings())
            }
            launchSyncListeners()
        }
    }

    private fun launchSyncListeners() {
        // Caregiver syncing Reminders TO Cloud
        viewModelScope.launch(Dispatchers.IO) {
            repository.allReminders.collect { reminders ->
                val currentSettings = repository.getSettingsDirect() ?: AppSettings()
                if (currentSettings.role == "Caregiver" && currentSettings.isPaired && currentSettings.connectedPatientId.isNotBlank()) {
                    repository.syncRepository.syncRemindersToCloud(currentSettings.connectedPatientId, reminders)
                }
            }
        }
        
        // Patient listening to Reminders FROM Cloud
        viewModelScope.launch(Dispatchers.IO) {
            settings.collect { currentSettings ->
                if (currentSettings.role == "Patient" && currentSettings.isPaired) {
                    val patientId = repository.syncRepository.getDeviceId()
                    repository.syncRepository.listenForCloudReminders(patientId).collect { cloudReminders ->
                        // Synchronize locally
                        val localIds = repository.allReminders.first().map { it.id }.toSet()
                        val cloudIds = cloudReminders.map { it.id }.toSet()
                        
                        for (r in cloudReminders) {
                            repository.insertReminder(r)
                            ReminderScheduler.scheduleReminder(context, r)
                        }
                        
                        val toDelete = localIds - cloudIds
                        for (id in toDelete) {
                            repository.deleteReminderById(id)
                            ReminderScheduler.cancelReminder(context, id)
                        }
                    }
                }
            }
        }

        // Patient syncing History TO Cloud
        viewModelScope.launch(Dispatchers.IO) {
            repository.allHistory.collect { history ->
                val currentSettings = repository.getSettingsDirect() ?: AppSettings()
                if (currentSettings.role == "Patient" && currentSettings.isPaired) {
                    val patientId = repository.syncRepository.getDeviceId()
                    repository.syncRepository.syncHistoryToCloud(patientId, history)
                }
            }
        }

        // Caregiver listening to History FROM Cloud
        viewModelScope.launch(Dispatchers.IO) {
            settings.collect { currentSettings ->
                if (currentSettings.role == "Caregiver" && currentSettings.isPaired && currentSettings.connectedPatientId.isNotBlank()) {
                    repository.syncRepository.listenForCloudHistory(currentSettings.connectedPatientId).collect { cloudHistory ->
                        for (item in cloudHistory) {
                            repository.insertHistory(item)
                        }
                    }
                }
            }
        }
    }

    private suspend fun prepopulateDefaultReminders() {
        Log.d("RemindCareViewModel", "Prepopulating database with default assistive reminders")
        val defaults = listOf(
            Reminder(
                title = "Morning Medicine",
                type = "Medicine",
                time = "08:00",
                repeat = "Daily",
                medicineName = "Paracetamol",
                medicineQuantity = "2 Tablets",
                medicineLocation = "Kitchen Cabinet",
                photoRequired = true,
                additionalNotes = "Take with water right after breakfast"
            ),
            Reminder(
                title = "Drink Water",
                type = "Water",
                time = "10:30",
                repeat = "Daily",
                additionalNotes = "Large glass from the blue bottle",
                photoRequired = false
            ),
            Reminder(
                title = "Eat Lunch",
                type = "Meal",
                time = "12:30",
                repeat = "Daily",
                additionalNotes = "Check fridge for pre-prepared chicken soup",
                photoRequired = false
            ),
            Reminder(
                title = "Afternoon Walk",
                type = "Exercise",
                time = "16:00",
                repeat = "Daily",
                additionalNotes = "15-minute gentle walk in the backyard",
                photoRequired = false
            ),
            Reminder(
                title = "Sleep Prep",
                type = "Sleep",
                time = "21:30",
                repeat = "Daily",
                additionalNotes = "Turn off TV and drink warm herbal tea",
                photoRequired = false
            )
        )

        for (reminder in defaults) {
            val id = repository.insertReminder(reminder)
            // Schedule using alarm manager
            ReminderScheduler.scheduleReminder(context, reminder.copy(id = id))
        }
    }

    // Reminder Actions
    fun addReminder(reminder: Reminder) {
        viewModelScope.launch(Dispatchers.IO) {
            val id = repository.insertReminder(reminder)
            val inserted = reminder.copy(id = id)
            ReminderScheduler.scheduleReminder(context, inserted)
        }
    }

    fun updateReminder(reminder: Reminder) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateReminder(reminder)
            ReminderScheduler.scheduleReminder(context, reminder)
        }
    }

    fun deleteReminder(reminder: Reminder) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteReminder(reminder)
            ReminderScheduler.cancelReminder(context, reminder.id)
            val currentSettings = settings.value
            if (currentSettings.role == "Caregiver" && currentSettings.isPaired && currentSettings.connectedPatientId.isNotBlank()) {
                repository.syncRepository.deleteCloudReminder(currentSettings.connectedPatientId, reminder.id)
            }
        }
    }

    // Setting Actions
    fun setRole(role: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = settings.value
            repository.saveSettings(current.copy(role = role))
        }
    }

    fun updateFontSize(size: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = settings.value
            repository.saveSettings(current.copy(fontSize = size))
        }
    }

    fun updateThemeMode(mode: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = settings.value
            repository.saveSettings(current.copy(themeMode = mode))
        }
    }

    fun updateVolume(volume: Float) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = settings.value
            repository.saveSettings(current.copy(reminderVolume = volume))
        }
    }

    fun updateRingtone(ringtone: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = settings.value
            repository.saveSettings(current.copy(ringtone = ringtone))
        }
    }

    fun toggleVoiceInstructions(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = settings.value
            repository.saveSettings(current.copy(voiceInstructionsEnabled = enabled))
        }
    }

    // Alarm Responses
    fun completeReminder(reminder: Reminder, photoBitmap: Bitmap? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            var photoPath: String? = null
            
            // Save bitmap locally if photo is required
            if (photoBitmap != null) {
                try {
                    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                    val file = File(context.filesDir, "remindcare_verification_$timestamp.jpg")
                    val out = FileOutputStream(file)
                    photoBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                    out.flush()
                    out.close()
                    photoPath = file.absolutePath
                    Log.d("RemindCareViewModel", "Saved local verification photo to: $photoPath")
                } catch (e: Exception) {
                    Log.e("RemindCareViewModel", "Failed to save verification photo", e)
                }
            }

            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
            
            // Log history
            repository.insertHistory(
                ReminderHistory(
                    reminderId = reminder.id,
                    title = reminder.title,
                    type = reminder.type,
                    scheduledTime = reminder.time,
                    completedTime = timestamp,
                    status = "COMPLETED",
                    photoPath = photoPath,
                    notes = "Completed on device"
                )
            )

            // Clear alert
            GlobalAlarmState.clearAlert(context)
            
            // Cancel notification
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
            notificationManager?.cancel(reminder.id.toInt())
        }
    }

    fun snoozeReminder(reminder: Reminder) {
        viewModelScope.launch(Dispatchers.IO) {
            // Schedule exact alarm in 5 minutes
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            if (alarmManager != null) {
                val calendar = Calendar.getInstance().apply {
                    add(Calendar.MINUTE, 5)
                }
                
                val intent = Intent(context, ReminderReceiver::class.java).apply {
                    action = ReminderReceiver.ACTION_TRIGGER_REMINDER
                    putExtra(ReminderReceiver.EXTRA_REMINDER_ID, reminder.id)
                }

                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    reminder.id.toInt() + 50000, // SNOOZE OFFSET
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                }
                Log.d("RemindCareViewModel", "Snoozed alarm scheduled in 5m for reminder ${reminder.id}")
            }

            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
            
            // Log history as SNOOZED
            repository.insertHistory(
                ReminderHistory(
                    reminderId = reminder.id,
                    title = reminder.title,
                    type = reminder.type,
                    scheduledTime = reminder.time,
                    completedTime = timestamp,
                    status = "SNOOZED",
                    notes = "Snoozed on device"
                )
            )

            // Clear alert
            GlobalAlarmState.clearAlert(context)

            // Cancel notification
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
            notificationManager?.cancel(reminder.id.toInt())
        }
    }

    fun skipReminder(reminder: Reminder) {
        viewModelScope.launch(Dispatchers.IO) {
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
            repository.insertHistory(
                ReminderHistory(
                    reminderId = reminder.id,
                    title = reminder.title,
                    type = reminder.type,
                    scheduledTime = reminder.time,
                    completedTime = timestamp,
                    status = "SKIPPED",
                    notes = "Skipped by patient"
                )
            )

            GlobalAlarmState.clearAlert(context)

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
            notificationManager?.cancel(reminder.id.toInt())
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearHistory()
        }
    }


    fun updateCaregiverProfile(name: String, pin: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = settings.value
            repository.saveSettings(current.copy(caregiverName = name, caregiverPin = pin))
        }
    }

    fun updatePatientProfile(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = settings.value
            repository.saveSettings(current.copy(patientName = name))
        }
    }

    // Pairing Logic via SyncRepository
    fun generatePairingCode(onCodeGenerated: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val code = repository.syncRepository.generatePairingCode(settings.value.patientName)
            launch(Dispatchers.Main) {
                onCodeGenerated(code)
            }
        }
    }

    fun requestPairing(code: String, onResult: (com.example.data.SyncResult) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.syncRepository.requestPairing(code, repository.syncRepository.getDeviceId(), settings.value.caregiverName).collect { result ->
                if (result is com.example.data.SyncResult.Success) {
                    val current = settings.value
                    repository.saveSettings(current.copy(
                        isPaired = true,
                        connectedPatientId = result.patientId,
                        connectedPatientName = result.patientName
                    ))
                }
                launch(Dispatchers.Main) {
                    onResult(result)
                }
            }
        }
    }

    fun listenForPairingRequest(code: String, onRequest: (com.example.data.PairingRequest?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.syncRepository.listenForPairingRequest(code).collect { request ->
                launch(Dispatchers.Main) {
                    onRequest(request)
                }
            }
        }
    }

    fun approvePairingRequest(code: String, caregiverId: String, caregiverName: String, onComplete: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val success = repository.syncRepository.approvePairingRequest(code, caregiverId, caregiverName)
            if (success) {
                val current = settings.value
                repository.saveSettings(current.copy(
                    isPaired = true,
                    connectedCaregiverName = caregiverName,
                    connectedCaregiverId = caregiverId
                ))
            }
            launch(Dispatchers.Main) {
                onComplete()
            }
        }
    }

    fun declinePairingRequest(code: String, onComplete: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.syncRepository.declinePairingRequest(code)
            launch(Dispatchers.Main) {
                onComplete()
            }
        }
    }

    fun disconnectPairing() {
        viewModelScope.launch(Dispatchers.IO) {
            val patientId = settings.value.connectedPatientId
            val caregiverId = settings.value.connectedCaregiverId
            repository.syncRepository.disconnect(patientId, caregiverId)
            val current = settings.value
            repository.saveSettings(current.copy(
                isPaired = false,
                connectedCaregiverName = "",
                connectedCaregiverId = "",
                connectedPatientId = "",
                connectedPatientName = ""
            ))
        }
    }

    class Factory(
        private val application: android.app.Application,
        private val repository: com.example.data.RemindCareRepository
    ) : androidx.lifecycle.ViewModelProvider.Factory {
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(RemindCareViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return RemindCareViewModel(application, repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
