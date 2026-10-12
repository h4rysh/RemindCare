package com.aistudio.remindcare.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RemindCareRepository(val database: AppDatabase, context: android.content.Context) {
    var syncRepository: SyncRepository = FirebaseSyncRepository(context)
    private val reminderDao = database.reminderDao()
    private val historyDao = database.historyDao()
    private val settingsDao = database.settingsDao()
    private val outboxDao = database.outboxDao()

    // Reminders
    val allReminders: Flow<List<Reminder>> = reminderDao.getAllReminders()
    val activeReminders: Flow<List<Reminder>> = reminderDao.getActiveReminders()

    suspend fun getReminderById(id: Long): Reminder? {
        return reminderDao.getReminderById(id)
    }

    suspend fun insertReminder(reminder: Reminder): Long {
        val withTimestamp = reminder.copy(
            updatedAtMillis = System.currentTimeMillis(),
            isDeleted = false
        )
        return reminderDao.insertReminder(withTimestamp)
    }

    suspend fun updateReminder(reminder: Reminder) {
        val withTimestamp = reminder.copy(
            updatedAtMillis = System.currentTimeMillis()
        )
        reminderDao.updateReminder(withTimestamp)
    }

    suspend fun deleteReminder(reminder: Reminder) {
        reminderDao.softDeleteReminderById(reminder.id, System.currentTimeMillis())
    }

    suspend fun deleteReminderById(id: Long) {
        reminderDao.softDeleteReminderById(id, System.currentTimeMillis())
    }

    // History
    val allHistory: Flow<List<ReminderHistory>> = historyDao.getAllHistory()

    suspend fun insertHistory(history: ReminderHistory) {
        val withTimestamp = history.copy(
            updatedAtMillis = System.currentTimeMillis(),
            isDeleted = false
        )
        historyDao.insertHistory(withTimestamp)
    }
    
    suspend fun getHistoryDirect() = historyDao.getHistoryDirect()

    suspend fun deleteHistoryById(id: Long) {
        historyDao.softDeleteHistoryById(id, System.currentTimeMillis())
    }

    suspend fun clearHistory() {
        historyDao.clearAllHistory()
    }

    // Settings
    val settingsFlow: Flow<AppSettings> = settingsDao.getSettingsFlow().map { settings ->
        settings ?: AppSettings()
    }

    suspend fun getSettingsDirect(): AppSettings {
        val existing = settingsDao.getSettingsDirect()
        if (existing == null) {
            val initial = AppSettings()
            settingsDao.saveSettings(initial)
            return initial
        }
        return existing
    }

    suspend fun saveSettings(settings: AppSettings) {
        settingsDao.saveSettings(settings)
    }
}
