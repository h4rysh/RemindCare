package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppSettings
import com.example.data.SyncResult
import com.example.ui.RemindCareViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientPairingScreen(viewModel: RemindCareViewModel, settings: AppSettings, onBack: () -> Unit, isSetupFlow: Boolean = false) {
    var pairingCode by remember { mutableStateOf<String?>(null) }
    var pairingRequest by remember { mutableStateOf<com.example.data.PairingRequest?>(null) }
    
    LaunchedEffect(Unit) {
        viewModel.generatePairingCode { code ->
            pairingCode = code
            viewModel.listenForPairingRequest(code) { request ->
                pairingRequest = request
            }
        }
    }

    if (pairingRequest != null && pairingRequest?.status == "requested") {
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
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, "Back")
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
                .padding(32.dp),
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

            if (pairingCode == null) {
                CircularProgressIndicator(color = NavyPrimary)
            } else {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    border = androidx.compose.foundation.BorderStroke(2.dp, NavyPrimary),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = pairingCode!!.replace("RC-", ""),
                        fontSize = 72.sp,
                        fontWeight = FontWeight.Black,
                        color = NavyPrimary,
                        textAlign = TextAlign.Center,
                        letterSpacing = 8.sp,
                        modifier = Modifier.padding(32.dp).fillMaxWidth()
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
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, "Back")
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
                .padding(24.dp),
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
                onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) codeInput = it },
                label = { Text("4-Digit Code") },
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
                Text(errorMessage!!, color = CoralUrgent, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            
            if (pendingMessage != null) {
                Text(pendingMessage!!, color = Color(0xFFF59E0B), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            
            if (successMessage != null) {
                Text(successMessage!!, color = SageGreen, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    isLoading = true
                    errorMessage = null
                    pendingMessage = null
                    val fullCode = "RC-$codeInput"
                    viewModel.requestPairing(fullCode) { result ->
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
                enabled = codeInput.length == 4 && !isLoading && successMessage == null && pendingMessage == null,
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
