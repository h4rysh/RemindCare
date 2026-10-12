package com.aistudio.remindcare.domain.repository

import com.aistudio.remindcare.domain.model.AppSettings
import kotlinx.coroutines.flow.Flow

/**
 * Domain Repository interface for Application Settings and Caregiver Configuration.
 * Pure Kotlin contract.
 */
interface SettingsRepository {
    fun getSettings(): Flow<AppSettings>
    suspend fun getSettingsDirect(): AppSettings
    suspend fun updateSettings(settings: AppSettings)
}
