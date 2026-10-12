package com.aistudio.remindcare.domain.usecase.pairing

import com.aistudio.remindcare.domain.repository.PairingRepository
import com.aistudio.remindcare.domain.repository.SettingsRepository

class DisconnectPairingUseCase(
    private val pairingRepository: PairingRepository,
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke() {
        val settings = settingsRepository.getSettingsDirect()
        pairingRepository.disconnect(settings.connectedPatientId, settings.connectedCaregiverId)
        settingsRepository.updateSettings(
            settings.copy(
                isPaired = false,
                connectedCaregiverId = "",
                connectedCaregiverName = "",
                connectedPatientId = "",
                connectedPatientName = ""
            )
        )
    }
}
