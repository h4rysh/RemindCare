package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppSettings
import com.example.ui.RemindCareViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaregiverDashboardScreen(
    viewModel: RemindCareViewModel,
    settings: AppSettings,
    onAddReminder: () -> Unit,
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
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = TextGray)
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

            if (selectedTab == 0) {
                if (allReminders.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No reminders scheduled.", color = TextGray)
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
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
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
                                    IconButton(onClick = { viewModel.deleteReminder(reminder) }) {
                                        Text("Del", color = CoralUrgent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                if (history.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No history available.", color = TextGray)
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
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
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(entry.completedTime ?: entry.scheduledTime, fontWeight = FontWeight.Bold, color = TextGray, fontSize = 12.sp)
                                        Text(entry.status, fontWeight = FontWeight.Black, color = statusColor, fontSize = 12.sp)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(entry.title, fontWeight = FontWeight.Black, fontSize = 18.sp, color = TextDark)
                                    if (entry.notes != null) {
                                        Text(entry.notes!!, fontSize = 14.sp, color = TextGray)
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
