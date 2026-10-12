package com.aistudio.remindcare.ui.theme

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

/**
 * Tactile feedback manager designed specifically for senior reassurance and accessibility.
 * Provides distinct haptic confirmation for life-critical medication events, errors, and keypresses.
 */
object RemindCareHaptics {

    /**
     * Subtle keypress click for PIN pad and keyboard entry
     */
    fun performKeyClick(haptic: HapticFeedback? = null, context: Context? = null) {
        haptic?.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        context?.let { ctx ->
            vibrate(ctx, longArrayOf(0, 15), intArrayOf(0, 70))
        }
    }

    /**
     * Confirmatory tap for buttons and interactive controls
     */
    fun performTap(haptic: HapticFeedback? = null, context: Context? = null) {
        haptic?.performHapticFeedback(HapticFeedbackType.LongPress)
        context?.let { ctx ->
            vibrate(ctx, longArrayOf(0, 25), intArrayOf(0, 120))
        }
    }

    /**
     * Reassuring double-pulse indicating successful medication intake, photo verification, or pairing
     */
    fun performSuccess(context: Context? = null, haptic: HapticFeedback? = null) {
        haptic?.performHapticFeedback(HapticFeedbackType.LongPress)
        context?.let { ctx ->
            vibrate(ctx, longArrayOf(0, 40, 60, 60), intArrayOf(0, 180, 0, 255))
        }
    }

    /**
     * Warning pulse for incorrect PIN, unauthorized access, or missed reminder alert
     */
    fun performError(context: Context? = null, haptic: HapticFeedback? = null) {
        haptic?.performHapticFeedback(HapticFeedbackType.LongPress)
        context?.let { ctx ->
            vibrate(ctx, longArrayOf(0, 60, 50, 60, 50, 80), intArrayOf(0, 255, 0, 255, 0, 255))
        }
    }

    private fun vibrate(context: Context, timings: LongArray, amplitudes: IntArray) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            } ?: return

            if (!vibrator.hasVibrator()) return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
                vibrator.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(timings, -1)
            }
        } catch (_: Exception) {
            // Ignore if vibration permission or hardware is unavailable
        }
    }
}
