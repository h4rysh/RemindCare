package com.example

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.example.data.AppDatabase
import com.example.data.RemindCareRepository
import com.example.receiver.ReminderReceiver
import com.example.ui.RemindCareViewModel
import com.example.ui.navigation.RemindCareNavGraph
import com.example.ui.theme.MyApplicationTheme
import com.example.util.GlobalAlarmState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val db by lazy { AppDatabase.getDatabase(applicationContext) }
    private val repository by lazy { RemindCareRepository(db, applicationContext) }
    
    private val viewModel: RemindCareViewModel by viewModels {
        RemindCareViewModel.Factory(application, repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ReminderReceiver.createNotificationChannel(applicationContext)
        handleIntent(intent)

        setContent {
            MyApplicationTheme {
                val settings by viewModel.settings.collectAsState()
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val permissionLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestPermission()
                    ) {}
                    LaunchedEffect(Unit) {
                        permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    RemindCareNavGraph(viewModel = viewModel, settings = settings)
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
                    GlobalAlarmState.triggerAlert(applicationContext, reminder)
                    // Then launch the Alarm Activity manually just in case
                    val alarmIntent = Intent(applicationContext, com.example.alarm.ReminderAlarmActivity::class.java).apply {
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
        GlobalAlarmState.clearAlert(applicationContext)
    }
}
