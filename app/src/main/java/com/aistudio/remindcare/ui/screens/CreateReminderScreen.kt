package com.aistudio.remindcare.ui.screens

import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.remindcare.data.Reminder
import com.aistudio.remindcare.ui.RemindCareViewModel
import com.aistudio.remindcare.ui.theme.*
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateReminderScreen(viewModel: RemindCareViewModel, reminderId: Long? = null, onBack: () -> Unit) {
    val reminders by viewModel.allReminders.collectAsState()
    val existingReminder = remember(reminders, reminderId) { reminders.find { it.id == reminderId } }

    var title by remember(existingReminder) { mutableStateOf(existingReminder?.title ?: "") }
    var type by remember(existingReminder) { mutableStateOf(existingReminder?.type ?: "Medicine") }
    var time by remember(existingReminder) { mutableStateOf(existingReminder?.time ?: "09:00") }
    var repeat by remember(existingReminder) { mutableStateOf(existingReminder?.repeat ?: "Daily") }
    
    var medicineName by remember(existingReminder) { mutableStateOf(existingReminder?.medicineName ?: "") }
    var quantity by remember(existingReminder) { mutableStateOf(existingReminder?.medicineQuantity ?: "") }
    var location by remember(existingReminder) { mutableStateOf(existingReminder?.medicineLocation ?: "") }
    var notes by remember(existingReminder) { mutableStateOf(existingReminder?.additionalNotes ?: "") }
    var photoRequired by remember(existingReminder) { mutableStateOf(existingReminder?.photoRequired ?: false) }

    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    val timePickerDialog = TimePickerDialog(
        context,
        { _, hourOfDay, minute ->
            time = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute)
        },
        calendar.get(Calendar.HOUR_OF_DAY),
        calendar.get(Calendar.MINUTE),
        true
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (existingReminder != null) "Edit Reminder" else "New Reminder", fontWeight = FontWeight.Bold) },
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("What should they remember?") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            )
            
            // Type dropdown
            var typeExpanded by remember { mutableStateOf(false) }
            val types = listOf("Medicine", "Meal", "Water", "Exercise", "Appointment", "Sleep", "General", "Custom")
            ExposedDropdownMenuBox(expanded = typeExpanded, onExpandedChange = { typeExpanded = it }) {
                OutlinedTextField(
                    value = type,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Type") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(type = androidx.compose.material3.MenuAnchorType.PrimaryNotEditable),
                    shape = RoundedCornerShape(16.dp)
                )
                ExposedDropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                    types.forEach { t ->
                        DropdownMenuItem(text = { Text(t) }, onClick = { type = t; typeExpanded = false })
                    }
                }
            }

            // Repeat dropdown
            var repeatExpanded by remember { mutableStateOf(false) }
            val repeatOptions = listOf("Once", "Daily", "Weekly")
            ExposedDropdownMenuBox(expanded = repeatExpanded, onExpandedChange = { repeatExpanded = it }) {
                OutlinedTextField(
                    value = repeat,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Repeat") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = repeatExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(type = androidx.compose.material3.MenuAnchorType.PrimaryNotEditable),
                    shape = RoundedCornerShape(16.dp)
                )
                ExposedDropdownMenu(expanded = repeatExpanded, onDismissRequest = { repeatExpanded = false }) {
                    repeatOptions.forEach { r ->
                        DropdownMenuItem(text = { Text(r) }, onClick = { repeat = r; repeatExpanded = false })
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("Time:", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Button(onClick = { timePickerDialog.show() }, colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)) {
                    Text(time, fontSize = 18.sp, color = Color.White)
                }
            }
            
            if (type == "Medicine") {
                OutlinedTextField(
                    value = medicineName,
                    onValueChange = { medicineName = it },
                    label = { Text("Medicine Name (e.g. Aspirin)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text("Quantity (e.g. 2 pills)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Location (e.g. Kitchen counter)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )
            }
            
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Additional Notes (optional)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                maxLines = 3
            )
            
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("Require photo confirmation?", modifier = Modifier.weight(1f))
                Switch(checked = photoRequired, onCheckedChange = { photoRequired = it })
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = {
                    val reminder = Reminder(
                        id = existingReminder?.id ?: 0L,
                        uuid = existingReminder?.uuid ?: java.util.UUID.randomUUID().toString(),
                        title = title,
                        type = type,
                        time = time,
                        repeat = repeat,
                        medicineName = medicineName.takeIf { type == "Medicine" && it.isNotBlank() },
                        medicineQuantity = quantity.takeIf { type == "Medicine" && it.isNotBlank() },
                        medicineLocation = location.takeIf { type == "Medicine" && it.isNotBlank() },
                        additionalNotes = notes.takeIf { it.isNotBlank() },
                        photoRequired = photoRequired,
                        imageUri = existingReminder?.imageUri,
                        soundUri = existingReminder?.soundUri,
                        isActive = existingReminder?.isActive ?: true
                    )
                    if (existingReminder != null) {
                        viewModel.updateReminder(reminder)
                    } else {
                        viewModel.addReminder(reminder)
                    }
                    onBack()
                },
                modifier = Modifier.fillMaxWidth().height(64.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SageGreen),
                enabled = title.isNotBlank()
            ) {
                Text(if (existingReminder != null) "UPDATE REMINDER" else "SAVE REMINDER", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}
