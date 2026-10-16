package com.aistudio.remindcare.ui.screens

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aistudio.remindcare.data.AppSettings
import com.aistudio.remindcare.data.Reminder
import com.aistudio.remindcare.ui.RemindCareViewModel
import com.aistudio.remindcare.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientHomeScreen(
    viewModel: RemindCareViewModel,
    settings: AppSettings,
    onNavigateToPairing: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val activeReminders by viewModel.activeReminders.collectAsState(initial = emptyList())
    val history by viewModel.history.collectAsState(initial = emptyList())
    val todayDateString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    val todayDate = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date())
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = todayDate, 
                        style = MaterialTheme.typography.titleMedium,
                        color = TextGray
                    ) 
                },
                actions = {
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier
                            .size(48.dp)
                            .semantics { contentDescription = "Open Settings" }
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null, tint = TextGray)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Text(
                    text = "Hello${if (settings.patientName.isNotBlank()) ", ${settings.patientName}" else ""}",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Black,
                    color = NavyPrimary,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (settings.isPaired) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = CircleShape,
                        color = SageGreenLight,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .clickable { onNavigateToSettings() }
                            .semantics { contentDescription = "Connected with ${settings.connectedCaregiverName}. Tap for settings." }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = SageGreen,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (settings.connectedCaregiverName.isNotBlank()) "with ${settings.connectedCaregiverName}" else "Connected",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = SageGreen
                            )
                        }
                    }
                }
            }

            if (!settings.isPaired) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = NavyPrimary.copy(alpha = 0.08f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NavyPrimary.copy(alpha = 0.2f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = NavyPrimary.copy(alpha = 0.12f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Link,
                                    contentDescription = null,
                                    tint = NavyPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Connect with Caregiver",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = NavyPrimary
                            )
                            Text(
                                text = "Share your code so your caregiver can manage reminders & track health",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextDark.copy(alpha = 0.75f)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = onNavigateToPairing,
                            colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Text(
                                text = "Pair",
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
            
            Text(
                text = "Today's plan",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = TextDark,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            val context = LocalContext.current
            val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

            // Check DND access
            val notificationManager = remember {
                context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            }
            // Check exact alarm permission
            val alarmManager = remember {
                context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            }

            var hasDndAccess by remember {
                mutableStateOf(notificationManager?.isNotificationPolicyAccessGranted ?: false)
            }
            var canScheduleExact by remember {
                mutableStateOf(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        alarmManager?.canScheduleExactAlarms() ?: false
                    } else {
                        true
                    }
                )
            }

            DisposableEffect(lifecycleOwner) {
                val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                    if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                        hasDndAccess = notificationManager?.isNotificationPolicyAccessGranted ?: false
                        canScheduleExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            alarmManager?.canScheduleExactAlarms() ?: false
                        } else {
                            true
                        }
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }
            
            if (!hasDndAccess) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = "Warning",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Do Not Disturb access needed",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                "Alarms may be silenced when DND is on.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                            )
                        }
                        TextButton(
                            onClick = {
                                context.startActivity(
                                    Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                )
                            },
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text("Fix", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            
            if (!canScheduleExact) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = "Warning",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Exact alarms disabled",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                "Reminders may be delayed by minutes.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                            )
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            TextButton(
                                onClick = {
                                    context.startActivity(
                                        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        }
                                    )
                                },
                                modifier = Modifier.height(48.dp)
                            ) {
                                Text("Fix", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            if (activeReminders.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Check, contentDescription = "All done", modifier = Modifier.size(64.dp), tint = SageGreen)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No tasks for today.", color = TextGray, style = MaterialTheme.typography.titleMedium)
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(activeReminders) { reminder ->
                        val isCompletedToday = history.any { 
                            it.reminderId == reminder.id && 
                            it.completedTime?.startsWith(todayDateString) == true && 
                            it.status == "COMPLETED" 
                        }
                        PatientTaskCard(reminder, isCompletedToday)
                    }
                }
            }
        }
    }
}

@Composable
fun PatientTaskCard(reminder: Reminder, isCompleted: Boolean = false) {
    val accessibleDescription = buildString {
        append("${reminder.time}, ")
        append("${reminder.title}, ")
        if (!reminder.medicineQuantity.isNullOrBlank()) {
            append("Quantity: ${reminder.medicineQuantity}, ")
        }
        append(if (isCompleted) "Completed for today" else "Not yet completed")
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = if (isCompleted) CardBackground.copy(alpha = 0.5f) else CardBackground),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .semantics(mergeDescendants = true) {
                contentDescription = accessibleDescription
            }
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(com.aistudio.remindcare.util.getColorForType(reminder.type).copy(alpha = if (isCompleted) 0.05f else 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isCompleted) Icons.Default.Check else com.aistudio.remindcare.util.getIconForType(reminder.type), 
                    contentDescription = null, 
                    tint = if (isCompleted) SageGreen else com.aistudio.remindcare.util.getColorForType(reminder.type), 
                    modifier = Modifier.size(32.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = reminder.time,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isCompleted) TextGray else NavyPrimary
                )
                Text(
                    text = reminder.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = if (isCompleted) TextGray else TextDark,
                    textDecoration = if (isCompleted) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                )
                if (!reminder.medicineQuantity.isNullOrBlank()) {
                    Text(
                        text = "Take: ${reminder.medicineQuantity}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextGray
                    )
                }
            }
        }
    }
}
