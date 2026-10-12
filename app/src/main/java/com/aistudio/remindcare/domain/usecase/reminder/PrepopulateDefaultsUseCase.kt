package com.aistudio.remindcare.domain.usecase.reminder

import com.aistudio.remindcare.domain.repository.ReminderRepository

class PrepopulateDefaultsUseCase(
    private val reminderRepository: ReminderRepository
) {
    suspend operator fun invoke() {
        reminderRepository.prepopulateDefaults()
    }
}
