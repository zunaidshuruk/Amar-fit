package com.example.presentation.workout

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.LibraryExercise
import com.example.util.ExerciseCalorieEstimator
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogExerciseDialog(
    loadLibrary: suspend () -> List<LibraryExercise>,
    weightKg: Float,
    onSave: suspend (name: String, category: String, sets: Int?, reps: Int?, durationSeconds: Int, startTime: Instant, caloriesOverride: Int?) -> Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var library by remember { mutableStateOf<List<LibraryExercise>>(emptyList()) }
    LaunchedEffect(Unit) {
        library = try {
            loadLibrary()
        } catch (e: Exception) {
            emptyList()
        }
    }

    var query by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("strength") }
    var mode by remember { mutableIntStateOf(0) } // 0 = "Sets × reps", 1 = "Duration"
    var sets by remember { mutableIntStateOf(3) }
    var reps by remember { mutableIntStateOf(10) }
    var restSeconds by remember { mutableIntStateOf(60) }
    var minutes by remember { mutableIntStateOf(20) }
    var dayOffset by remember { mutableIntStateOf(0) } // 0 = Today, 1 = Yesterday

    val now = remember { LocalTime.now() }
    val initialDurationSeconds = remember(mode, sets, reps, restSeconds, minutes) {
        if (mode == 0) ExerciseCalorieEstimator.sessionSeconds(sets, reps, restSeconds) else minutes * 60
    }
    var startMinutes by remember {
        val nowMins = now.hour * 60 + now.minute
        val durationMins = (initialDurationSeconds + 59) / 60
        mutableIntStateOf((nowMins - durationMins).coerceAtLeast(0))
    }

    var showTimePicker by remember { mutableStateOf(false) }
    var calorieText by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }

    val durationSeconds = if (mode == 0) ExerciseCalorieEstimator.sessionSeconds(sets, reps, restSeconds) else minutes * 60
    val estimate = ExerciseCalorieEstimator.estimateKcal(category, weightKg, durationSeconds)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Top Row: Title + Close Icon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Add exercise",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Exercise search / selection
                    item {
                        if (name.isBlank()) {
                            OutlinedTextField(
                                value = query,
                                onValueChange = { query = it },
                                label = { Text("Search or type an exercise") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            val q = query.trim()
                            val matches = remember(library, q) {
                                (if (q.isEmpty()) library.sortedBy { it.name } else library.filter { it.name.contains(q, ignoreCase = true) }).take(8)
                            }
                            val hasExactMatch = remember(library, q) {
                                library.any { it.name.equals(q, ignoreCase = true) }
                            }

                            if (q.length >= 2 && !hasExactMatch) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            name = q
                                        }
                                        .padding(vertical = 10.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Use \"$q\" as my own exercise",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            matches.forEach { item ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            name = item.name
                                            if (!item.category.isNullOrBlank()) {
                                                category = item.category
                                            }
                                        }
                                        .padding(vertical = 10.dp, horizontal = 4.dp)
                                ) {
                                    Text(
                                        text = item.name,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    val cat = item.category ?: "strength"
                                    Text(
                                        text = cat.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() },
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = name,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = category.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() },
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    TextButton(onClick = { name = "" }) {
                                        Text("Change")
                                    }
                                }
                            }
                        }
                    }

                    // Type Chips
                    item {
                        Text(
                            text = "Type",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                "Strength" to "strength",
                                "Cardio" to "cardio",
                                "Stretching" to "stretching",
                                "Plyometrics" to "plyometrics"
                            ).forEach { (label, catVal) ->
                                FilterChip(
                                    selected = category.equals(catVal, ignoreCase = true),
                                    onClick = { category = catVal },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }

                    // Mode: Sets x reps vs Duration
                    item {
                        Text(
                            text = "Logging Mode",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            FilterChip(
                                selected = mode == 0,
                                onClick = { mode = 0 },
                                label = { Text("Sets × reps") }
                            )
                            FilterChip(
                                selected = mode == 1,
                                onClick = { mode = 1 },
                                label = { Text("Duration") }
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (mode == 0) {
                                    StepperRow(
                                        label = "Sets",
                                        valueText = "$sets",
                                        onMinus = { sets = (sets - 1).coerceAtLeast(1) },
                                        onPlus = { sets = (sets + 1).coerceAtMost(10) },
                                        minusEnabled = sets > 1,
                                        plusEnabled = sets < 10
                                    )
                                    StepperRow(
                                        label = "Reps",
                                        valueText = "$reps",
                                        onMinus = { reps = (reps - 1).coerceAtLeast(1) },
                                        onPlus = { reps = (reps + 1).coerceAtMost(50) },
                                        minusEnabled = reps > 1,
                                        plusEnabled = reps < 50
                                    )
                                    StepperRow(
                                        label = "Rest between sets",
                                        valueText = "$restSeconds s",
                                        onMinus = { restSeconds = (restSeconds - 15).coerceAtLeast(0) },
                                        onPlus = { restSeconds = (restSeconds + 15).coerceAtMost(180) },
                                        minusEnabled = restSeconds > 0,
                                        plusEnabled = restSeconds < 180
                                    )
                                } else {
                                    StepperRow(
                                        label = "Minutes",
                                        valueText = "$minutes min",
                                        onMinus = { minutes = (minutes - 1).coerceAtLeast(1) },
                                        onPlus = { minutes = (minutes + 1).coerceAtMost(180) },
                                        minusEnabled = minutes > 1,
                                        plusEnabled = minutes < 180
                                    )
                                }

                                Text(
                                    text = "About ${(durationSeconds + 30) / 60} min",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // When: Today / Yesterday + Start Time
                    item {
                        Text(
                            text = "When",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilterChip(
                                selected = dayOffset == 0,
                                onClick = { dayOffset = 0 },
                                label = { Text("Today") }
                            )
                            FilterChip(
                                selected = dayOffset == 1,
                                onClick = { dayOffset = 1 },
                                label = { Text("Yesterday") }
                            )

                            Spacer(modifier = Modifier.weight(1f))

                            val startLocalTime = LocalTime.of(startMinutes / 60, startMinutes % 60)
                            val timeFormat = DateTimeFormatter.ofPattern("h:mm a", Locale.US)
                            OutlinedButton(
                                onClick = { showTimePicker = true },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Started at ${startLocalTime.format(timeFormat)}")
                            }
                        }
                    }

                    // Calories
                    item {
                        Text(
                            text = "Calories",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Estimated: ~$estimate kcal",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = calorieText,
                            onValueChange = { newText ->
                                if (newText.all { it.isDigit() } && newText.length <= 4) {
                                    calorieText = newText
                                }
                            },
                            label = { Text("Calories (optional, to override)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Save Button
                Button(
                    onClick = {
                        if (name.isBlank() || saving) return@Button
                        saving = true
                        coroutineScope.launch {
                            val startTime = LocalDate.now()
                                .minusDays(dayOffset.toLong())
                                .atStartOfDay(ZoneId.systemDefault())
                                .plusMinutes(startMinutes.toLong())
                                .toInstant()

                            val success = onSave(
                                name.trim(),
                                category,
                                if (mode == 0) sets else null,
                                if (mode == 0) reps else null,
                                durationSeconds,
                                startTime,
                                calorieText.toIntOrNull()
                            )
                            saving = false
                            if (success) {
                                Toast.makeText(context, "Exercise logged", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            } else {
                                Toast.makeText(context, "Could not log it. It may already be logged.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    enabled = name.isNotBlank() && !saving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    if (saving) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Save", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showTimePicker) {
        val initialHour = startMinutes / 60
        val initialMinute = startMinutes % 60
        val timePickerState = rememberTimePickerState(
            initialHour = initialHour,
            initialMinute = initialMinute,
            is24Hour = false
        )

        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        startMinutes = timePickerState.hour * 60 + timePickerState.minute
                        showTimePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Cancel")
                }
            },
            text = {
                TimePicker(state = timePickerState)
            }
        )
    }
}

@Composable
private fun StepperRow(
    label: String,
    valueText: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    minusEnabled: Boolean,
    plusEnabled: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onMinus,
                enabled = minusEnabled
            ) {
                Icon(
                    Icons.Default.Remove,
                    contentDescription = "Decrease $label",
                    tint = if (minusEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            }
            Text(
                text = valueText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(min = 64.dp)
            )
            IconButton(
                onClick = onPlus,
                enabled = plusEnabled
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Increase $label",
                    tint = if (plusEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            }
        }
    }
}
