package com.aistudio.remindcare.domain.usecase.reminder

import com.aistudio.remindcare.domain.model.Reminder
import com.aistudio.remindcare.domain.repository.ReminderRepository

class GetReminderByIdUseCase(
    private val reminderRepository: ReminderRepository
) {
    suspend operator fun invoke(id: Long): Reminder? {
        return reminderRepository.getReminderById(id)
    }
}
