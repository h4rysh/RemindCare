package com.aistudio.remindcare.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders WHERE isDeleted = 0 ORDER BY time ASC")
    fun getAllReminders(): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE isActive = 1 AND isDeleted = 0 ORDER BY time ASC")
    fun getActiveReminders(): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE isActive = 1 AND isDeleted = 0 ORDER BY time ASC")
    suspend fun getActiveRemindersDirect(): List<Reminder>

    @Query("SELECT * FROM reminders WHERE id = :id AND isDeleted = 0 LIMIT 1")
    suspend fun getReminderById(id: Long): Reminder?

    @Query("SELECT * FROM reminders WHERE uuid = :uuid LIMIT 1")
    suspend fun getReminderByUuid(uuid: String): Reminder?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: Reminder): Long

    @Update
    suspend fun updateReminder(reminder: Reminder)

    @Delete
    suspend fun deleteReminder(reminder: Reminder)

    @Query("UPDATE reminders SET isDeleted = 1, updatedAtMillis = :updatedAt WHERE id = :id")
    suspend fun softDeleteReminderById(id: Long, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteReminderById(id: Long)
}

@Dao
interface HistoryDao {
    @Query("SELECT * FROM reminder_history WHERE isDeleted = 0 ORDER BY id DESC")
    fun getAllHistory(): Flow<List<ReminderHistory>>
    
    @Query("SELECT * FROM reminder_history WHERE reminderId = :reminderId AND isDeleted = 0")
    fun getHistoryForReminderFlow(reminderId: Long): Flow<List<ReminderHistory>>
    
    @Query("SELECT * FROM reminder_history WHERE reminderId = :reminderId AND isDeleted = 0")
    suspend fun getHistoryForReminder(reminderId: Long): List<ReminderHistory>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: ReminderHistory)

    @Query("SELECT * FROM reminder_history WHERE isDeleted = 0 ORDER BY id DESC")
    suspend fun getHistoryDirect(): List<ReminderHistory>

    @Query("SELECT * FROM reminder_history WHERE uuid = :uuid LIMIT 1")
    suspend fun getHistoryByUuid(uuid: String): ReminderHistory?

    @Query("UPDATE reminder_history SET isDeleted = 1, updatedAtMillis = :updatedAt WHERE id = :id")
    suspend fun softDeleteHistoryById(id: Long, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM reminder_history WHERE id = :id")
    suspend fun deleteHistoryById(id: Long)

    @Query("DELETE FROM reminder_history")
    suspend fun clearAllHistory()
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    fun getSettingsFlow(): Flow<AppSettings?>

    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettingsDirect(): AppSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: AppSettings)
}

@Dao
interface OutboxDao {
    @Query("SELECT * FROM sync_outbox ORDER BY id ASC")
    suspend fun getAllPending(): List<SyncOutboxEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: SyncOutboxEntity): Long

    @Query("DELETE FROM sync_outbox WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM sync_outbox")
    suspend fun clearAll()
}
