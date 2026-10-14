package com.aistudio.remindcare.alarm

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.remindcare.data.AppDatabase
import com.aistudio.remindcare.data.RemindCareRepository
import com.aistudio.remindcare.receiver.ReminderReceiver
import com.aistudio.remindcare.ui.RemindCareViewModel
import com.aistudio.remindcare.ui.theme.*
import com.aistudio.remindcare.util.GlobalAlarmState

class ReminderAlarmActivity : ComponentActivity() {

    private val repository by lazy {
        RemindCareRepository(AppDatabase.getDatabase(applicationContext), applicationContext)
    }

    private val viewModel: RemindCareViewModel by viewModels {
        RemindCareViewModel.Factory(application, repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            keyguardManager?.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
        
        enableEdgeToEdge()

        val reminderId = intent.getLongExtra(ReminderReceiver.EXTRA_REMINDER_ID, -1L)
        
        setContent {
            val settings by viewModel.settings.collectAsState()
            RemindCareTheme(fontSizeStr = settings.fontSize) {
                val allReminders by viewModel.allReminders.collectAsState(initial = emptyList())
                val activeGlobal by GlobalAlarmState.activeAlert.collectAsState()
                val reminder = remember(allReminders, activeGlobal, reminderId) {
                    allReminders.find { it.id == reminderId } ?: activeGlobal?.takeIf { it.id == reminderId }
                }

                if (reminder != null) {
                    var showBypassDialog by remember { mutableStateOf(false) }
                    var showCameraError by remember { mutableStateOf(false) }

                    val cameraLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                        androidx.activity.result.contract.ActivityResultContracts.TakePicturePreview()
                    ) { bitmap ->
                        if (bitmap != null) {
                            showCameraError = false
                            ReminderAlarmService.stop(applicationContext)
                            viewModel.completeReminder(reminder, bitmap)
                            finish()
                        } else {
                            showCameraError = true
                        }
                    }

                    if (showBypassDialog) {
                        AlertDialog(
                            onDismissRequest = { showBypassDialog = false },
                            title = { Text("Complete Without Photo?", fontWeight = FontWeight.Bold) },
                            text = { Text("Are you sure you took your medication? This will mark it done without photo proof.") },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        showBypassDialog = false
                                        ReminderAlarmService.stop(applicationContext)
                                        viewModel.completeReminder(reminder, null)
                                        finish()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = SageGreen)
                                ) {
                                    Text("Yes, Mark Completed", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showBypassDialog = false }) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                            .padding(24.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "TIME FOR YOUR",
                            color = TextGray,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = reminder.type.uppercase(),
                            color = CoralUrgent,
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Black
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Text(
                            text = reminder.title,
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Black,
                            color = TextDark,
                            textAlign = TextAlign.Center
                        )
                        
                        if (!reminder.medicineQuantity.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Take: ${reminder.medicineQuantity}",
                                style = MaterialTheme.typography.headlineMedium,
                                color = NavyPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (!reminder.medicineLocation.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Location: ${reminder.medicineLocation}",
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextGray
                            )
                        }

                        Spacer(modifier = Modifier.height(48.dp))
                        
                        if (showCameraError) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(bottom = 16.dp)
                            ) {
                                Text(
                                    text = "Photo not captured — try again or complete without photo",
                                    color = CoralUrgent,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                TextButton(
                                    onClick = { showBypassDialog = true },
                                    colors = ButtonDefaults.textButtonColors(contentColor = NavyPrimary)
                                ) {
                                    Text("Unable to take photo? Tap here", fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Complete Action (Giant Accessible Button >= 80dp)
                        Button(
                            onClick = {
                                if (reminder.photoRequired) {
                                    cameraLauncher.launch(null)
                                } else {
                                    ReminderAlarmService.stop(applicationContext)
                                    viewModel.completeReminder(reminder)
                                    finish()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(88.dp)
                                .semantics { contentDescription = "Mark ${reminder.title} as completed" },
                            shape = RoundedCornerShape(28.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SageGreen)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = if (reminder.photoRequired) "TAKE PHOTO & COMPLETE" else "COMPLETED",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        // Snooze Action (Distinct Color & Shape)
                        Button(
                            onClick = {
                                ReminderAlarmService.stop(applicationContext)
                                viewModel.snoozeReminder(reminder)
                                finish()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(72.dp)
                                .semantics { contentDescription = "Snooze reminder for 5 minutes" },
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CardBorder)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Notifications, contentDescription = null, tint = TextDark, modifier = Modifier.size(28.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "SNOOZE FOR 5 MIN",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark
                                )
                            }
                        }

                        if (reminder.photoRequired && !showCameraError) {
                            Spacer(modifier = Modifier.height(16.dp))
                            TextButton(
                                onClick = { showBypassDialog = true },
                                modifier = Modifier.height(48.dp)
                            ) {
                                Text("Can't take a photo? Mark done anyway", color = TextGray, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                } else {
                    // Safe timeout state if reminder is missing
                    var timedOut by remember { mutableStateOf(false) }

                    LaunchedEffect(Unit) {
                        kotlinx.coroutines.delay(5000L)
                        timedOut = true
                    }

                    if (timedOut) {
                        LaunchedEffect(timedOut) {
                            ReminderAlarmService.stop(applicationContext)
                            kotlinx.coroutines.delay(2000L)
                            finish()
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            if (timedOut) {
                                Text(
                                    text = "This reminder is no longer active.",
                                    color = TextGray,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            } else {
                                CircularProgressIndicator(color = NavyPrimary)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Loading Reminder...",
                                    color = TextGray,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = {
                                    ReminderAlarmService.stop(applicationContext)
                                    finish()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CardBorder),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.height(56.dp)
                            ) {
                                Text("Dismiss", color = TextDark, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }
            }
        }
    }
}
