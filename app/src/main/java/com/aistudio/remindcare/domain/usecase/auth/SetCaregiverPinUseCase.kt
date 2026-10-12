package com.aistudio.remindcare.domain.usecase.auth

import com.aistudio.remindcare.domain.repository.SettingsRepository
import com.aistudio.remindcare.util.HashUtils

class SetCaregiverPinUseCase(
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(rawPin: String): Boolean {
        if (rawPin.isNotBlank() && rawPin.length != 4) {
            return false // Invalid PIN format
        }
        val settings = settingsRepository.getSettingsDirect()
        val hashedPin = if (rawPin.isNotBlank()) HashUtils.hashPin(rawPin) else ""
        settingsRepository.updateSettings(settings.copy(caregiverPin = hashedPin))
        return true
    }
}
