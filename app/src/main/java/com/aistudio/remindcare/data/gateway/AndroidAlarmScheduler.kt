package com.aistudio.remindcare.data.gateway

import android.content.Context
import com.aistudio.remindcare.data.mapper.toEntity
import com.aistudio.remindcare.domain.model.Reminder
import com.aistudio.remindcare.domain.repository.AlarmSchedulerGateway
import com.aistudio.remindcare.util.ReminderScheduler

class AndroidAlarmScheduler(private val context: Context) : AlarmSchedulerGateway {
    override fun scheduleReminder(reminder: Reminder) {
        ReminderScheduler.scheduleReminder(context, reminder.toEntity())
    }

    override fun cancelReminder(reminderId: Long) {
        ReminderScheduler.cancelReminder(context, reminderId)
    }

    override fun rescheduleAll(reminders: List<Reminder>) {
        reminders.forEach { reminder ->
            ReminderScheduler.scheduleReminder(context, reminder.toEntity())
        }
    }
}
