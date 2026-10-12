package com.aistudio.remindcare.domain.usecase.reminder

import com.aistudio.remindcare.domain.model.Reminder
import com.aistudio.remindcare.domain.repository.AlarmSchedulerGateway
import com.aistudio.remindcare.domain.repository.ReminderRepository

class DeleteReminderUseCase(
    private val reminderRepository: ReminderRepository,
    private val alarmScheduler: AlarmSchedulerGateway
) {
    suspend operator fun invoke(reminder: Reminder) {
        alarmScheduler.cancelReminder(reminder.id)
        reminderRepository.softDeleteReminder(reminder.id)
    }
}
