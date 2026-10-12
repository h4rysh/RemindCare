package com.aistudio.remindcare.domain.repository

import com.aistudio.remindcare.domain.model.Reminder

/**
 * Domain Gateway interface for exact alarm scheduling.
 * Keeps hardware alarm APIs decoupled from domain logic.
 */
interface AlarmSchedulerGateway {
    fun scheduleReminder(reminder: Reminder)
    fun cancelReminder(reminderId: Long)
    fun rescheduleAll(reminders: List<Reminder>)
}
