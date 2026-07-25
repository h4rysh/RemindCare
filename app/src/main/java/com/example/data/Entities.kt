package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val type: String, // "Medicine", "Meal", "Water", "Exercise", "Appointment", "Sleep", "General", "Custom"
    val time: String, // "HH:mm" (e.g. "08:30")
    val repeat: String, // "Daily", "Once", "Weekly"
    val medicineName: String? = null,
    val medicineQuantity: String? = null,
    val medicineLocation: String? = null,
    val photoRequired: Boolean = false,
    val additionalNotes: String? = null,
    val imageUri: String? = null, // Resource/File Uri of local image or default asset ID
    val isActive: Boolean = true,
    val soundUri: String? = null
)

@Entity(tableName = "reminder_history")
data class ReminderHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val reminderId: Long,
    val title: String,
    val type: String,
    val scheduledTime: String, // Date + Time
    val completedTime: String? = null, // Date + Time when completed
    val status: String, // "COMPLETED", "MISSED", "SNOOZED", "SKIPPED"
    val photoPath: String? = null, // Path to local photo if confirmation required
    val notes: String? = null
)

@Entity(tableName = "app_settings")
data class AppSettings(
    @PrimaryKey val id: Int = 1, // Single row configuration
    val role: String? = null, // "Patient", "Caregiver", or null if not set
    val patientName: String = "",
    val caregiverName: String = "",
    val caregiverPin: String = "",
    val fontSize: String = "Normal", // "Normal", "Large", "Very Large"
    val themeMode: String = "System", // "Light", "Dark", "System"
    val reminderVolume: Float = 0.8f,
    val ringtone: String = "Default", // "Default", "Loud Alarm", "Soft Chime"
    val voiceInstructionsEnabled: Boolean = true,
    val isPaired: Boolean = false,
    val connectedCaregiverName: String = "",
    val connectedCaregiverId: String = "",
    val connectedPatientName: String = "",
    val connectedPatientId: String = ""
)
