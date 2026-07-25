package com.example.alarm

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppDatabase
import com.example.data.RemindCareRepository
import com.example.receiver.ReminderReceiver
import com.example.ui.RemindCareViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderAlarmActivity : ComponentActivity() {

    private val db by lazy { AppDatabase.getDatabase(applicationContext) }
    private val repository by lazy { RemindCareRepository(db, applicationContext) }
    
    private val viewModel: RemindCareViewModel by viewModels {
        RemindCareViewModel.Factory(application, repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Show over lockscreen and wake up screen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
        
        enableEdgeToEdge()

        val reminderId = intent.getLongExtra(ReminderReceiver.EXTRA_REMINDER_ID, -1L)
        
        setContent {
            MyApplicationTheme {
                val allReminders by viewModel.allReminders.collectAsState(initial = emptyList())
                val reminder = allReminders.find { it.id == reminderId }

                if (reminder != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(WarmOffWhite)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("TIME FOR YOUR", color = TextGray, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text(reminder.type.uppercase(), color = CoralUrgent, fontSize = 36.sp, fontWeight = FontWeight.Black)
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        Text(reminder.title, fontSize = 48.sp, fontWeight = FontWeight.Black, color = TextDark, textAlign = TextAlign.Center)
                        
                        if (!reminder.medicineQuantity.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Take: ${reminder.medicineQuantity}", fontSize = 28.sp, color = NavyPrimary, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(64.dp))
                        
                        Button(
                            onClick = {
                                viewModel.completeReminder(reminder)
                                finish()
                            },
                            modifier = Modifier.fillMaxWidth().height(96.dp),
                            shape = RoundedCornerShape(32.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SageGreen)
                        ) {
                            Text("COMPLETED", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color.White)
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Button(
                            onClick = {
                                viewModel.snoozeReminder(reminder)
                                finish()
                            },
                            modifier = Modifier.fillMaxWidth().height(72.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CardBorder)
                        ) {
                            Text("SNOOZE FOR 5 MIN", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextDark)
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
                        Text("Loading Reminder...", color = Color.White)
                    }
                }
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        com.example.util.GlobalAlarmState.clearAlert(applicationContext)
    }
}
