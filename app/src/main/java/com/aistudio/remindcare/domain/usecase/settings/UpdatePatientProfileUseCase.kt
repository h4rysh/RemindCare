package com.aistudio.remindcare.domain.usecase.settings

import com.aistudio.remindcare.domain.repository.SettingsRepository

class UpdatePatientProfileUseCase(
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(patientName: String) {
        val current = settingsRepository.getSettingsDirect()
        settingsRepository.updateSettings(current.copy(patientName = patientName))
    }
}
