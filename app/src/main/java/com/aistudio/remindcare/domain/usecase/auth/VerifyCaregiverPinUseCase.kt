package com.aistudio.remindcare.domain.usecase.auth

import com.aistudio.remindcare.domain.repository.SettingsRepository
import com.aistudio.remindcare.util.HashUtils

class VerifyCaregiverPinUseCase(
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(inputPin: String): Boolean {
        if (inputPin.isBlank()) return false
        val settings = settingsRepository.getSettingsDirect()
        val storedHash = settings.caregiverPin
        if (storedHash.isBlank()) return true // No PIN configured
        return HashUtils.verifyPin(inputPin, storedHash)
    }
}
