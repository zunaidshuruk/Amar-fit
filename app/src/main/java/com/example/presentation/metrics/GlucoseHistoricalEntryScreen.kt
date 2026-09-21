package com.example.presentation.metrics

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.presentation.viewmodel.ShasthoViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlucoseHistoricalEntryScreen(viewModel: ShasthoViewModel, onNavigateBack: () -> Unit = {}) {
    val profile by viewModel.userProfile.collectAsState()
    val isDark = profile?.isDarkMode ?: isSystemInDarkTheme()
    val glucoseAccent = AccentTokens.glucoseAccent(isDark)
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var showDatePicker by remember { mutableStateOf(false) }
    var selectedDateMillis by remember { mutableStateOf<Long?>(null) }

    val todayStartMillis = remember {
        LocalDate.now().atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
    }

    val datePickerState = rememberDatePickerState(
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                return utcTimeMillis < todayStartMillis
            }
            override fun isSelectableYear(year: Int): Boolean {
                return year <= LocalDate.now().year
            }
        }
    )

    val selectedDateString = remember(selectedDateMillis) {
        selectedDateMillis?.let {
            Instant.ofEpochMilli(it).atZone(ZoneId.of("UTC")).toLocalDate().toString()
        }
    }
    val selectedDateDisplay = remember(selectedDateMillis) {
        selectedDateMillis?.let {
            Instant.ofEpochMilli(it).atZone(ZoneId.of("UTC")).toLocalDate()
                .format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
        } ?: "Select a date"
    }

    var beforeBreakfastInput by remember { mutableStateOf("") }
    var afterBreakfastInput by remember { mutableStateOf("") }
    var beforeLunchInput by remember { mutableStateOf("") }
    var afterLunchInput by remember { mutableStateOf("") }
    var beforeDinnerInput by remember { mutableStateOf("") }
    var afterDinnerInput by remember { mutableStateOf("") }
    var specimenSource by remember { mutableStateOf("Not set") }
    var expandedSpecimenSource by remember { mutableStateOf(false) }
    val specimenSourceOptions = listOf("Not set", "Interstitial fluid", "Capillary blood", "Plasma", "Serum", "Tears", "Whole blood")

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    if (datePickerState.selectedDateMillis != null) {
                        selectedDateMillis = datePickerState.selectedDateMillis
                    }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                text = "Add Historical Glucose Entry",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        OutlinedTextField(
            value = selectedDateDisplay,
            onValueChange = {},
            readOnly = true,
            label = { Text("Date") },
            trailingIcon = {
                IconButton(onClick = { showDatePicker = true }) {
                    Icon(Icons.Default.DateRange, contentDescription = "Pick date")
                }
            },
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            shape = RoundedCornerShape(12.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = glucoseAccent.bg),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Readings (mmol/L)", fontWeight = FontWeight.Bold, color = glucoseAccent.onBg)
                Spacer(modifier = Modifier.height(12.dp))

                Text("Breakfast", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = glucoseAccent.onBg)
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = beforeBreakfastInput,
                        onValueChange = { beforeBreakfastInput = it },
                        label = { Text("Before") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = afterBreakfastInput,
                        onValueChange = { afterBreakfastInput = it },
                        label = { Text("After") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text("Lunch", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = glucoseAccent.onBg)
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = beforeLunchInput,
                        onValueChange = { beforeLunchInput = it },
                        label = { Text("Before") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = afterLunchInput,
                        onValueChange = { afterLunchInput = it },
                        label = { Text("After") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text("Dinner", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = glucoseAccent.onBg)
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = beforeDinnerInput,
                        onValueChange = { beforeDinnerInput = it },
                        label = { Text("Before") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = afterDinnerInput,
                        onValueChange = { afterDinnerInput = it },
                        label = { Text("After") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                ExposedDropdownMenuBox(
                    expanded = expandedSpecimenSource,
                    onExpandedChange = { expandedSpecimenSource = !expandedSpecimenSource },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = specimenSource,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Specimen source") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedSpecimenSource) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = expandedSpecimenSource,
                        onDismissRequest = { expandedSpecimenSource = false }
                    ) {
                        specimenSourceOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    specimenSource = option
                                    expandedSpecimenSource = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        val date = selectedDateString
                        if (date == null) {
                            android.widget.Toast.makeText(context, "Pick a date first", android.widget.Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val bb = beforeBreakfastInput.toFloatOrNull()
                        val ab = afterBreakfastInput.toFloatOrNull()
                        val bl = beforeLunchInput.toFloatOrNull()
                        val al = afterLunchInput.toFloatOrNull()
                        val bd = beforeDinnerInput.toFloatOrNull()
                        val ad = afterDinnerInput.toFloatOrNull()
                        if (bb == null && ab == null && bl == null && al == null && bd == null && ad == null) {
                            android.widget.Toast.makeText(context, "Enter a valid glucose reading", android.widget.Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        coroutineScope.launch {
                            val existing = viewModel.getMetricsForDateFlow(date).firstOrNull()
                            val hasExistingGlucose = existing != null && listOf(
                                existing.bloodGlucoseMorning,
                                existing.bloodGlucoseNight,
                                existing.bloodGlucoseBeforeBreakfast,
                                existing.bloodGlucoseAfterBreakfast,
                                existing.bloodGlucoseBeforeLunch,
                                existing.bloodGlucoseAfterLunch,
                                existing.bloodGlucoseBeforeDinner,
                                existing.bloodGlucoseAfterDinner
                            ).any { it > 0f }
                            if (hasExistingGlucose) {
                                android.widget.Toast.makeText(context, "This date already has glucose data logged — skipped to avoid overwriting it", android.widget.Toast.LENGTH_LONG).show()
                            } else {
                                bb?.let { viewModel.setBloodGlucoseBeforeBreakfast(it, specimenSource, date) }
                                ab?.let { viewModel.setBloodGlucoseAfterBreakfast(it, specimenSource, date) }
                                bl?.let { viewModel.setBloodGlucoseBeforeLunch(it, specimenSource, date) }
                                al?.let { viewModel.setBloodGlucoseAfterLunch(it, specimenSource, date) }
                                bd?.let { viewModel.setBloodGlucoseBeforeDinner(it, specimenSource, date) }
                                ad?.let { viewModel.setBloodGlucoseAfterDinner(it, specimenSource, date) }
                                android.widget.Toast.makeText(context, "Historical reading(s) saved for $selectedDateDisplay", android.widget.Toast.LENGTH_SHORT).show()
                                onNavigateBack()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Indigo600)
                ) {
                    Text("Save Historical Entry")
                }
            }
        }

        Spacer(modifier = Modifier.height(100.dp))
    }
}
