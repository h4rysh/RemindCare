package com.aistudio.remindcare.domain.usecase.dose

import com.aistudio.remindcare.domain.model.ReminderHistory
import com.aistudio.remindcare.domain.repository.AlarmSchedulerGateway
import com.aistudio.remindcare.domain.repository.HistoryRepository
import com.aistudio.remindcare.domain.repository.PhotoStorageGateway
import com.aistudio.remindcare.domain.repository.ReminderRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CompleteReminderUseCase(
    private val reminderRepository: ReminderRepository,
    private val historyRepository: HistoryRepository,
    private val alarmScheduler: AlarmSchedulerGateway,
    private val photoStorage: PhotoStorageGateway
) {
    suspend operator fun invoke(
        reminderId: Long,
        photoPathOrUri: String? = null,
        notes: String? = null
    ): ReminderHistory? {
        val reminder = reminderRepository.getReminderById(reminderId) ?: return null
        
        val persistedPhotoPath = if (!photoPathOrUri.isNullOrBlank()) {
            photoStorage.savePhotoProof(reminderId, photoPathOrUri)
        } else null

        val now = Date()
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

        val history = ReminderHistory(
            reminderId = reminder.id,
            title = reminder.title,
            type = reminder.type,
            scheduledTime = "${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(now)} ${reminder.time}",
            completedTime = dateTimeFormat.format(now),
            status = "COMPLETED",
            photoPath = persistedPhotoPath,
            notes = notes,
            updatedAtMillis = System.currentTimeMillis()
        )

        historyRepository.insertHistory(history)

        if (reminder.repeat == "Once") {
            val updated = reminder.copy(isActive = false, updatedAtMillis = System.currentTimeMillis())
            reminderRepository.updateReminder(updated)
            alarmScheduler.cancelReminder(reminder.id)
        } else {
            // Re-schedule for next period
            alarmScheduler.scheduleReminder(reminder)
        }

        return history
    }
}
