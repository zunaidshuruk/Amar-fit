package com.example.presentation.workout

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.WorkoutProgram
import com.example.data.model.ProgramDay
import com.example.data.model.ProgramDaysJson
import com.example.data.model.WorkoutPlan
import com.example.presentation.viewmodel.ShasthoViewModel

@Composable
fun ProgramsSection(
    viewModel: ShasthoViewModel,
    onStartDay: (programId: String, dayIndex: Int, plan: WorkoutPlan) -> Unit
) {
    val programs by viewModel.workoutPrograms.collectAsState()
    val savedWorkouts by viewModel.savedWorkouts.collectAsState()
    var selectedId by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    var showNewProgramDialog by remember { mutableStateOf(false) }
    var newProgramName by remember { mutableStateOf("") }

    var showAddDayDialog by remember { mutableStateOf(false) }
    var showPickSavedDialog by remember { mutableStateOf(false) }
    var planToEditIndex by remember { mutableStateOf<Int?>(null) }
    var newCustomPlanForDay by remember { mutableStateOf<WorkoutPlan?>(null) }

    var showRestartConfirmDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    fun saveDays(program: WorkoutProgram, days: List<ProgramDay>, nextIndex: Int = program.nextDayIndex) {
        val boundedNext = nextIndex.coerceIn(0, maxOf(0, days.size - 1))
        viewModel.saveWorkoutProgram(
            program.copy(
                daysJson = ProgramDaysJson.toJson(days),
                nextDayIndex = boundedNext
            )
        )
    }

    val selectedProgram = programs.firstOrNull { it.cloudId == selectedId }

    if (selectedProgram == null) {
        // LIST VIEW
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Programs",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Button(
                    onClick = {
                        newProgramName = ""
                        showNewProgramDialog = true
                    },
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("New program")
                }
            }

            if (programs.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Text(
                        text = "A program is a series of workouts you follow day by day: Day 1, Day 2 and so on. Create one to get started.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    programs.forEach { program ->
                        val days = remember(program.daysJson) { ProgramDaysJson.parse(program.daysJson) }
                        val n = days.size
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = program.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                val subtitle = if (n > 0) {
                                    "$n days • Day ${program.nextDayIndex + 1} of $n is next • ${program.completedDays} days completed"
                                } else {
                                    "0 days • ${program.completedDays} days completed"
                                }
                                Text(
                                    text = subtitle,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (n > 0) {
                                    LinearProgressIndicator(
                                        progress = { (program.nextDayIndex.toFloat() / n.toFloat()).coerceIn(0f, 1f) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Button(
                                        onClick = { selectedId = program.cloudId },
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Open")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    } else {
        // DETAIL VIEW
        val days = remember(selectedProgram.daysJson) { ProgramDaysJson.parse(selectedProgram.daysJson) }
        val n = days.size

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { selectedId = null }) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = selectedProgram.title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f)
                )
            }

            // Summary Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val summaryText = if (n > 0) {
                        "Day ${selectedProgram.nextDayIndex + 1} of $n is next • ${selectedProgram.completedDays} days completed"
                    } else {
                        "0 days • ${selectedProgram.completedDays} days completed"
                    }
                    Text(
                        text = summaryText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { showRestartConfirmDialog = true },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Restart")
                        }
                        TextButton(
                            onClick = { showDeleteConfirmDialog = true }
                        ) {
                            Text("Delete program", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            // Days list
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                days.forEachIndexed { i, day ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "Day ${i + 1}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (i == selectedProgram.nextDayIndex) {
                                            Text(
                                                text = "NEXT",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        } else if (i < selectedProgram.nextDayIndex) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = "Completed",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = day.label.ifBlank { day.plan?.title ?: "Rest day" },
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    val detailSubtitle = if (!day.isRestDay && day.plan != null) {
                                        val exCount = day.plan.warmup.size + day.plan.mainExercises.size + day.plan.cooldown.size
                                        if (day.plan.rounds > 1) "$exCount exercises • ${day.plan.rounds} rounds" else "$exCount exercises"
                                    } else {
                                        "Rest day"
                                    }
                                    Text(
                                        text = detailSubtitle,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (!day.isRestDay && day.plan != null) {
                                    Button(
                                        onClick = { onStartDay(selectedProgram.cloudId, i, day.plan) },
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Start")
                                    }
                                } else {
                                    Button(
                                        onClick = { viewModel.completeProgramDay(selectedProgram.cloudId, i) },
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Mark done")
                                    }
                                }
                            }

                            // Day Actions (Edit, Move Up, Move Down, Delete)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (!day.isRestDay && day.plan != null) {
                                    IconButton(
                                        onClick = { planToEditIndex = i },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = "Edit day",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = {
                                        if (i > 0) {
                                            val list = days.toMutableList()
                                            val prev = list[i - 1]
                                            list[i - 1] = list[i]
                                            list[i] = prev
                                            saveDays(selectedProgram, list)
                                        }
                                    },
                                    enabled = i > 0,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        Icons.Default.KeyboardArrowUp,
                                        contentDescription = "Move up",
                                        modifier = Modifier.size(18.dp),
                                        tint = if (i > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        if (i < days.lastIndex) {
                                            val list = days.toMutableList()
                                            val next = list[i + 1]
                                            list[i + 1] = list[i]
                                            list[i] = next
                                            saveDays(selectedProgram, list)
                                        }
                                    },
                                    enabled = i < days.lastIndex,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Move down",
                                        modifier = Modifier.size(18.dp),
                                        tint = if (i < days.lastIndex) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        val list = days.toMutableList()
                                        list.removeAt(i)
                                        val nextIdx = if (i < selectedProgram.nextDayIndex) {
                                            selectedProgram.nextDayIndex - 1
                                        } else {
                                            selectedProgram.nextDayIndex
                                        }
                                        saveDays(selectedProgram, list, nextIdx)
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete day",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Add day button
            OutlinedButton(
                onClick = { showAddDayDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Add day", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    // Dialog: New Program
    if (showNewProgramDialog) {
        AlertDialog(
            onDismissRequest = { showNewProgramDialog = false },
            title = { Text("New program") },
            text = {
                OutlinedTextField(
                    value = newProgramName,
                    onValueChange = { if (it.length <= 40) newProgramName = it },
                    label = { Text("Program name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val name = newProgramName.trim()
                        if (name.isNotBlank()) {
                            val newProg = WorkoutProgram(title = name)
                            viewModel.saveWorkoutProgram(newProg)
                            selectedId = newProg.cloudId
                            showNewProgramDialog = false
                        }
                    },
                    enabled = newProgramName.trim().isNotBlank()
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewProgramDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog: Add a Day
    if (showAddDayDialog && selectedProgram != null) {
        val days = remember(selectedProgram.daysJson) { ProgramDaysJson.parse(selectedProgram.daysJson) }
        AlertDialog(
            onDismissRequest = { showAddDayDialog = false },
            title = { Text("Add a day") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = {
                            showAddDayDialog = false
                            showPickSavedDialog = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Pick from my saved workouts",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Start
                        )
                    }
                    TextButton(
                        onClick = {
                            showAddDayDialog = false
                            newCustomPlanForDay = WorkoutPlan(title = "Day ${days.size + 1}")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Build a new workout",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Start
                        )
                    }
                    TextButton(
                        onClick = {
                            val list = days + ProgramDay(label = "Rest day", isRestDay = true)
                            saveDays(selectedProgram, list)
                            showAddDayDialog = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Rest day",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Start
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAddDayDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog: Pick From Saved Workouts
    if (showPickSavedDialog && selectedProgram != null) {
        val days = remember(selectedProgram.daysJson) { ProgramDaysJson.parse(selectedProgram.daysJson) }
        val parseableSaved = remember(savedWorkouts) {
            savedWorkouts.mapNotNull { w ->
                if (w.structuredJson.isNotBlank()) {
                    try {
                        val plan = com.example.data.remote.RetrofitClient.moshi
                            .adapter(WorkoutPlan::class.java)
                            .fromJson(w.structuredJson)
                        if (plan != null) w to plan else null
                    } catch (e: Exception) {
                        null
                    }
                } else null
            }
        }

        AlertDialog(
            onDismissRequest = { showPickSavedDialog = false },
            title = { Text("Pick from saved workouts") },
            text = {
                if (parseableSaved.isEmpty()) {
                    Text(
                        text = "You have no saved workouts with exercises yet.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        parseableSaved.forEach { (workout, plan) ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val list = days + ProgramDay(
                                            label = plan.title.ifBlank { workout.title },
                                            isRestDay = false,
                                            plan = plan
                                        )
                                        saveDays(selectedProgram, list)
                                        showPickSavedDialog = false
                                    },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = workout.title.ifBlank { plan.title },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    val exCount = plan.warmup.size + plan.mainExercises.size + plan.cooldown.size
                                    Text(
                                        text = "$exCount exercises",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showPickSavedDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog: Restart confirmation
    if (showRestartConfirmDialog && selectedProgram != null) {
        AlertDialog(
            onDismissRequest = { showRestartConfirmDialog = false },
            title = { Text("Restart Program") },
            text = { Text("Restart this program from Day 1?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.restartWorkoutProgram(selectedProgram.cloudId)
                        showRestartConfirmDialog = false
                    }
                ) {
                    Text("Restart")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestartConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog: Delete confirmation
    if (showDeleteConfirmDialog && selectedProgram != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete Program") },
            text = { Text("Are you sure you want to delete \"${selectedProgram.title}\"?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteWorkoutProgram(selectedProgram.cloudId)
                        selectedId = null
                        showDeleteConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // WorkoutPlanEditor: Edit Day
    if (planToEditIndex != null && selectedProgram != null) {
        val days = remember(selectedProgram.daysJson) { ProgramDaysJson.parse(selectedProgram.daysJson) }
        val dayIdx = planToEditIndex!!
        if (dayIdx in days.indices) {
            val currentPlan = days[dayIdx].plan ?: WorkoutPlan(title = days[dayIdx].label)
            WorkoutPlanEditor(
                initialPlan = currentPlan,
                onSave = { newPlan ->
                    val list = days.toMutableList()
                    list[dayIdx] = list[dayIdx].copy(label = newPlan.title, plan = newPlan, isRestDay = false)
                    saveDays(selectedProgram, list)
                    planToEditIndex = null
                },
                onDismiss = { planToEditIndex = null },
                loadLibrary = { viewModel.getExerciseLibrary(context) }
            )
        }
    }

    // WorkoutPlanEditor: Build New Workout Day
    if (newCustomPlanForDay != null && selectedProgram != null) {
        val days = remember(selectedProgram.daysJson) { ProgramDaysJson.parse(selectedProgram.daysJson) }
        WorkoutPlanEditor(
            initialPlan = newCustomPlanForDay!!,
            onSave = { newPlan ->
                val list = days + ProgramDay(label = newPlan.title, isRestDay = false, plan = newPlan)
                saveDays(selectedProgram, list)
                newCustomPlanForDay = null
            },
            onDismiss = { newCustomPlanForDay = null },
            loadLibrary = { viewModel.getExerciseLibrary(context) }
        )
    }
}
