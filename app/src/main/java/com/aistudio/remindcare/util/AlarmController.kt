package com.aistudio.remindcare.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
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
    private var audioFocusRequest: AudioFocusRequest? = null

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

    fun startAlarm(reminderTitle: String, detailText: String, volume: Float = 0.8f, enableVoice: Boolean = true, ringtoneMode: String = "Default") {
        stopAlarm() // Stop any previous alarms first

        // Set system alarm volume based on app volume setting
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            if (audioManager != null) {
                val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
                val desiredLevel = (maxVolume * volume).toInt().coerceIn(1, maxVolume)
                audioManager.setStreamVolume(AudioManager.STREAM_ALARM, desiredLevel, 0)
            }
        } catch (e: Exception) {
            Log.e("AlarmController", "Failed to set alarm stream volume", e)
        }

        // Request audio focus for alarm
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            if (audioManager != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
                        .setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_ALARM)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build()
                        )
                        .build()
                    audioFocusRequest = focusRequest
                    audioManager.requestAudioFocus(focusRequest)
                } else {
                    @Suppress("DEPRECATION")
                    audioManager.requestAudioFocus(null, AudioManager.STREAM_ALARM, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
                }
            }
        } catch (e: Exception) {
            Log.e("AlarmController", "Failed to request audio focus", e)
        }

        // 1. Play Alarm Sound
        try {
            val alarmUri: Uri = when(ringtoneMode) {
                "Loud Alarm" -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                "Soft Chime" -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                else -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            } ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            
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
            Log.e("AlarmController", "Failed to start MediaPlayer with system URI, trying fallback", e)
            // Fallback: try the default ringtone, or generate a tone
            try {
                val fallbackUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                if (fallbackUri != null) {
                    mediaPlayer = MediaPlayer().apply {
                        setDataSource(context, fallbackUri)
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
                } else {
                    Log.e("AlarmController", "No fallback audio URI available")
                }
            } catch (fallbackEx: Exception) {
                Log.e("AlarmController", "Fallback audio also failed", fallbackEx)
            }
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
        // Abandon audio focus
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            if (audioManager != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    audioFocusRequest?.let {
                        audioManager.abandonAudioFocusRequest(it)
                    }
                } else {
                    @Suppress("DEPRECATION")
                    audioManager.abandonAudioFocus(null)
                }
            }
        } catch (e: Exception) {
            Log.e("AlarmController", "Error abandoning audio focus", e)
        }

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
