package com.aistudio.remindcare.domain.usecase.reminder

import com.aistudio.remindcare.domain.model.Reminder
import com.aistudio.remindcare.domain.repository.AlarmSchedulerGateway
import com.aistudio.remindcare.domain.repository.ReminderRepository

class SaveReminderUseCase(
    private val reminderRepository: ReminderRepository,
    private val alarmScheduler: AlarmSchedulerGateway
) {
    suspend operator fun invoke(reminder: Reminder): Long {
        val updatedReminder = reminder.copy(updatedAtMillis = System.currentTimeMillis())
        val id = if (reminder.id == 0L) {
            reminderRepository.insertReminder(updatedReminder)
        } else {
            reminderRepository.updateReminder(updatedReminder)
            reminder.id
        }
        val persisted = reminder.copy(id = id, updatedAtMillis = updatedReminder.updatedAtMillis)
        if (persisted.isActive && !persisted.isDeleted) {
            alarmScheduler.scheduleReminder(persisted)
        } else {
            alarmScheduler.cancelReminder(persisted.id)
        }
        return id
    }
}
