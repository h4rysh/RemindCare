package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RemindCareRepository(private val database: AppDatabase, context: android.content.Context) {
    val syncRepository: SyncRepository = FirebaseSyncRepository(context)
    private val reminderDao = database.reminderDao()
    private val historyDao = database.historyDao()
    private val settingsDao = database.settingsDao()

    // Reminders
    val allReminders: Flow<List<Reminder>> = reminderDao.getAllReminders()
    val activeReminders: Flow<List<Reminder>> = reminderDao.getActiveReminders()

    suspend fun getReminderById(id: Long): Reminder? {
        return reminderDao.getReminderById(id)
    }

    suspend fun insertReminder(reminder: Reminder): Long {
        return reminderDao.insertReminder(reminder)
    }

    suspend fun updateReminder(reminder: Reminder) {
        reminderDao.updateReminder(reminder)
    }

    suspend fun deleteReminder(reminder: Reminder) {
        reminderDao.deleteReminder(reminder)
    }

    suspend fun deleteReminderById(id: Long) {
        reminderDao.deleteReminderById(id)
    }

    // History
    val allHistory: Flow<List<ReminderHistory>> = historyDao.getAllHistory()

    suspend fun insertHistory(history: ReminderHistory): Long {
        return historyDao.insertHistory(history)
    }

    suspend fun deleteHistoryById(id: Long) {
        historyDao.deleteHistoryById(id)
    }

    suspend fun clearHistory() {
        historyDao.clearAllHistory()
    }

    // Settings
    val settingsFlow: Flow<AppSettings> = settingsDao.getSettingsFlow().map { settings ->
        settings ?: AppSettings() // Fallback to default if not inserted yet
    }

    suspend fun getSettingsDirect(): AppSettings {
        return settingsDao.getSettingsDirect() ?: AppSettings()
    }

    suspend fun saveSettings(settings: AppSettings) {
        settingsDao.saveSettings(settings)
    }
}
