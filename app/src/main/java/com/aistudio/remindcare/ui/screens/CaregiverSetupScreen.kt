package com.aistudio.remindcare.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import com.aistudio.remindcare.ui.RemindCareViewModel
import com.aistudio.remindcare.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaregiverSetupScreen(viewModel: RemindCareViewModel, onComplete: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Caregiver Setup", fontWeight = FontWeight.Bold, color = NavyPrimary) },
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
                text = "Secure your account",
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                color = TextDark
            )
            
            Text(
                text = "A 4-digit PIN ensures only you can edit the schedule or change sensitive settings.",
                fontSize = 16.sp,
                color = TextGray
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Your Name") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NavyPrimary,
                    unfocusedBorderColor = CardBorder,
                    focusedContainerColor = CardBackground,
                    unfocusedContainerColor = CardBackground
                )
            )

            OutlinedTextField(
                value = pin,
                onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) pin = it },
                label = { Text("Create a 4-Digit PIN") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NavyPrimary,
                    unfocusedBorderColor = CardBorder,
                    focusedContainerColor = CardBackground,
                    unfocusedContainerColor = CardBackground
                )
            )

            Spacer(modifier = Modifier.weight(1f, fill = false))
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    viewModel.updateCaregiverProfile(name, pin)
                    onComplete()
                },
                enabled = name.isNotBlank() && pin.length == 4,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
            ) {
                Text("Complete Setup", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}
