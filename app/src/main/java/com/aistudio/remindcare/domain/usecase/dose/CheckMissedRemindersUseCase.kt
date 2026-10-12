package com.aistudio.remindcare.domain.usecase.dose

import com.aistudio.remindcare.domain.model.ReminderHistory
import com.aistudio.remindcare.domain.repository.HistoryRepository
import com.aistudio.remindcare.domain.repository.ReminderRepository
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class CheckMissedRemindersUseCase(
    private val reminderRepository: ReminderRepository,
    private val historyRepository: HistoryRepository
) {
    suspend operator fun invoke(): List<ReminderHistory> {
        val reminders = reminderRepository.getRemindersDirect()
        val history = historyRepository.getHistoryDirect()

        val now = Date()
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(now)
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val currentCalendar = Calendar.getInstance().apply { time = now }

        val missedList = mutableListOf<ReminderHistory>()

        for (reminder in reminders) {
            if (!reminder.isActive || reminder.isDeleted) continue

            val parts = reminder.time.split(":")
            if (parts.size != 2) continue
            val hour = parts[0].toIntOrNull() ?: continue
            val minute = parts[1].toIntOrNull() ?: continue

            val reminderCalendar = Calendar.getInstance().apply {
                time = now
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            // If reminder time was > 30 minutes ago today and has no logged history today
            val thirtyMinutesAgo = (currentCalendar.timeInMillis - reminderCalendar.timeInMillis) > (30 * 60 * 1000)
            if (thirtyMinutesAgo && currentCalendar.timeInMillis > reminderCalendar.timeInMillis) {
                val hasLoggedToday = history.any { h ->
                    h.reminderId == reminder.id && h.scheduledTime.startsWith(todayStr)
                }

                if (!hasLoggedToday) {
                    val missedHistory = ReminderHistory(
                        reminderId = reminder.id,
                        title = reminder.title,
                        type = reminder.type,
                        scheduledTime = "$todayStr ${reminder.time}",
                        completedTime = null,
                        status = "MISSED",
                        notes = "Missed scheduled dose",
                        updatedAtMillis = System.currentTimeMillis()
                    )
                    historyRepository.insertHistory(missedHistory)
                    missedList.add(missedHistory)
                }
            }
        }
        return missedList
    }
}
