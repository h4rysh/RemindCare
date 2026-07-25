package com.example.util

import android.content.Context
import android.util.Log
import com.example.data.Reminder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object GlobalAlarmState {
    private const val TAG = "GlobalAlarmState"
    
    private val _activeAlert = MutableStateFlow<Reminder?>(null)
    val activeAlert = _activeAlert.asStateFlow()

    fun triggerAlert(context: Context, reminder: Reminder) {
        Log.d(TAG, "Triggering alarm alert for: ${reminder.title}")
        _activeAlert.value = reminder
        
        // Retrieve volume and voice settings from AppSettings
        // Since we're in a broadcast receiver/background context, we can launch a quick thread to read it,
        // or use the default settings values first, then play.
        // Let's retrieve it from database or fallback to standard values safely.
        val alarmController = AlarmController.getInstance(context)
        
        val detailText = buildString {
            if (!reminder.medicineName.isNullOrBlank()) append("Medicine: ${reminder.medicineName}. ")
            if (!reminder.medicineQuantity.isNullOrBlank()) append("Quantity: ${reminder.medicineQuantity}. ")
            if (!reminder.medicineLocation.isNullOrBlank()) append("Location: ${reminder.medicineLocation}. ")
            if (!reminder.additionalNotes.isNullOrBlank()) append("Notes: ${reminder.additionalNotes}.")
        }

        // Start loud looping audio, vibration, and Text-to-Speech
        alarmController.startAlarm(
            reminderTitle = reminder.title,
            detailText = detailText,
            volume = 0.9f,
            enableVoice = true
        )
    }

    fun clearAlert(context: Context) {
        Log.d(TAG, "Clearing active alarm alert")
        _activeAlert.value = null
        AlarmController.getInstance(context).stopAlarm()
    }
}
