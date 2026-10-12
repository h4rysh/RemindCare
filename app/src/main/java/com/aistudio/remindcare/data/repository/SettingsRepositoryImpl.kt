package com.aistudio.remindcare.data.repository

import com.aistudio.remindcare.data.AppDatabase
import com.aistudio.remindcare.data.mapper.toDomain
import com.aistudio.remindcare.data.mapper.toEntity
import com.aistudio.remindcare.domain.model.AppSettings
import com.aistudio.remindcare.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsRepositoryImpl(
    private val database: AppDatabase
) : SettingsRepository {

    private val settingsDao = database.settingsDao()

    override fun getSettings(): Flow<AppSettings> {
        return settingsDao.getSettingsFlow().map { entity ->
            entity?.toDomain() ?: AppSettings()
        }
    }

    override suspend fun getSettingsDirect(): AppSettings {
        val existing = settingsDao.getSettingsDirect()
        if (existing == null) {
            val initial = AppSettings()
            settingsDao.saveSettings(initial.toEntity())
            return initial
        }
        return existing.toDomain()
    }

    override suspend fun updateSettings(settings: AppSettings) {
        settingsDao.saveSettings(settings.toEntity())
    }
}
