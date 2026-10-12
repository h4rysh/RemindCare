package com.aistudio.remindcare.domain.usecase.dose

import com.aistudio.remindcare.domain.model.ReminderHistory
import com.aistudio.remindcare.domain.repository.AlarmSchedulerGateway
import com.aistudio.remindcare.domain.repository.HistoryRepository
import com.aistudio.remindcare.domain.repository.ReminderRepository
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class SnoozeReminderUseCase(
    private val reminderRepository: ReminderRepository,
    private val historyRepository: HistoryRepository,
    private val alarmScheduler: AlarmSchedulerGateway
) {
    suspend operator fun invoke(reminderId: Long, snoozeMinutes: Int = 10): ReminderHistory? {
        val reminder = reminderRepository.getReminderById(reminderId) ?: return null

        val now = Date()
        val calendar = Calendar.getInstance().apply {
            time = now
            add(Calendar.MINUTE, snoozeMinutes)
        }
        val snoozedTimeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(calendar.time)

        val history = ReminderHistory(
            reminderId = reminder.id,
            title = reminder.title,
            type = reminder.type,
            scheduledTime = "${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(now)} ${reminder.time}",
            completedTime = null,
            status = "SNOOZED",
            notes = "Snoozed for $snoozeMinutes minutes until $snoozedTimeStr",
            updatedAtMillis = System.currentTimeMillis()
        )
        historyRepository.insertHistory(history)

        // Reschedule alarm for the snooze time
        val snoozedReminder = reminder.copy(time = snoozedTimeStr)
        alarmScheduler.scheduleReminder(snoozedReminder)

        return history
    }
}
