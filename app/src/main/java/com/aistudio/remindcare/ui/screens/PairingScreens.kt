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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.remindcare.data.AppSettings
import com.aistudio.remindcare.data.SyncResult
import com.aistudio.remindcare.ui.RemindCareViewModel
import com.aistudio.remindcare.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientPairingScreen(viewModel: RemindCareViewModel, settings: AppSettings, onBack: () -> Unit, isSetupFlow: Boolean = false) {
    var pairingCode by remember { mutableStateOf<String?>(null) }
    var pairingRequest by remember { mutableStateOf<com.aistudio.remindcare.data.PairingRequest?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isGenerating by remember { mutableStateOf(true) }
    
    val generateCode = {
        isGenerating = true
        errorMessage = null
        viewModel.generatePairingCode { code, error ->
            isGenerating = false
            if (error != null) {
                errorMessage = error
            } else if (code != null) {
                pairingCode = code
                viewModel.listenForPairingRequest(code) { request ->
                    pairingRequest = request
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        generateCode()
    }

    if (pairingRequest != null && pairingRequest?.status == com.aistudio.remindcare.data.PairingStatus.REQUESTED) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Connection Request", fontWeight = FontWeight.Bold) },
            text = { Text("${pairingRequest?.caregiverName} wants to connect to your care plan.") },
            confirmButton = {
                Button(
                    onClick = {
                        pairingCode?.let { code ->
                            viewModel.approvePairingRequest(code, pairingRequest!!.caregiverId, pairingRequest!!.caregiverName) {
                                onBack()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SageGreen)
                ) {
                    Text("Allow Connection", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    pairingCode?.let { code ->
                        viewModel.declinePairingRequest(code) {
                            pairingRequest = null
                        }
                    }
                }) {
                    Text("Decline", color = CoralUrgent)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Connect Caregiver", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    if (!isSetupFlow) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .size(48.dp)
                                .semantics { contentDescription = "Go back" }
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                        }
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
                .padding(32.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Ask your caregiver to enter this code on their device to connect.",
                fontSize = 20.sp,
                textAlign = TextAlign.Center,
                color = TextDark,
                modifier = Modifier.padding(bottom = 48.dp)
            )

            if (isGenerating) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(vertical = 24.dp)
                ) {
                    CircularProgressIndicator(color = NavyPrimary, strokeWidth = 4.dp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Connecting to secure care network...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextGray
                    )
                }
            } else if (errorMessage != null) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CoralUrgent.copy(alpha = 0.12f)),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (errorMessage?.contains("PERMISSION_DENIED") == true) {
                                "Connecting to care cloud. Please check your internet connection and tap retry."
                            } else {
                                errorMessage ?: "Failed to generate pairing code."
                            },
                            color = CoralUrgent,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { generateCode() },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.height(56.dp).fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
                        ) {
                            Text("Retry Connection", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            } else if (pairingCode != null) {
                val talkBackDescription = "Pairing code: ${pairingCode!!.map { "$it " }.joinToString("")}"
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    border = androidx.compose.foundation.BorderStroke(2.dp, NavyPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .semantics {
                            contentDescription = talkBackDescription
                        }
                ) {
                    Text(
                        text = pairingCode!!,
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Black,
                        color = NavyPrimary,
                        textAlign = TextAlign.Center,
                        letterSpacing = 4.sp,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp).fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(64.dp))
            
            Text(
                text = "This code expires in 10 minutes.",
                color = TextGray,
                fontSize = 16.sp
            )
            
            if (isSetupFlow) {
                Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CardBorder, contentColor = TextDark)
                ) {
                    Text("Skip for Now", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaregiverPairingScreen(viewModel: RemindCareViewModel, settings: AppSettings, onBack: () -> Unit, isSetupFlow: Boolean = false) {
    var codeInput by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var pendingMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Connect to Patient", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    if (!isSetupFlow) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .size(48.dp)
                                .semantics { contentDescription = "Go back" }
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                        }
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
            Text(
                text = "Enter Patient Code",
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                color = TextDark
            )
            
            Text(
                text = "Ask the patient to open 'Connect Caregiver' on their device.",
                fontSize = 18.sp,
                color = TextGray
            )

            OutlinedTextField(
                value = codeInput,
                onValueChange = { if (it.length <= 6 && it.all { char -> char.isDigit() }) codeInput = it },
                label = { Text("6-Digit Code") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                textStyle = LocalTextStyle.current.copy(fontSize = 32.sp, letterSpacing = 8.sp, textAlign = TextAlign.Center),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NavyPrimary,
                    unfocusedBorderColor = CardBorder,
                    focusedContainerColor = CardBackground,
                    unfocusedContainerColor = CardBackground
                )
            )

            if (errorMessage != null) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CoralUrgent.copy(alpha = 0.12f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        errorMessage!!,
                        color = CoralUrgent,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            
            if (pendingMessage != null) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = AmberWarning.copy(alpha = 0.15f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(color = AmberWarning, modifier = Modifier.size(24.dp))
                        Text(
                            pendingMessage!!,
                            color = TextDark,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            if (successMessage != null) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SageGreen.copy(alpha = 0.15f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        successMessage!!,
                        color = SageGreen,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f, fill = false))
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    isLoading = true
                    errorMessage = null
                    pendingMessage = null
                    viewModel.requestPairing(codeInput) { result ->
                        when (result) {
                            is SyncResult.Success -> {
                                isLoading = false
                                pendingMessage = null
                                successMessage = "Connected to ${result.patientName}!"
                            }
                            is SyncResult.PendingApproval -> {
                                pendingMessage = "Waiting for patient approval..."
                            }
                            is SyncResult.Error -> {
                                isLoading = false
                                pendingMessage = null
                                errorMessage = result.message
                            }
                        }
                    }
                },
                enabled = codeInput.length == 6 && !isLoading && successMessage == null && pendingMessage == null,
                modifier = Modifier.fillMaxWidth().height(64.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
            ) {
                if (isLoading && pendingMessage == null) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else if (successMessage != null) {
                    Text("CONNECTED", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                } else {
                    Text("CONNECT", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
            
            if (successMessage != null || isSetupFlow) {
                Button(
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CardBorder, contentColor = TextDark)
                ) {
                    if (successMessage != null) {
                        Text("Continue", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Text("Skip for Now", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
