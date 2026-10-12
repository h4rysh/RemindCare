package com.aistudio.remindcare

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.aistudio.remindcare.data.AppDatabase
import com.aistudio.remindcare.data.RemindCareRepository
import com.aistudio.remindcare.receiver.ReminderReceiver
import com.aistudio.remindcare.ui.RemindCareViewModel
import com.aistudio.remindcare.ui.navigation.RemindCareNavGraph
import com.aistudio.remindcare.ui.theme.RemindCareTheme
import com.aistudio.remindcare.util.GlobalAlarmState
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val db by lazy { AppDatabase.getDatabase(applicationContext) }
    private val repository by lazy { RemindCareRepository(db, applicationContext) }
    
    private val viewModel: RemindCareViewModel by viewModels {
        RemindCareViewModel.Factory(application, repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ReminderReceiver.createNotificationChannel(applicationContext)
        
        // Sign in anonymously to Firebase
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            auth.signInAnonymously().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("MainActivity", "signInAnonymously:success")
                } else {
                    Log.w("MainActivity", "signInAnonymously:failure", task.exception)
                }
            }
        }
        
        handleIntent(intent)

        setContent {
            val settings by viewModel.settings.collectAsState()
            val isDark = when(settings.themeMode) {
                "Light" -> false
                "Dark" -> true
                else -> isSystemInDarkTheme()
            }
            
            RemindCareTheme(darkTheme = isDark, fontSizeStr = settings.fontSize) {
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val permissionLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestPermission(),
                    ) {}
                    LaunchedEffect(Unit) {
                        permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    RemindCareNavGraph(
                        viewModel = viewModel,
                        settings = settings,
                        modifier = Modifier.padding(innerPadding),
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val reminderId = intent?.getLongExtra(ReminderReceiver.EXTRA_REMINDER_ID, -1L) ?: -1L
        if (reminderId != -1L) {
            Log.d("MainActivity", "Launched from alarm notification, trigger alert for reminder: $reminderId")
            lifecycleScope.launch(Dispatchers.IO) {
                val reminder = db.reminderDao().getReminderById(reminderId)
                if (reminder != null) {
                    // Only re-trigger if this isn't already the active alarm
                    // (avoids restarting sound/TTS from the beginning)
                    if (GlobalAlarmState.activeAlert.value?.id != reminder.id) {
                        GlobalAlarmState.triggerAlert(applicationContext, reminder)
                    }
                    // Launch the Alarm Activity
                    val alarmIntent = Intent(applicationContext, com.aistudio.remindcare.alarm.ReminderAlarmActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        putExtra(ReminderReceiver.EXTRA_REMINDER_ID, reminder.id)
                    }
                    startActivity(alarmIntent)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
