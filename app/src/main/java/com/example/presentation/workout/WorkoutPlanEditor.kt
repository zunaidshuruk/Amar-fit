package com.example.presentation.workout

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.WorkoutExercise
import com.example.data.model.WorkoutPlan

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutPlanEditor(
    initialPlan: WorkoutPlan,
    onSave: (WorkoutPlan) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember(initialPlan) { mutableStateOf(initialPlan.title) }
    val warmup = remember(initialPlan) { mutableStateListOf<WorkoutExercise>().apply { addAll(initialPlan.warmup) } }
    val main = remember(initialPlan) { mutableStateListOf<WorkoutExercise>().apply { addAll(initialPlan.mainExercises) } }
    val cooldown = remember(initialPlan) { mutableStateListOf<WorkoutExercise>().apply { addAll(initialPlan.cooldown) } }

    val totalExercises = warmup.size + main.size + cooldown.size

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
            ) {
                // Top bar Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Customize workout",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Button(
                        onClick = {
                            val updated = initialPlan.copy(
                                title = title.trim().ifBlank { initialPlan.title },
                                warmup = warmup.toList(),
                                mainExercises = main.toList(),
                                cooldown = cooldown.toList()
                            )
                            onSave(updated)
                        },
                        enabled = totalExercises > 0
                    ) {
                        Text("Save")
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Workout title") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (warmup.isNotEmpty()) {
                        item {
                            Text(
                                text = "Warmup",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                        items(warmup.size) { index ->
                            val exercise = warmup[index]
                            ExerciseCard(
                                exercise = exercise,
                                canMoveUp = index > 0,
                                canMoveDown = index < warmup.lastIndex,
                                canDelete = totalExercises > 1,
                                onMoveUp = {
                                    if (index > 0) {
                                        val prev = warmup[index - 1]
                                        warmup[index - 1] = exercise
                                        warmup[index] = prev
                                    }
                                },
                                onMoveDown = {
                                    if (index < warmup.lastIndex) {
                                        val next = warmup[index + 1]
                                        warmup[index + 1] = exercise
                                        warmup[index] = next
                                    }
                                },
                                onDelete = {
                                    if (totalExercises > 1) {
                                        warmup.removeAt(index)
                                    }
                                },
                                onUpdate = { updated ->
                                    warmup[index] = updated
                                }
                            )
                        }
                    }

                    if (main.isNotEmpty()) {
                        item {
                            Text(
                                text = "Main Exercises",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                        items(main.size) { index ->
                            val exercise = main[index]
                            ExerciseCard(
                                exercise = exercise,
                                canMoveUp = index > 0,
                                canMoveDown = index < main.lastIndex,
                                canDelete = totalExercises > 1,
                                onMoveUp = {
                                    if (index > 0) {
                                        val prev = main[index - 1]
                                        main[index - 1] = exercise
                                        main[index] = prev
                                    }
                                },
                                onMoveDown = {
                                    if (index < main.lastIndex) {
                                        val next = main[index + 1]
                                        main[index + 1] = exercise
                                        main[index] = next
                                    }
                                },
                                onDelete = {
                                    if (totalExercises > 1) {
                                        main.removeAt(index)
                                    }
                                },
                                onUpdate = { updated ->
                                    main[index] = updated
                                }
                            )
                        }
                    }

                    if (cooldown.isNotEmpty()) {
                        item {
                            Text(
                                text = "Cooldown",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                        items(cooldown.size) { index ->
                            val exercise = cooldown[index]
                            ExerciseCard(
                                exercise = exercise,
                                canMoveUp = index > 0,
                                canMoveDown = index < cooldown.lastIndex,
                                canDelete = totalExercises > 1,
                                onMoveUp = {
                                    if (index > 0) {
                                        val prev = cooldown[index - 1]
                                        cooldown[index - 1] = exercise
                                        cooldown[index] = prev
                                    }
                                },
                                onMoveDown = {
                                    if (index < cooldown.lastIndex) {
                                        val next = cooldown[index + 1]
                                        cooldown[index + 1] = exercise
                                        cooldown[index] = next
                                    }
                                },
                                onDelete = {
                                    if (totalExercises > 1) {
                                        cooldown.removeAt(index)
                                    }
                                },
                                onUpdate = { updated ->
                                    cooldown[index] = updated
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExerciseCard(
    exercise: WorkoutExercise,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    canDelete: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit,
    onUpdate: (WorkoutExercise) -> Unit
) {
    val isTimed = (exercise.durationSeconds ?: 0) > 0

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = exercise.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onMoveUp,
                    enabled = canMoveUp
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowUp,
                        contentDescription = "Move up",
                        tint = if (canMoveUp) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                }
                IconButton(
                    onClick = onMoveDown,
                    enabled = canMoveDown
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = "Move down",
                        tint = if (canMoveDown) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                }
                IconButton(
                    onClick = onDelete,
                    enabled = canDelete
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete exercise",
                        tint = if (canDelete) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                }
            }

            // Mode chips
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = !isTimed,
                    onClick = {
                        if (isTimed) {
                            onUpdate(
                                exercise.copy(
                                    durationSeconds = 0,
                                    reps = exercise.reps?.takeIf { it.isNotBlank() } ?: "10"
                                )
                            )
                        }
                    },
                    label = { Text("Reps") }
                )
                FilterChip(
                    selected = isTimed,
                    onClick = {
                        if (!isTimed) {
                            onUpdate(
                                exercise.copy(
                                    durationSeconds = (exercise.durationSeconds?.takeIf { it > 0 } ?: 30),
                                    reps = null
                                )
                            )
                        }
                    },
                    label = { Text("Timed") }
                )
            }

            // Sets stepper (1..10)
            val currentSets = exercise.sets ?: 1
            StepperRow(
                label = "Sets",
                valueText = "$currentSets",
                onMinus = { onUpdate(exercise.copy(sets = currentSets - 1)) },
                onPlus = { onUpdate(exercise.copy(sets = currentSets + 1)) },
                minusEnabled = currentSets > 1,
                plusEnabled = currentSets < 10
            )

            // Duration stepper (5..600, step 5) only in Timed mode
            if (isTimed) {
                val currentDuration = exercise.durationSeconds ?: 30
                StepperRow(
                    label = "Duration",
                    valueText = "$currentDuration s",
                    onMinus = { onUpdate(exercise.copy(durationSeconds = (currentDuration - 5).coerceAtLeast(5))) },
                    onPlus = { onUpdate(exercise.copy(durationSeconds = (currentDuration + 5).coerceAtMost(600))) },
                    minusEnabled = currentDuration > 5,
                    plusEnabled = currentDuration < 600
                )
            }

            // Rest stepper (0..300, step 5)
            val currentRest = exercise.restSeconds
            StepperRow(
                label = "Rest",
                valueText = "$currentRest s",
                onMinus = { onUpdate(exercise.copy(restSeconds = (currentRest - 5).coerceAtLeast(0))) },
                onPlus = { onUpdate(exercise.copy(restSeconds = (currentRest + 5).coerceAtMost(300))) },
                minusEnabled = currentRest > 0,
                plusEnabled = currentRest < 300
            )

            // Reps free text only in Reps mode
            if (!isTimed) {
                OutlinedTextField(
                    value = exercise.reps ?: "",
                    onValueChange = { newText ->
                        if (newText.length <= 16) {
                            onUpdate(exercise.copy(reps = newText))
                        }
                    },
                    label = { Text("Reps") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
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
