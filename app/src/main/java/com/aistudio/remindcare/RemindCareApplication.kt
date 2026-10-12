package com.aistudio.remindcare

import android.app.Application
import com.aistudio.remindcare.util.AlarmWatchdogWorker
import com.google.firebase.FirebaseApp

class RemindCareApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            FirebaseApp.initializeApp(this)
        } catch (e: Exception) {
            android.util.Log.w("RemindCareApp", "FirebaseApp initialization handled: ${e.message}")
        }
        
        // Schedule periodic alarm watchdog self-healing safety net
        AlarmWatchdogWorker.schedule(this)
        com.aistudio.remindcare.util.CrashlyticsManager.log("App", "RemindCareApplication initialized")
    }
}
