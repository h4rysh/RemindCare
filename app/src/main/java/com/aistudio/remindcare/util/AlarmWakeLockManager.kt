package com.aistudio.remindcare.util

import android.annotation.SuppressLint
import android.content.Context
import android.os.PowerManager
import android.util.Log

object AlarmWakeLockManager {
    private const val TAG = "AlarmWakeLockManager"
    private const val WAKE_LOCK_TAG = "RemindCare:AlarmWakeLock"
    private const val DEFAULT_TIMEOUT_MS = 60000L // 60 seconds

    private var wakeLock: PowerManager.WakeLock? = null
    private val lock = Any()

    @SuppressLint("WakelockTimeout")
    fun acquire(context: Context, timeoutMs: Long = DEFAULT_TIMEOUT_MS) {
        synchronized(lock) {
            try {
                if (wakeLock == null) {
                    val powerManager = context.applicationContext.getSystemService(Context.POWER_SERVICE) as? PowerManager
                    wakeLock = powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG)?.apply {
                        setReferenceCounted(false)
                    }
                }
                wakeLock?.let {
                    if (!it.isHeld) {
                        it.acquire(timeoutMs)
                        Log.d(TAG, "Partial WakeLock acquired for ${timeoutMs}ms")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to acquire WakeLock", e)
            }
        }
    }

    fun release() {
        synchronized(lock) {
            try {
                wakeLock?.let {
                    if (it.isHeld) {
                        it.release()
                        Log.d(TAG, "Partial WakeLock released")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to release WakeLock", e)
            }
        }
    }
}
