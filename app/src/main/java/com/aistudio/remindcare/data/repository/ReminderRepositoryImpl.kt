package com.aistudio.remindcare.data.repository

import com.aistudio.remindcare.data.AppDatabase
import com.aistudio.remindcare.data.SyncRepository
import com.aistudio.remindcare.data.mapper.toDomain
import com.aistudio.remindcare.data.mapper.toEntity
import com.aistudio.remindcare.domain.model.Reminder
import com.aistudio.remindcare.domain.repository.ReminderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ReminderRepositoryImpl(
    private val database: AppDatabase,
    private val syncRepository: SyncRepository
) : ReminderRepository {

    private val reminderDao = database.reminderDao()

    override fun getAllReminders(): Flow<List<Reminder>> {
        return reminderDao.getAllReminders().map { list -> list.map { it.toDomain() } }
    }

    override fun getActiveReminders(): Flow<List<Reminder>> {
        return reminderDao.getActiveReminders().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getReminderById(id: Long): Reminder? {
        return reminderDao.getReminderById(id)?.toDomain()
    }

    override suspend fun getReminderByUuid(uuid: String): Reminder? {
        return reminderDao.getReminderByUuid(uuid)?.toDomain()
    }

    override suspend fun insertReminder(reminder: Reminder): Long {
        val entity = reminder.toEntity().copy(
            updatedAtMillis = System.currentTimeMillis(),
            isDeleted = false
        )
        return reminderDao.insertReminder(entity)
    }

    override suspend fun updateReminder(reminder: Reminder) {
        val entity = reminder.toEntity().copy(
            updatedAtMillis = System.currentTimeMillis()
        )
        reminderDao.updateReminder(entity)
    }

    override suspend fun deleteReminder(reminder: Reminder) {
        softDeleteReminder(reminder.id)
    }

    override suspend fun softDeleteReminder(id: Long) {
        reminderDao.softDeleteReminderById(id, System.currentTimeMillis())
    }

    override suspend fun getRemindersDirect(): List<Reminder> {
        return reminderDao.getActiveRemindersDirect().map { it.toDomain() }
    }

    override suspend fun prepopulateDefaults() {
        val existing = reminderDao.getActiveRemindersDirect()
        if (existing.isEmpty()) {
            val defaults = listOf(
                Reminder(title = "Morning Medicine", type = "Medicine", time = "08:00", repeat = "Daily", medicineName = "Multivitamin", photoRequired = true),
                Reminder(title = "Drink Water", type = "Water", time = "10:30", repeat = "Daily", photoRequired = false),
                Reminder(title = "Eat Lunch", type = "Meal", time = "12:30", repeat = "Daily", photoRequired = false),
                Reminder(title = "Afternoon Walk", type = "Exercise", time = "16:00", repeat = "Daily", photoRequired = false),
                Reminder(title = "Sleep Prep", type = "Sleep", time = "21:30", repeat = "Daily", photoRequired = false)
            )
            for (r in defaults) {
                insertReminder(r)
            }
        }
    }

    override suspend fun syncRemindersToCloud(patientId: String, reminders: List<Reminder>) {
        syncRepository.syncRemindersToCloud(patientId, reminders.map { it.toEntity() })
    }

    override fun observeCloudReminders(patientId: String): Flow<List<Reminder>> {
        return syncRepository.listenForCloudReminders(patientId).map { list -> list.map { it.toDomain() } }
    }
}
