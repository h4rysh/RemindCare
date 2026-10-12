package com.aistudio.remindcare.domain.model

/**
 * Pure Kotlin Domain Model for Medication and Reminder History logs.
 * Free from framework annotations.
 */
data class ReminderHistory(
    val id: Long = 0,
    val uuid: String = java.util.UUID.randomUUID().toString(),
    val reminderId: Long = 0,
    val title: String = "",
    val type: String = "General",
    val scheduledTime: String = "", // Date + Time
    val completedTime: String? = null, // Date + Time when completed
    val status: String = "COMPLETED", // "COMPLETED", "MISSED", "SNOOZED", "SKIPPED"
    val photoPath: String? = null, // Path to local photo if confirmation required
    val notes: String? = null,
    val isDeleted: Boolean = false,
    val updatedAtMillis: Long = System.currentTimeMillis()
)
