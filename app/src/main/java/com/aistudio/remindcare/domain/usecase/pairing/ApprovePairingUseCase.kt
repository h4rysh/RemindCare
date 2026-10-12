package com.aistudio.remindcare.domain.usecase.pairing

import com.aistudio.remindcare.domain.repository.HistoryRepository
import com.aistudio.remindcare.domain.repository.PairingRepository
import com.aistudio.remindcare.domain.repository.ReminderRepository
import com.aistudio.remindcare.domain.repository.SettingsRepository

class ApprovePairingUseCase(
    private val pairingRepository: PairingRepository,
    private val settingsRepository: SettingsRepository,
    private val reminderRepository: ReminderRepository,
    private val historyRepository: HistoryRepository
) {
    suspend operator fun invoke(code: String, caregiverId: String, caregiverName: String): Boolean {
        val success = pairingRepository.approvePairingRequest(code, caregiverId, caregiverName)
        if (success) {
            val settings = settingsRepository.getSettingsDirect()
            val patientId = pairingRepository.getDeviceId()
            settingsRepository.updateSettings(
                settings.copy(
                    isPaired = true,
                    connectedCaregiverId = caregiverId,
                    connectedCaregiverName = caregiverName,
                    connectedPatientId = patientId
                )
            )

            // Trigger immediate cloud push of existing local state
            val localReminders = reminderRepository.getRemindersDirect()
            if (localReminders.isNotEmpty()) {
                reminderRepository.syncRemindersToCloud(patientId, localReminders)
            }
            val localHistory = historyRepository.getHistoryDirect()
            if (localHistory.isNotEmpty()) {
                historyRepository.syncHistoryToCloud(patientId, localHistory)
            }
        }
        return success
    }
}
