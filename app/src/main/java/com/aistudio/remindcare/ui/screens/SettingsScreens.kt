package com.aistudio.remindcare.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.remindcare.data.AppSettings
import com.aistudio.remindcare.ui.RemindCareViewModel
import com.aistudio.remindcare.ui.components.CaregiverPinPadDialog
import com.aistudio.remindcare.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientSettingsScreen(
    viewModel: RemindCareViewModel,
    settings: AppSettings,
    onBack: () -> Unit,
    onSwitchRole: () -> Unit
) {
    var showPinDialog by remember { mutableStateOf(false) }
    var pinDialogAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var pinError by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(48.dp)
                            .semantics { contentDescription = "Go back" }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
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
                                if (settings.caregiverPin.isNotBlank()) {
                                    pinDialogAction = { viewModel.disconnectPairing() }
                                    showPinDialog = true
                                } else {
                                    viewModel.disconnectPairing()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CardBorder, contentColor = CoralUrgent),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Text("Spoken Instructions", fontSize = 18.sp, modifier = Modifier.weight(1f))
                        Switch(
                            checked = settings.voiceInstructionsEnabled,
                            onCheckedChange = { viewModel.toggleVoiceInstructions(it) }
                        )
                    }
                    HorizontalDivider(color = CardBorder)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    ) {
                        Text("Text Size", fontSize = 18.sp, modifier = Modifier.padding(bottom = 8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Normal", "Large", "Very Large").forEach { size ->
                                FilterChip(
                                    selected = settings.fontSize == size,
                                    onClick = { viewModel.updateFontSize(size) },
                                    label = { Text(size) },
                                    modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    if (settings.caregiverPin.isNotBlank()) {
                        pinDialogAction = onSwitchRole
                        showPinDialog = true
                    } else {
                        onSwitchRole()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = TextGray),
                border = androidx.compose.foundation.BorderStroke(2.dp, CardBorder)
            ) {
                Text("Switch to Caregiver Mode", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showPinDialog) {
        CaregiverPinPadDialog(
            title = "Caregiver Verification",
            subtitle = "Enter 4-digit caregiver PIN to authorize this change.",
            isError = pinError,
            onPinSubmit = { enteredPin ->
                viewModel.verifyCaregiverPin(enteredPin) { isValid ->
                    if (isValid) {
                        showPinDialog = false
                        pinError = false
                        pinDialogAction?.invoke()
                    } else {
                        pinError = true
                    }
                }
            },
            onDismiss = {
                showPinDialog = false
                pinError = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaregiverSettingsScreen(
    viewModel: RemindCareViewModel,
    settings: AppSettings,
    onBack: () -> Unit,
    onSwitchRole: () -> Unit
) {
    var showPinDialog by remember { mutableStateOf(false) }
    var pinDialogAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var pinError by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Caregiver Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(48.dp)
                            .semantics { contentDescription = "Go back" }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
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
                            onClick = {
                                if (settings.caregiverPin.isNotBlank()) {
                                    pinDialogAction = { viewModel.disconnectPairing() }
                                    showPinDialog = true
                                } else {
                                    viewModel.disconnectPairing()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CardBorder, contentColor = CoralUrgent),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
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
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    ) {
                        Text("Theme", fontSize = 18.sp, modifier = Modifier.padding(bottom = 8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("System", "Light", "Dark").forEach { mode ->
                                FilterChip(
                                    selected = settings.themeMode == mode,
                                    onClick = { viewModel.updateThemeMode(mode) },
                                    label = { Text(mode) },
                                    modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                                )
                            }
                        }
                    }
                    HorizontalDivider(color = CardBorder)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    ) {
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
                            TextButton(
                                onClick = { isEditingPin = false; newPin = "" },
                                modifier = Modifier.height(48.dp)
                            ) {
                                Text("Cancel")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    viewModel.updateCaregiverProfile(settings.caregiverName, newPin)
                                    isEditingPin = false
                                    newPin = ""
                                },
                                enabled = newPin.length == 4,
                                modifier = Modifier.height(48.dp)
                            ) {
                                Text("Save")
                            }
                        }
                    } else {
                        Button(
                            onClick = { isEditingPin = true },
                            colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            Text(if (settings.caregiverPin.isEmpty()) "Set PIN" else "Change PIN")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    if (settings.caregiverPin.isNotBlank()) {
                        pinDialogAction = onSwitchRole
                        showPinDialog = true
                    } else {
                        onSwitchRole()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = TextGray),
                border = androidx.compose.foundation.BorderStroke(2.dp, CardBorder)
            ) {
                Text("Start Over / Switch Role", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showPinDialog) {
        CaregiverPinPadDialog(
            title = "Caregiver Verification",
            subtitle = "Enter 4-digit caregiver PIN to confirm this action.",
            isError = pinError,
            onPinSubmit = { enteredPin ->
                viewModel.verifyCaregiverPin(enteredPin) { isValid ->
                    if (isValid) {
                        showPinDialog = false
                        pinError = false
                        pinDialogAction?.invoke()
                    } else {
                        pinError = true
                    }
                }
            },
            onDismiss = {
                showPinDialog = false
                pinError = false
            }
        )
    }
}
