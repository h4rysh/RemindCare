package com.aistudio.remindcare.domain.usecase.settings

import com.aistudio.remindcare.domain.repository.SettingsRepository
import com.aistudio.remindcare.util.HashUtils

class UpdateCaregiverProfileUseCase(
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(caregiverName: String, rawPin: String? = null) {
        val current = settingsRepository.getSettingsDirect()
        val hashedPin = if (!rawPin.isNullOrBlank()) {
            HashUtils.hashPin(rawPin)
        } else {
            current.caregiverPin
        }
        settingsRepository.updateSettings(
            current.copy(
                caregiverName = caregiverName,
                caregiverPin = hashedPin
            )
        )
    }
}
