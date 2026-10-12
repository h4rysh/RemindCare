package com.aistudio.remindcare.domain.usecase.sync

import com.aistudio.remindcare.domain.model.Reminder
import com.aistudio.remindcare.domain.model.ReminderHistory
import com.aistudio.remindcare.domain.repository.HistoryRepository
import com.aistudio.remindcare.domain.repository.ReminderRepository
import kotlinx.coroutines.flow.Flow

class ObserveSyncUseCase(
    private val reminderRepository: ReminderRepository,
    private val historyRepository: HistoryRepository
) {
    fun observeReminders(patientId: String): Flow<List<Reminder>> {
        return reminderRepository.observeCloudReminders(patientId)
    }

    fun observeHistory(patientId: String): Flow<List<ReminderHistory>> {
        return historyRepository.observeCloudHistory(patientId)
    }
}
