package com.aistudio.remindcare.domain.usecase.settings

import com.aistudio.remindcare.domain.model.AppSettings
import com.aistudio.remindcare.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow

class GetSettingsUseCase(
    private val settingsRepository: SettingsRepository
) {
    operator fun invoke(): Flow<AppSettings> {
        return settingsRepository.getSettings()
    }

    suspend fun getDirect(): AppSettings {
        return settingsRepository.getSettingsDirect()
    }
}
