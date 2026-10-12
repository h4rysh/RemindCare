package com.aistudio.remindcare.util

import kotlinx.coroutines.launch
import android.content.Context
import android.util.Log
import com.aistudio.remindcare.data.Reminder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object GlobalAlarmState {
    private const val TAG = "GlobalAlarmState"
    
    private val _activeAlert = MutableStateFlow<Reminder?>(null)
    val activeAlert = _activeAlert.asStateFlow()

    fun triggerAlert(context: Context, reminder: Reminder) {
        Log.d(TAG, "Triggering alarm alert for: ${reminder.title}")
        _activeAlert.value = reminder
        
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            val db = com.aistudio.remindcare.data.AppDatabase.getDatabase(context)
            val settings = db.settingsDao().getSettingsDirect() ?: com.aistudio.remindcare.data.AppSettings()
            
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
                volume = settings.reminderVolume,
                enableVoice = settings.voiceInstructionsEnabled,
                ringtoneMode = settings.ringtone
            )
        }
    }

    fun clearAlert(context: Context) {
        Log.d(TAG, "Clearing active alarm alert")
        _activeAlert.value = null
        AlarmController.getInstance(context).stopAlarm()
    }
}
