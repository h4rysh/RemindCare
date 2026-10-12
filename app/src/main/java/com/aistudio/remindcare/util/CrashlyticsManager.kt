package com.aistudio.remindcare.util

import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * High-reliability Crashlytics and telemetry manager.
 * Safely wraps Firebase Crashlytics to guarantee zero crashes during unit testing
 * or when Firebase is not initialized, while preserving strict PHI privacy standards.
 */
object CrashlyticsManager {
    private const val TAG = "CrashlyticsManager"

    private val crashlytics: FirebaseCrashlytics?
        get() = try {
            FirebaseCrashlytics.getInstance()
        } catch (e: Throwable) {
            null
        }

    /**
     * Sets privacy-safe, anonymized operational session keys.
     * Note: Never log patient names, medication instructions, or health notes (PHI).
     */
    fun setUserState(role: String?, isPaired: Boolean, activeAlarmsCount: Int, deviceId: String?) {
        try {
            crashlytics?.apply {
                setCustomKey("user_role", role ?: "Unknown")
                setCustomKey("is_paired", isPaired)
                setCustomKey("active_alarms_count", activeAlarmsCount)
                if (!deviceId.isNullOrBlank()) {
                    setUserId(deviceId)
                }
            }
            Log.d(TAG, "UserState updated: role=$role, isPaired=$isPaired, alarms=$activeAlarmsCount")
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to set Crashlytics user state", e)
        }
    }

    /**
     * Logs structured operational breadcrumbs to Crashlytics.
     */
    fun log(tag: String, message: String) {
        try {
            crashlytics?.log("[$tag] $message")
            Log.d(tag, message)
        } catch (e: Throwable) {
            Log.d(tag, message)
        }
    }

    /**
     * Records non-fatal exceptions (e.g., sync timeout, exact alarm permission denied).
     */
    fun recordNonFatal(throwable: Throwable, context: String? = null) {
        try {
            if (!context.isNullOrBlank()) {
                crashlytics?.log("ErrorContext: $context")
            }
            crashlytics?.recordException(throwable)
            Log.e(TAG, "Non-fatal recorded: $context", throwable)
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to record non-fatal: $context", throwable)
        }
    }
}
