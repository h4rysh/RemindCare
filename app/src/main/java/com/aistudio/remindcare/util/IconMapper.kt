package com.aistudio.remindcare.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

fun getIconForType(type: String): ImageVector {
    return when(type) {
        "Medicine" -> Icons.Default.MedicalServices
        "Meal" -> Icons.Default.Restaurant
        "Water" -> Icons.Default.WaterDrop
        "Exercise" -> Icons.AutoMirrored.Filled.DirectionsRun
        "Appointment" -> Icons.Default.Event
        "Sleep" -> Icons.Default.Bedtime
        "General" -> Icons.Default.Notifications
        else -> Icons.Default.Star
    }
}

fun getColorForType(type: String): Color {
    return when(type) {
        "Medicine" -> Color(0xFFDC2626) // CoralUrgent
        "Meal" -> Color(0xFFD97706) // AmberWarning
        "Water" -> Color(0xFF2563EB) // Blue
        "Exercise" -> Color(0xFF16A34A) // SageGreen
        "Appointment" -> Color(0xFF7C3AED) // Purple
        "Sleep" -> Color(0xFF4F46E5) // Indigo
        "General" -> Color(0xFF1E3A8A) // NavyPrimary
        else -> Color(0xFF1E3A8A) // NavyPrimary
    }
}
