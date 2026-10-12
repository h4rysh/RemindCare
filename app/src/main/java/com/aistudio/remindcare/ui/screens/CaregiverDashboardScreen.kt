package com.aistudio.remindcare.ui.screens

import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.*
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.remindcare.data.AppSettings
import com.aistudio.remindcare.ui.RemindCareViewModel
import com.aistudio.remindcare.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaregiverDashboardScreen(
    viewModel: RemindCareViewModel,
    settings: AppSettings,
    onAddReminder: () -> Unit,
    onEditReminder: (Long) -> Unit,
    onNavigateToPairing: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val allReminders by viewModel.allReminders.collectAsState(initial = emptyList())
    val history by viewModel.history.collectAsState(initial = emptyList())
    val patientName = if (settings.patientName.isNotBlank()) settings.patientName else "Patient"
    
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Caregiver Dashboard", fontSize = 18.sp, color = TextGray) },
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = WarmOffWhite)
            )
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = onAddReminder,
                    containerColor = NavyPrimary,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Reminder")
                }
            }
        },
        containerColor = WarmOffWhite
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Text(
                text = "Tracking $patientName",
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                color = NavyPrimary,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 16.dp)
            )
            
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = WarmOffWhite,
                contentColor = NavyPrimary
            ) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                    Text("Reminders", modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Bold)
                }
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                    Text("History", modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Bold)
                }
            }

            var reminderToDelete by remember { mutableStateOf<com.aistudio.remindcare.data.Reminder?>(null) }

            if (reminderToDelete != null) {
                AlertDialog(
                    onDismissRequest = { reminderToDelete = null },
                    title = { Text("Delete Reminder?") },
                    text = { Text("Are you sure you want to delete '${reminderToDelete?.title}'? This cannot be undone.") },
                    confirmButton = {
                        TextButton(onClick = {
                            reminderToDelete?.let { viewModel.deleteReminder(it) }
                            reminderToDelete = null
                        }) {
                            Text("Delete", color = CoralUrgent)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { reminderToDelete = null }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            if (selectedTab == 0) {
                if (allReminders.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().animateContentSize().padding(32.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Add, contentDescription = "No reminders", modifier = Modifier.size(64.dp), tint = TextGray.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("No reminders scheduled.", color = TextGray, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                            Text("Tap the '+' button to add one.", color = TextGray.copy(alpha = 0.7f), fontSize = 14.sp)
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(allReminders) { reminder ->
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = CardBackground),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                                modifier = Modifier.fillMaxWidth().animateContentSize().clickable { onEditReminder(reminder.id) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        com.aistudio.remindcare.util.getIconForType(reminder.type),
                                        contentDescription = reminder.title,
                                        tint = if (reminder.isActive) NavyPrimary else TextGray,
                                        modifier = Modifier.size(32.dp).padding(end = 8.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(reminder.time, fontWeight = FontWeight.Bold, color = if (reminder.isActive) NavyPrimary else TextGray)
                                        Text(reminder.title, fontWeight = FontWeight.Black, fontSize = 18.sp, color = if (reminder.isActive) TextDark else TextGray)
                                        if (!reminder.isActive) {
                                            Text("Disabled", fontSize = 12.sp, color = CoralUrgent)
                                        }
                                    }
                                    Switch(
                                        checked = reminder.isActive,
                                        onCheckedChange = { isActive ->
                                            viewModel.updateReminder(reminder.copy(isActive = isActive))
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    IconButton(
                                        onClick = { reminderToDelete = reminder },
                                        modifier = Modifier
                                            .size(48.dp)
                                            .background(CoralUrgent.copy(alpha = 0.1f), CircleShape)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete ${reminder.title}",
                                            tint = CoralUrgent
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                if (history.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().animateContentSize().padding(32.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Notifications, contentDescription = "No history", modifier = Modifier.size(64.dp), tint = TextGray.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("No history available.", color = TextGray, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                            Text("Completed and missed tasks will appear here.", color = TextGray.copy(alpha = 0.7f), fontSize = 14.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                    }
                } else {
                    val completedCount = history.count { it.status == "COMPLETED" }
                    val missedCount = history.count { it.status == "MISSED" }
                    val totalLogged = history.size
                    val complianceRate = if (totalLogged > 0) (completedCount * 100) / totalLogged else 100
                    val complianceColor = when {
                        complianceRate >= 80 -> SageGreen
                        complianceRate >= 60 -> Color(0xFFF59E0B)
                        else -> CoralUrgent
                    }

                    LazyColumn(
                        contentPadding = PaddingValues(24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = CardBackground),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                                modifier = Modifier.fillMaxWidth().animateContentSize()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Adherence Compliance",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = NavyPrimary
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = complianceColor.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = "$complianceRate% Adherence",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Black,
                                                color = complianceColor,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceAround
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("$completedCount", fontSize = 20.sp, fontWeight = FontWeight.Black, color = SageGreen)
                                            Text("Taken", fontSize = 12.sp, color = TextGray)
                                        }
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("$missedCount", fontSize = 20.sp, fontWeight = FontWeight.Black, color = CoralUrgent)
                                            Text("Missed", fontSize = 12.sp, color = TextGray)
                                        }
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("$totalLogged", fontSize = 20.sp, fontWeight = FontWeight.Black, color = NavyPrimary)
                                            Text("Total Logged", fontSize = 12.sp, color = TextGray)
                                        }
                                    }
                                }
                            }
                        }
                        items(history) { entry ->
                            val statusColor = when (entry.status) {
                                "COMPLETED" -> SageGreen
                                "MISSED" -> CoralUrgent
                                "SNOOZED" -> Color(0xFFF59E0B)
                                else -> TextGray
                            }
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = CardBackground),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                                modifier = Modifier.fillMaxWidth().animateContentSize()
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        com.aistudio.remindcare.util.getIconForType(entry.type),
                                        contentDescription = entry.title,
                                        tint = statusColor,
                                        modifier = Modifier.size(24.dp).padding(end = 8.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(modifier = Modifier.fillMaxWidth().animateContentSize(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(entry.completedTime ?: entry.scheduledTime, fontWeight = FontWeight.Bold, color = TextGray, fontSize = 12.sp)
                                            Text(entry.status, fontWeight = FontWeight.Black, color = statusColor, fontSize = 12.sp)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(entry.title, fontWeight = FontWeight.Black, fontSize = 18.sp, color = TextDark)
                                        if (entry.photoPath != null) {
                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                                                coil.compose.AsyncImage(
                                                    model = java.io.File(entry.photoPath),
                                                    contentDescription = "Photo Proof",
                                                    modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp)),
                                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Photo Verified", fontSize = 14.sp, color = NavyPrimary, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        if (entry.notes != null) {
                                            Text(entry.notes!!, fontSize = 14.sp, color = TextGray, modifier = Modifier.padding(top = 4.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
