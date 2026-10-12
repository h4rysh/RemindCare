package com.aistudio.remindcare.domain.repository

import com.aistudio.remindcare.domain.model.Reminder
import kotlinx.coroutines.flow.Flow

/**
 * Domain Repository interface for Reminders.
 * Pure Kotlin contract.
 */
interface ReminderRepository {
    fun getAllReminders(): Flow<List<Reminder>>
    fun getActiveReminders(): Flow<List<Reminder>>
    suspend fun getReminderById(id: Long): Reminder?
    suspend fun getReminderByUuid(uuid: String): Reminder?
    suspend fun insertReminder(reminder: Reminder): Long
    suspend fun updateReminder(reminder: Reminder)
    suspend fun deleteReminder(reminder: Reminder)
    suspend fun softDeleteReminder(id: Long)
    suspend fun getRemindersDirect(): List<Reminder>
    suspend fun prepopulateDefaults()
    suspend fun syncRemindersToCloud(patientId: String, reminders: List<Reminder>)
    fun observeCloudReminders(patientId: String): Flow<List<Reminder>>
}
