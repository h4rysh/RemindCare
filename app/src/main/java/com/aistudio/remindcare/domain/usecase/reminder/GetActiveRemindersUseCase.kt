package com.aistudio.remindcare.domain.usecase.reminder

import com.aistudio.remindcare.domain.model.Reminder
import com.aistudio.remindcare.domain.repository.ReminderRepository
import kotlinx.coroutines.flow.Flow

class GetActiveRemindersUseCase(
    private val reminderRepository: ReminderRepository
) {
    operator fun invoke(): Flow<List<Reminder>> {
        return reminderRepository.getActiveReminders()
    }
}
