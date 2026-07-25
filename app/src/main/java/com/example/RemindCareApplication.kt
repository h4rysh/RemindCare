package com.example

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

class RemindCareApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                val apiKey = BuildConfig.FIREBASE_API_KEY
                val appId = BuildConfig.FIREBASE_APP_ID
                val projectId = BuildConfig.FIREBASE_PROJECT_ID
                
                if (apiKey.isNotBlank() && apiKey != "MY_FIREBASE_API_KEY") {
                    val options = FirebaseOptions.Builder()
                        .setApiKey(apiKey)
                        .setApplicationId(appId)
                        .setProjectId(projectId)
                        .build()
                    FirebaseApp.initializeApp(this, options)
                    Log.d("RemindCareApp", "Firebase initialized programmatically.")
                } else {
                    Log.d("RemindCareApp", "Firebase skipped: Missing credentials.")
                }
            }
        } catch (e: Exception) {
            Log.e("RemindCareApp", "Firebase initialization failed", e)
        }
    }
}
