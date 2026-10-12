package com.aistudio.remindcare.domain.usecase.settings

import com.aistudio.remindcare.domain.model.AppSettings
import com.aistudio.remindcare.domain.repository.SettingsRepository

class UpdateSettingsUseCase(
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(settings: AppSettings) {
        settingsRepository.updateSettings(settings)
    }
}
