package com.aistudio.remindcare.ui

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aistudio.remindcare.data.AppSettings
import com.aistudio.remindcare.data.Reminder
import com.aistudio.remindcare.data.ReminderHistory
import com.aistudio.remindcare.data.RemindCareRepository
import com.aistudio.remindcare.data.gateway.AndroidAlarmScheduler
import com.aistudio.remindcare.data.gateway.FilePhotoStorage
import com.aistudio.remindcare.data.mapper.toData
import com.aistudio.remindcare.data.mapper.toDomain
import com.aistudio.remindcare.data.mapper.toEntity
import com.aistudio.remindcare.data.repository.HistoryRepositoryImpl
import com.aistudio.remindcare.data.repository.PairingRepositoryImpl
import com.aistudio.remindcare.data.repository.ReminderRepositoryImpl
import com.aistudio.remindcare.data.repository.SettingsRepositoryImpl
import com.aistudio.remindcare.domain.usecase.auth.SetCaregiverPinUseCase
import com.aistudio.remindcare.domain.usecase.auth.VerifyCaregiverPinUseCase
import com.aistudio.remindcare.domain.usecase.dose.CheckMissedRemindersUseCase
import com.aistudio.remindcare.domain.usecase.dose.CompleteReminderUseCase
import com.aistudio.remindcare.domain.usecase.dose.SnoozeReminderUseCase
import com.aistudio.remindcare.domain.usecase.pairing.ApprovePairingUseCase
import com.aistudio.remindcare.domain.usecase.pairing.DeclinePairingUseCase
import com.aistudio.remindcare.domain.usecase.pairing.DisconnectPairingUseCase
import com.aistudio.remindcare.domain.usecase.pairing.GeneratePairingCodeUseCase
import com.aistudio.remindcare.domain.usecase.pairing.ListenPairingRequestUseCase
import com.aistudio.remindcare.domain.usecase.pairing.RequestPairingUseCase
import com.aistudio.remindcare.domain.usecase.reminder.DeleteReminderUseCase
import com.aistudio.remindcare.domain.usecase.reminder.GetActiveRemindersUseCase
import com.aistudio.remindcare.domain.usecase.reminder.GetReminderByIdUseCase
import com.aistudio.remindcare.domain.usecase.reminder.GetRemindersUseCase
import com.aistudio.remindcare.domain.usecase.reminder.PrepopulateDefaultsUseCase
import com.aistudio.remindcare.domain.usecase.reminder.SaveReminderUseCase
import com.aistudio.remindcare.domain.usecase.settings.GetSettingsUseCase
import com.aistudio.remindcare.domain.usecase.settings.UpdateCaregiverProfileUseCase
import com.aistudio.remindcare.domain.usecase.settings.UpdatePatientProfileUseCase
import com.aistudio.remindcare.domain.usecase.settings.UpdateSettingsUseCase
import com.aistudio.remindcare.domain.usecase.sync.ObserveSyncUseCase
import com.aistudio.remindcare.receiver.ReminderReceiver
import com.aistudio.remindcare.util.CrashlyticsManager
import com.aistudio.remindcare.util.GlobalAlarmState
import com.aistudio.remindcare.util.ReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class RemindCareViewModel(
    application: Application,
    val repository: RemindCareRepository,
    val getRemindersUseCase: GetRemindersUseCase = GetRemindersUseCase(ReminderRepositoryImpl(repository.database, repository.syncRepository)),
    val getActiveRemindersUseCase: GetActiveRemindersUseCase = GetActiveRemindersUseCase(ReminderRepositoryImpl(repository.database, repository.syncRepository)),
    val getReminderByIdUseCase: GetReminderByIdUseCase = GetReminderByIdUseCase(ReminderRepositoryImpl(repository.database, repository.syncRepository)),
    val saveReminderUseCase: SaveReminderUseCase = SaveReminderUseCase(
        ReminderRepositoryImpl(repository.database, repository.syncRepository),
        AndroidAlarmScheduler(application.applicationContext)
    ),
    val deleteReminderUseCase: DeleteReminderUseCase = DeleteReminderUseCase(
        ReminderRepositoryImpl(repository.database, repository.syncRepository),
        AndroidAlarmScheduler(application.applicationContext)
    ),
    val prepopulateDefaultsUseCase: PrepopulateDefaultsUseCase = PrepopulateDefaultsUseCase(
        ReminderRepositoryImpl(repository.database, repository.syncRepository)
    ),
    val completeReminderUseCase: CompleteReminderUseCase = CompleteReminderUseCase(
        ReminderRepositoryImpl(repository.database, repository.syncRepository),
        HistoryRepositoryImpl(repository.database, repository.syncRepository),
        AndroidAlarmScheduler(application.applicationContext),
        FilePhotoStorage(application.applicationContext)
    ),
    val snoozeReminderUseCase: SnoozeReminderUseCase = SnoozeReminderUseCase(
        ReminderRepositoryImpl(repository.database, repository.syncRepository),
        HistoryRepositoryImpl(repository.database, repository.syncRepository),
        AndroidAlarmScheduler(application.applicationContext)
    ),
    val checkMissedRemindersUseCase: CheckMissedRemindersUseCase = CheckMissedRemindersUseCase(
        ReminderRepositoryImpl(repository.database, repository.syncRepository),
        HistoryRepositoryImpl(repository.database, repository.syncRepository)
    ),
    val getSettingsUseCase: GetSettingsUseCase = GetSettingsUseCase(SettingsRepositoryImpl(repository.database)),
    val updateSettingsUseCase: UpdateSettingsUseCase = UpdateSettingsUseCase(SettingsRepositoryImpl(repository.database)),
    val updateCaregiverProfileUseCase: UpdateCaregiverProfileUseCase = UpdateCaregiverProfileUseCase(SettingsRepositoryImpl(repository.database)),
    val updatePatientProfileUseCase: UpdatePatientProfileUseCase = UpdatePatientProfileUseCase(SettingsRepositoryImpl(repository.database)),
    val verifyCaregiverPinUseCase: VerifyCaregiverPinUseCase = VerifyCaregiverPinUseCase(SettingsRepositoryImpl(repository.database)),
    val setCaregiverPinUseCase: SetCaregiverPinUseCase = SetCaregiverPinUseCase(SettingsRepositoryImpl(repository.database)),
    val generatePairingCodeUseCase: GeneratePairingCodeUseCase = GeneratePairingCodeUseCase(
        PairingRepositoryImpl(repository.syncRepository),
        SettingsRepositoryImpl(repository.database)
    ),
    val requestPairingUseCase: RequestPairingUseCase = RequestPairingUseCase(
        PairingRepositoryImpl(repository.syncRepository),
        SettingsRepositoryImpl(repository.database)
    ),
    val listenPairingRequestUseCase: ListenPairingRequestUseCase = ListenPairingRequestUseCase(
        PairingRepositoryImpl(repository.syncRepository)
    ),
    val approvePairingUseCase: ApprovePairingUseCase = ApprovePairingUseCase(
        PairingRepositoryImpl(repository.syncRepository),
        SettingsRepositoryImpl(repository.database),
        ReminderRepositoryImpl(repository.database, repository.syncRepository),
        HistoryRepositoryImpl(repository.database, repository.syncRepository)
    ),
    val declinePairingUseCase: DeclinePairingUseCase = DeclinePairingUseCase(
        PairingRepositoryImpl(repository.syncRepository)
    ),
    val disconnectPairingUseCase: DisconnectPairingUseCase = DisconnectPairingUseCase(
        PairingRepositoryImpl(repository.syncRepository),
        SettingsRepositoryImpl(repository.database)
    ),
    val observeSyncUseCase: ObserveSyncUseCase = ObserveSyncUseCase(
        ReminderRepositoryImpl(repository.database, repository.syncRepository),
        HistoryRepositoryImpl(repository.database, repository.syncRepository)
    )
) : AndroidViewModel(application) {

    private val context get() = getApplication<Application>().applicationContext

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
                prepopulateDefaultsUseCase()
            }
            // Ensure settings are initialized
            repository.getSettingsDirect()
            launchSyncListeners()
            // Track Crashlytics user state reactively
            viewModelScope.launch(Dispatchers.IO) {
                kotlinx.coroutines.flow.combine(settings, activeReminders) { s, reminders ->
                    Pair(s, reminders.size)
                }.collect { (s, count) ->
                    CrashlyticsManager.setUserState(
                        role = s.role,
                        isPaired = s.isPaired,
                        activeAlarmsCount = count,
                        deviceId = repository.syncRepository.getDeviceId()
                    )
                }
            }
        }
    }

    private val isApplyingCloudReminders = java.util.concurrent.atomic.AtomicBoolean(false)

    private fun launchSyncListeners() {
        // Caregiver syncing Reminders TO Cloud
        viewModelScope.launch(Dispatchers.IO) {
            repository.allReminders.collect { reminders ->
                if (isApplyingCloudReminders.get()) return@collect
                val currentSettings = settings.value
                if ((currentSettings.role == "Caregiver") &&
                    currentSettings.isPaired &&
                    currentSettings.connectedPatientId.isNotBlank()
                ) {
                    repository.syncRepository.syncRemindersToCloud(currentSettings.connectedPatientId, reminders)
                }
            }
        }

        // Patient and Caregiver listening to Reminders FROM Cloud
        viewModelScope.launch(Dispatchers.IO) {
            settings.flatMapLatest { currentSettings ->
                if ((currentSettings.role == "Patient") && currentSettings.isPaired) {
                    val patientId = repository.syncRepository.getDeviceId()
                    repository.syncRepository.listenForCloudReminders(patientId)
                } else if ((currentSettings.role == "Caregiver") && currentSettings.isPaired && currentSettings.connectedPatientId.isNotBlank()) {
                    repository.syncRepository.listenForCloudReminders(currentSettings.connectedPatientId)
                } else {
                    kotlinx.coroutines.flow.emptyFlow()
                }
            }.collect { cloudReminders ->
                isApplyingCloudReminders.set(true)
                try {
                    val localReminders = repository.allReminders.first()
                    val localMapByUuid = localReminders.filter { it.uuid.isNotBlank() }.associateBy { it.uuid }
                    val cloudMapByUuid = cloudReminders.filter { it.uuid.isNotBlank() }.associateBy { it.uuid }

                    // Insert or update changed reminders from cloud
                    for (cr in cloudReminders) {
                        val existingLocal = if (cr.uuid.isNotBlank()) localMapByUuid[cr.uuid] else null
                        if (existingLocal == null) {
                            repository.insertReminder(cr)
                            if (settings.value.role == "Patient") {
                                ReminderScheduler.scheduleReminder(context, cr)
                            }
                        } else if (existingLocal.copy(id = cr.id, updatedAtMillis = cr.updatedAtMillis) != cr) {
                            val updated = cr.copy(id = existingLocal.id)
                            repository.updateReminder(updated)
                            if (settings.value.role == "Patient") {
                                ReminderScheduler.scheduleReminder(context, updated)
                            }
                        }
                    }

                    // Delete reminders removed from cloud
                    for (local in localReminders) {
                        if (local.uuid.isNotBlank() && !cloudMapByUuid.containsKey(local.uuid)) {
                            repository.deleteReminderById(local.id)
                            if (settings.value.role == "Patient") {
                                ReminderScheduler.cancelReminder(context, local.id)
                            }
                        }
                    }
                } finally {
                    kotlinx.coroutines.delay(200L)
                    isApplyingCloudReminders.set(false)
                }
            }
        }

        // Patient syncing History TO Cloud
        viewModelScope.launch(Dispatchers.IO) {
            repository.allHistory.collect { history ->
                val currentSettings = settings.value
                if ((currentSettings.role == "Patient") &&
                    currentSettings.isPaired
                ) {
                    val patientId = repository.syncRepository.getDeviceId()
                    repository.syncRepository.syncHistoryToCloud(patientId, history)
                }
            }
        }

        // Caregiver listening to History FROM Cloud
        viewModelScope.launch(Dispatchers.IO) {
            settings.flatMapLatest { currentSettings ->
                if ((currentSettings.role == "Caregiver") &&
                    currentSettings.isPaired &&
                    currentSettings.connectedPatientId.isNotBlank()
                ) {
                    repository.syncRepository.listenForCloudHistory(currentSettings.connectedPatientId)
                } else {
                    kotlinx.coroutines.flow.emptyFlow()
                }
            }.collect { cloudHistory ->
                for (item in cloudHistory) {
                    repository.insertHistory(item)
                }
            }
        }
    }

    // Reminder Actions delegated to Domain UseCases
    fun addReminder(reminder: Reminder) {
        viewModelScope.launch(Dispatchers.IO) {
            saveReminderUseCase(reminder.toDomain())
        }
    }

    fun updateReminder(reminder: Reminder) {
        viewModelScope.launch(Dispatchers.IO) {
            saveReminderUseCase(reminder.toDomain())
        }
    }

    fun deleteReminder(reminder: Reminder) {
        viewModelScope.launch(Dispatchers.IO) {
            deleteReminderUseCase(reminder.toDomain())
            val currentSettings = settings.value
            if ((currentSettings.role == "Caregiver") &&
                currentSettings.isPaired &&
                currentSettings.connectedPatientId.isNotBlank()
            ) {
                repository.syncRepository.deleteCloudReminder(currentSettings.connectedPatientId, reminder.id)
            }
        }
    }

    // Setting Actions delegated to Domain UseCases
    fun setRole(role: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = repository.getSettingsDirect()
            updateSettingsUseCase(current.copy(role = role).toDomain())
        }
    }

    fun updateFontSize(size: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = repository.getSettingsDirect()
            updateSettingsUseCase(current.copy(fontSize = size).toDomain())
        }
    }

    fun updateThemeMode(mode: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = repository.getSettingsDirect()
            updateSettingsUseCase(current.copy(themeMode = mode).toDomain())
        }
    }

    fun updateVolume(volume: Float) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = repository.getSettingsDirect()
            updateSettingsUseCase(current.copy(reminderVolume = volume).toDomain())
        }
    }

    fun toggleVoiceInstructions(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = repository.getSettingsDirect()
            updateSettingsUseCase(current.copy(voiceInstructionsEnabled = enabled).toDomain())
        }
    }

    // Alarm Responses delegated to Domain UseCases
    fun completeReminder(reminder: Reminder, photoBitmap: Bitmap? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            ReminderReceiver.cancelMissedCheck(context, reminder.id)
            var photoPath: String? = null

            // Save bitmap locally if photo proof is captured
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

            completeReminderUseCase(reminder.id, photoPath, "Completed on device")

            // Clear alert
            GlobalAlarmState.clearAlert(context)

            // Cancel notification
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
            notificationManager?.cancel(reminder.id.toInt())
        }
    }

    fun snoozeReminder(reminder: Reminder) {
        viewModelScope.launch(Dispatchers.IO) {
            ReminderReceiver.cancelMissedCheck(context, reminder.id)
            snoozeReminderUseCase(reminder.id, snoozeMinutes = 5)

            // Clear alert
            GlobalAlarmState.clearAlert(context)

            // Cancel notification
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
            notificationManager?.cancel(reminder.id.toInt())
        }
    }

    fun updateCaregiverProfile(name: String, pin: String) {
        viewModelScope.launch(Dispatchers.IO) {
            updateCaregiverProfileUseCase(name, pin)
        }
    }

    fun updatePatientProfile(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            updatePatientProfileUseCase(name)
        }
    }

    // PIN Authentication delegated to Domain UseCases
    fun verifyCaregiverPin(inputPin: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val isValid = verifyCaregiverPinUseCase(inputPin)
            launch(Dispatchers.Main) {
                onResult(isValid)
            }
        }
    }

    fun setCaregiverPin(rawPin: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val success = setCaregiverPinUseCase(rawPin)
            launch(Dispatchers.Main) {
                onResult(success)
            }
        }
    }

    // Pairing Logic delegated to Domain UseCases
    fun generatePairingCode(onResult: (String?, String?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val code = generatePairingCodeUseCase(settings.value.patientName)
                CrashlyticsManager.log("Pairing", "Pairing code generated successfully")
                launch(Dispatchers.Main) {
                    onResult(code, null)
                }
            } catch (e: Exception) {
                CrashlyticsManager.recordNonFatal(e, "ViewModel generatePairingCode error")
                launch(Dispatchers.Main) {
                    onResult(null, e.message ?: "Failed to generate pairing code")
                }
            }
        }
    }

    fun requestPairing(code: String, onResult: (com.aistudio.remindcare.data.SyncResult) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            requestPairingUseCase(code).collect { domainResult ->
                val entityResult = domainResult.toData()
                if (entityResult is com.aistudio.remindcare.data.SyncResult.Success) {
                    val current = repository.getSettingsDirect()
                    repository.saveSettings(
                        current.copy(
                            role = "Caregiver",
                            isPaired = true,
                            connectedPatientId = entityResult.patientId,
                            connectedPatientName = entityResult.patientName
                        )
                    )
                }
                launch(Dispatchers.Main) {
                    onResult(entityResult)
                }
            }
        }
    }

    fun listenForPairingRequest(code: String, onRequest: (com.aistudio.remindcare.data.PairingRequest?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            listenPairingRequestUseCase(code).collect { domainReq ->
                val entityReq = domainReq?.toData()
                launch(Dispatchers.Main) {
                    onRequest(entityReq)
                }
            }
        }
    }

    fun approvePairingRequest(code: String, caregiverId: String, caregiverName: String, onComplete: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            approvePairingUseCase(code, caregiverId, caregiverName)
            launch(Dispatchers.Main) {
                onComplete()
            }
        }
    }

    fun declinePairingRequest(code: String, onComplete: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            declinePairingUseCase(code)
            launch(Dispatchers.Main) {
                onComplete()
            }
        }
    }

    fun disconnectPairing() {
        viewModelScope.launch(Dispatchers.IO) {
            disconnectPairingUseCase()
        }
    }

    class Factory(
        private val application: Application,
        private val repository: RemindCareRepository,
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
