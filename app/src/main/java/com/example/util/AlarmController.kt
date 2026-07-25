package com.example.util

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class AlarmController private constructor(private val context: Context) : TextToSpeech.OnInitListener {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private var pendingSpeechText: String? = null

    init {
        // Initialize TTS
        try {
            tts = TextToSpeech(context, this)
        } catch (e: Exception) {
            Log.e("AlarmController", "TTS Init failed", e)
        }

        // Initialize Vibrator
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.getDefault())
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.setLanguage(Locale.US)
            }
            isTtsReady = true
            pendingSpeechText?.let {
                speakText(it)
                pendingSpeechText = null
            }
        }
    }

    fun startAlarm(reminderTitle: String, detailText: String, volume: Float = 0.8f, enableVoice: Boolean = true) {
        stopAlarm() // Stop any previous alarms first

        // 1. Play Alarm Sound
        try {
            val alarmUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM) 
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, alarmUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                setVolume(volume, volume)
                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.e("AlarmController", "Failed to start MediaPlayer", e)
        }

        // 2. Play Looping Vibration
        try {
            if (vibrator?.hasVibrator() == true) {
                val pattern = longArrayOf(0, 1000, 1000) // Start immediately, vibrate 1s, pause 1s
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0)) // Loop from index 0
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(pattern, 0)
                }
            }
        } catch (e: Exception) {
            Log.e("AlarmController", "Failed to start vibration", e)
        }

        // 3. Spoken Announcement (Text-To-Speech)
        if (enableVoice) {
            val announcement = "Attention. Time to complete task: $reminderTitle. $detailText"
            if (isTtsReady) {
                speakText(announcement)
            } else {
                pendingSpeechText = announcement
            }
        }
    }

    private fun speakText(text: String) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "RemindCareAlarm")
            } else {
                @Suppress("DEPRECATION")
                tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null)
            }
        } catch (e: Exception) {
            Log.e("AlarmController", "TTS speak failed", e)
        }
    }

    fun stopAlarm() {
        // Stop sound
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
        } catch (e: Exception) {
            Log.e("AlarmController", "Error releasing MediaPlayer", e)
        } finally {
            mediaPlayer = null
        }

        // Stop vibration
        try {
            vibrator?.cancel()
        } catch (e: Exception) {
            Log.e("AlarmController", "Error stopping vibrator", e)
        }

        // Stop TTS
        try {
            tts?.stop()
        } catch (e: Exception) {
            Log.e("AlarmController", "Error stopping TTS", e)
        }
    }

    fun release() {
        stopAlarm()
        try {
            tts?.shutdown()
        } catch (e: Exception) {
            Log.e("AlarmController", "Error shutting down TTS", e)
        } finally {
            tts = null
            isTtsReady = false
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: AlarmController? = null

        fun getInstance(context: Context): AlarmController {
            return INSTANCE ?: synchronized(this) {
                val instance = AlarmController(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
