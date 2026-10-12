package com.aistudio.remindcare.domain.model

/**
 * Pure Kotlin Domain Model for RemindCare Application Settings.
 * Free from framework annotations.
 */
data class AppSettings(
    val id: Int = 1,
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
