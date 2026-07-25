package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppSettings
import com.example.ui.RemindCareViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientSettingsScreen(viewModel: RemindCareViewModel, settings: AppSettings, onBack: () -> Unit, onSwitchRole: () -> Unit) {
    var showPinDialog by remember { mutableStateOf(false) }
    var pinDialogAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var pinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = WarmOffWhite)
            )
        },
        containerColor = WarmOffWhite
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text("Connection", fontSize = 22.sp, fontWeight = FontWeight.Black, color = NavyPrimary)
            
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Caregiver Status", fontSize = 14.sp, color = TextGray)
                    if (settings.isPaired) {
                        Text("Connected", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = SageGreen)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { 
                                pinDialogAction = { viewModel.disconnectPairing() }
                                showPinDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CardBorder, contentColor = CoralUrgent),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Disconnect", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Text("Not Connected", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextDark)
                    }
                }
            }
            
            Text("Preferences", fontSize = 22.sp, fontWeight = FontWeight.Black, color = NavyPrimary)
            
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        Text("Spoken Instructions", fontSize = 18.sp, modifier = Modifier.weight(1f))
                        Switch(checked = settings.voiceInstructionsEnabled, onCheckedChange = { viewModel.toggleVoiceInstructions(it) })
                    }
                    HorizontalDivider(color = CardBorder)
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                        Text("Text Size", fontSize = 18.sp, modifier = Modifier.padding(bottom = 8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Normal", "Large", "Very Large").forEach { size ->
                                FilterChip(
                                    selected = settings.fontSize == size,
                                    onClick = { viewModel.updateFontSize(size) },
                                    label = { Text(size) }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = { 
                    pinDialogAction = onSwitchRole
                    showPinDialog = true 
                },
                modifier = Modifier.fillMaxWidth().height(64.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = TextGray),
                border = androidx.compose.foundation.BorderStroke(2.dp, CardBorder)
            ) {
                Text("Switch to Caregiver Mode", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
    
    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { showPinDialog = false; pinError = false; pinInput = "" },
            title = { Text("Caregiver Access Required") },
            text = {
                Column {
                    Text("Enter the caregiver PIN to authorize this action.", color = TextGray, modifier = Modifier.padding(bottom = 16.dp))
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { pinInput = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        isError = pinError
                    )
                    if (pinError) {
                        Text("Incorrect PIN", color = CoralUrgent, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pinInput == settings.caregiverPin || settings.caregiverPin.isBlank()) {
                            showPinDialog = false
                            pinError = false
                            pinDialogAction?.invoke()
                        } else {
                            pinError = true
                        }
                    }
                ) {
                    Text("Unlock")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false; pinError = false; pinInput = "" }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaregiverSettingsScreen(viewModel: RemindCareViewModel, settings: AppSettings, onBack: () -> Unit, onSwitchRole: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Caregiver Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = WarmOffWhite)
            )
        },
        containerColor = WarmOffWhite
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text("Patient Connection", fontSize = 22.sp, fontWeight = FontWeight.Black, color = NavyPrimary)
            
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Status", fontSize = 14.sp, color = TextGray)
                    if (settings.isPaired) {
                        Text("Connected", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = SageGreen)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.disconnectPairing() },
                            colors = ButtonDefaults.buttonColors(containerColor = CardBorder, contentColor = CoralUrgent),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Disconnect", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Text("Not Connected", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextDark)
                    }
                }
            }
            
            Text("App Preferences", fontSize = 22.sp, fontWeight = FontWeight.Black, color = NavyPrimary)
            
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                        Text("Theme", fontSize = 18.sp, modifier = Modifier.padding(bottom = 8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("System", "Light", "Dark").forEach { mode ->
                                FilterChip(
                                    selected = settings.themeMode == mode,
                                    onClick = { viewModel.updateThemeMode(mode) },
                                    label = { Text(mode) }
                                )
                            }
                        }
                    }
                    Divider(color = CardBorder)
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                        Text("Alarm Volume", fontSize = 18.sp, modifier = Modifier.padding(bottom = 8.dp))
                        Slider(
                            value = settings.reminderVolume,
                            onValueChange = { viewModel.updateVolume(it) },
                            valueRange = 0f..1f,
                            steps = 4
                        )
                    }
                }
            }

            Text("Security", fontSize = 22.sp, fontWeight = FontWeight.Black, color = NavyPrimary)
            
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                var isEditingPin by remember { mutableStateOf(false) }
                var newPin by remember { mutableStateOf("") }
                
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Caregiver PIN", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextDark)
                    Text("Required to modify settings or switch roles.", fontSize = 14.sp, color = TextGray)
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (isEditingPin) {
                        OutlinedTextField(
                            value = newPin,
                            onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) newPin = it },
                            label = { Text("New 4-Digit PIN") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { isEditingPin = false; newPin = "" }) { Text("Cancel") }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(onClick = { 
                                viewModel.updateCaregiverProfile(settings.caregiverName, newPin)
                                isEditingPin = false
                                newPin = ""
                            }, enabled = newPin.length == 4) { Text("Save") }
                        }
                    } else {
                        Button(
                            onClick = { isEditingPin = true },
                            colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (settings.caregiverPin.isEmpty()) "Set PIN" else "Change PIN")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = onSwitchRole,
                modifier = Modifier.fillMaxWidth().height(64.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = TextGray),
                border = androidx.compose.foundation.BorderStroke(2.dp, CardBorder)
            ) {
                Text("Start Over / Switch Role", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// Added at the end to modify the file but wait, it's easier to use sed or just replace the whole file. I will sed edit it.
