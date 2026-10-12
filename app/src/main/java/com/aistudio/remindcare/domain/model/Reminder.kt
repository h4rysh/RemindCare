package com.aistudio.remindcare.domain.model

/**
 * Pure Kotlin Domain Model for RemindCare Reminders.
 * Free from framework annotations (zero Android/Room/Compose dependencies).
 */
data class Reminder(
    val id: Long = 0,
    val uuid: String = java.util.UUID.randomUUID().toString(),
    val title: String = "",
    val type: String = "Medicine", // "Medicine", "Meal", "Water", "Exercise", "Appointment", "Sleep", "General", "Custom"
    val time: String = "08:00", // "HH:mm" (e.g. "08:30")
    val repeat: String = "Daily", // "Daily", "Once", "Weekly"
    val medicineName: String? = null,
    val medicineQuantity: String? = null,
    val medicineLocation: String? = null,
    val photoRequired: Boolean = false,
    val additionalNotes: String? = null,
    val imageUri: String? = null,
    val isActive: Boolean = true,
    val soundUri: String? = null,
    val isDeleted: Boolean = false,
    val updatedAtMillis: Long = System.currentTimeMillis()
)
