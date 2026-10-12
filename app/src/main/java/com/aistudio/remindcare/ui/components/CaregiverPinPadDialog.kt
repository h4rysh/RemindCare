package com.aistudio.remindcare.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.aistudio.remindcare.ui.theme.*
import kotlinx.coroutines.launch

/**
 * Accessible 4-digit PIN pad dialog adhering to Apple HIG craft and senior ergonomics.
 * Features >= 64dp touch targets, tactile haptics, high-contrast indicators, and visual shake on error.
 */
@Composable
fun CaregiverPinPadDialog(
    title: String = "Caregiver Authorization",
    subtitle: String = "Enter your 4-digit security PIN to continue.",
    isError: Boolean = false,
    errorMessage: String = "Incorrect PIN. Please try again.",
    onPinSubmit: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val shakeOffset = remember { Animatable(0f) }

    // Trigger shake animation and haptic feedback when error occurs
    LaunchedEffect(isError) {
        if (isError) {
            RemindCareHaptics.performError(context, haptic)
            pin = ""
            coroutineScope.launch {
                shakeOffset.animateTo(
                    targetValue = 0f,
                    animationSpec = keyframes {
                        durationMillis = 400
                        0f at 0
                        (-20f) at 50
                        20f at 100
                        (-15f) at 150
                        15f at 200
                        (-8f) at 250
                        8f at 300
                        (-3f) at 350
                        0f at 400
                    }
                )
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Lock Icon
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(NavyPrimary.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = "Security Gate",
                        tint = NavyPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextDark,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextGray,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 4-Digit PIN Indicators with shake offset
                Row(
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.offset(x = shakeOffset.value.dp)
                ) {
                    for (i in 0 until 4) {
                        val isFilled = i < pin.length
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isError -> CoralUrgent
                                        isFilled -> NavyPrimary
                                        else -> Color.Transparent
                                    }
                                )
                                .border(
                                    width = 2.dp,
                                    color = when {
                                        isError -> CoralUrgent
                                        isFilled -> NavyPrimary
                                        else -> CardBorder
                                    },
                                    shape = CircleShape
                                )
                        )
                    }
                }

                // Error text or spacing
                Box(
                    modifier = Modifier
                        .height(32.dp)
                        .padding(top = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isError) {
                        Text(
                            text = errorMessage,
                            color = CoralUrgent,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Numeric Keypad Grid (>= 64dp touch targets)
                val keys = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("C", "0", "DEL")
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    for (row in keys) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            for (key in row) {
                                Box(
                                    modifier = Modifier.weight(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    when (key) {
                                        "DEL" -> {
                                            IconButton(
                                                onClick = {
                                                    RemindCareHaptics.performKeyClick(haptic, context)
                                                    if (pin.isNotEmpty()) {
                                                        pin = pin.dropLast(1)
                                                    }
                                                },
                                                modifier = Modifier
                                                    .size(68.dp)
                                                    .clip(CircleShape)
                                                    .background(CardBackground)
                                                    .semantics { contentDescription = "Delete digit" }
                                            ) {
                                                Icon(
                                                    Icons.AutoMirrored.Filled.Backspace,
                                                    contentDescription = null,
                                                    tint = TextDark,
                                                    modifier = Modifier.size(26.dp)
                                                )
                                            }
                                        }
                                        "C" -> {
                                            TextButton(
                                                onClick = {
                                                    RemindCareHaptics.performTap(haptic, context)
                                                    pin = ""
                                                },
                                                modifier = Modifier
                                                    .size(68.dp)
                                                    .clip(CircleShape)
                                                    .semantics { contentDescription = "Clear PIN" }
                                            ) {
                                                Text(
                                                    text = "C",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextGray
                                                )
                                            }
                                        }
                                        else -> {
                                            // Number Button
                                            Surface(
                                                onClick = {
                                                    RemindCareHaptics.performKeyClick(haptic, context)
                                                    if (pin.length < 4) {
                                                        val newPin = pin + key
                                                        pin = newPin
                                                        if (newPin.length == 4) {
                                                            onPinSubmit(newPin)
                                                        }
                                                    }
                                                },
                                                shape = CircleShape,
                                                color = CardBackground,
                                                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                                                modifier = Modifier
                                                    .size(68.dp)
                                                    .semantics { contentDescription = "Digit $key" }
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(
                                                        text = key,
                                                        style = MaterialTheme.typography.headlineSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = TextDark
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Cancel Button (>= 48dp height)
                TextButton(
                    onClick = {
                        RemindCareHaptics.performTap(haptic, context)
                        onDismiss()
                    },
                    modifier = Modifier
                        .height(48.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = "Cancel",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextGray,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
