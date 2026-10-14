package com.aistudio.remindcare

import android.app.Application
import com.google.firebase.FirebaseApp

class TestApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
            }
        } catch (e: Exception) {
            android.util.Log.w("TestApp", "FirebaseApp init: ${e.message}")
        }
    }
}
