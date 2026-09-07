package com.example.presentation.workout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.UserProfile
import com.example.data.model.WorkoutExercise
import com.example.data.model.WorkoutPlan
import com.example.presentation.viewmodel.ShasthoViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.delay

enum class SessionStepType {
    EXERCISE,
    REST
}

data class SessionStep(
    val stepId: Int,
    val type: SessionStepType,
    val exercise: WorkoutExercise?,
    val title: String,
    val sectionName: String,
    val durationSeconds: Int?,
    val exerciseIndex: Int,
    val totalExercises: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutSessionScreen(
    plan: WorkoutPlan,
    userProfile: UserProfile?,
    viewModel: ShasthoViewModel,
    onExit: () -> Unit
) {
    val isDark = userProfile?.isDarkMode ?: isSystemInDarkTheme()

    // 1. Flatten warmup + mainExercises + cooldown into ordered steps with rest periods
    val sessionSteps = remember(plan) {
        val steps = mutableListOf<SessionStep>()
        val exerciseList = mutableListOf<Pair<String, WorkoutExercise>>()
        plan.warmup.forEach { exerciseList.add("Warmup" to it) }
        plan.mainExercises.forEach { exerciseList.add("Main Exercise" to it) }
        plan.cooldown.forEach { exerciseList.add("Cooldown" to it) }

        val totalCount = exerciseList.size
        var stepIdCounter = 0
        exerciseList.forEachIndexed { index, (section, exercise) ->
            stepIdCounter++
            steps.add(
                SessionStep(
                    stepId = stepIdCounter,
                    type = SessionStepType.EXERCISE,
                    exercise = exercise,
                    title = exercise.name,
                    sectionName = section,
                    durationSeconds = exercise.durationSeconds?.takeIf { it > 0 },
                    exerciseIndex = index + 1,
                    totalExercises = totalCount
                )
            )

            // Insert rest period after each exercise using restSeconds,
            // skipping if restSeconds <= 0 and skipping trailing rest after last exercise
            if (exercise.restSeconds > 0 && index < totalCount - 1) {
                stepIdCounter++
                steps.add(
                    SessionStep(
                        stepId = stepIdCounter,
                        type = SessionStepType.REST,
                        exercise = exercise,
                        title = "Rest Period",
                        sectionName = "Rest",
                        durationSeconds = exercise.restSeconds,
                        exerciseIndex = index + 1,
                        totalExercises = totalCount
                    )
                )
            }
        }
        steps
    }

    var currentStepIndex by remember { mutableIntStateOf(0) }
    var isPaused by remember { mutableStateOf(false) }
    var isCompleted by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }

    // Elapsed tracking
    var remainingSeconds by remember { mutableIntStateOf(0) }
    var currentStepElapsedSeconds by remember { mutableIntStateOf(0) }
    var totalSessionElapsedSeconds by remember { mutableIntStateOf(0) }
    val exerciseElapsedTimes = remember { mutableStateMapOf<Int, Int>() }

    // Current exercise video state
    var currentVideoId by remember { mutableStateOf<String?>(null) }
    var isVideoLoading by remember { mutableStateOf(false) }

    val currentStep = sessionSteps.getOrNull(currentStepIndex)

    // Lazy / On-demand video resolution for the current exercise
    LaunchedEffect(currentStepIndex) {
        val step = sessionSteps.getOrNull(currentStepIndex)
        remainingSeconds = step?.durationSeconds ?: 0
        currentStepElapsedSeconds = 0

        if (step?.type == SessionStepType.EXERCISE && step.exercise != null) {
            isVideoLoading = true
            currentVideoId = null
            val query = step.exercise.youtubeSearchQuery.ifBlank { step.exercise.name }
            currentVideoId = viewModel.resolveYoutubeVideoId(query)
            isVideoLoading = false
        } else {
            currentVideoId = null
            isVideoLoading = false
        }
    }

    fun advanceStep() {
        val step = sessionSteps.getOrNull(currentStepIndex)
        if (step?.type == SessionStepType.EXERCISE && step.exercise != null) {
            val existing = exerciseElapsedTimes[step.stepId] ?: 0
            exerciseElapsedTimes[step.stepId] = existing + currentStepElapsedSeconds
        }

        if (currentStepIndex + 1 < sessionSteps.size) {
            currentStepIndex++
        } else {
            isCompleted = true
        }
    }

    // Interval countdown timer
    LaunchedEffect(currentStepIndex, isPaused, isCompleted) {
        while (!isPaused && !isCompleted && currentStepIndex < sessionSteps.size) {
            delay(1000L)
            currentStepElapsedSeconds++
            totalSessionElapsedSeconds++

            val step = sessionSteps.getOrNull(currentStepIndex) ?: break
            if (step.durationSeconds != null) {
                if (remainingSeconds > 1) {
                    remainingSeconds--
                } else {
                    remainingSeconds = 0
                    advanceStep()
                }
            }
        }
    }

    // Calorie calculation helper
    fun computeCalories(): Double? {
        val weightKg = userProfile?.weightKg
        if (weightKg == null || weightKg <= 0f) return null

        var total = 0.0
        sessionSteps.forEach { step ->
            if (step.type == SessionStepType.EXERCISE && step.exercise != null) {
                val sec = if (step.stepId == currentStep?.stepId && !isCompleted) {
                    (exerciseElapsedTimes[step.stepId] ?: 0) + currentStepElapsedSeconds
                } else {
                    exerciseElapsedTimes[step.stepId] ?: 0
                }
                if (sec > 0) {
                    val met = if (step.exercise.metValue > 0) step.exercise.metValue else 3.5
                    total += met * weightKg.toDouble() * (sec.toDouble() / 3600.0)
                }
            }
        }
        return total
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isCompleted) "Workout Summary" else plan.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (isCompleted) {
                                onExit()
                            } else {
                                showExitDialog = true
                            }
                        }
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Exit session")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isCompleted) {
                // Summary Screen
                WorkoutCompletionSummary(
                    planTitle = plan.title,
                    totalSeconds = totalSessionElapsedSeconds,
                    totalCalories = computeCalories(),
                    hasWeight = (userProfile?.weightKg ?: 0f) > 0f,
                    completedSteps = sessionSteps.filter { it.type == SessionStepType.EXERCISE },
                    exerciseElapsedTimes = exerciseElapsedTimes,
                    isDark = isDark,
                    onFinish = onExit
                )
            } else if (currentStep != null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header progress indicator
                    val progressFraction = (currentStepIndex.toFloat() / sessionSteps.size.toFloat()).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = Secondary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (currentStep.type == SessionStepType.EXERCISE) {
                                "Exercise ${currentStep.exerciseIndex} of ${currentStep.totalExercises}"
                            } else {
                                "Rest Interval"
                            },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Live time
                        val mins = totalSessionElapsedSeconds / 60
                        val secs = totalSessionElapsedSeconds % 60
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Timer,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "%02d:%02d".format(mins, secs),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Step Title & Section Chip
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = when (currentStep.sectionName) {
                                "Warmup" -> AccentTokens.stepsAccent(isDark).bg
                                "Cooldown" -> AccentTokens.waterAccent(isDark).bg
                                "Rest" -> MaterialTheme.colorScheme.secondaryContainer
                                else -> AccentTokens.caloriesAccent(isDark).bg
                            }
                        ) {
                            Text(
                                text = currentStep.sectionName.uppercase(),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = when (currentStep.sectionName) {
                                    "Warmup" -> AccentTokens.stepsAccent(isDark).onBg
                                    "Cooldown" -> AccentTokens.waterAccent(isDark).onBg
                                    "Rest" -> MaterialTheme.colorScheme.onSecondaryContainer
                                    else -> AccentTokens.caloriesAccent(isDark).onBg
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = currentStep.title,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Media embed or Rest visual
                    if (currentStep.type == SessionStepType.EXERCISE && currentStep.exercise != null) {
                        YouTubeVideoEmbed(
                            videoId = currentVideoId,
                            isLoading = isVideoLoading,
                            searchQuery = currentStep.exercise.youtubeSearchQuery.ifBlank { currentStep.exercise.name }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Exercise details card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceAround,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (currentStep.exercise.sets != null) {
                                        WorkoutMetricBadge(
                                            label = "Sets",
                                            value = "${currentStep.exercise.sets}"
                                        )
                                    }
                                    if (!currentStep.exercise.reps.isNullOrBlank()) {
                                        WorkoutMetricBadge(
                                            label = "Reps",
                                            value = currentStep.exercise.reps
                                        )
                                    }
                                    if (currentStep.exercise.durationSeconds != null && currentStep.exercise.durationSeconds > 0) {
                                        WorkoutMetricBadge(
                                            label = "Duration",
                                            value = "${currentStep.exercise.durationSeconds}s"
                                        )
                                    }
                                    WorkoutMetricBadge(
                                        label = "Rest",
                                        value = "${currentStep.exercise.restSeconds}s"
                                    )
                                }
                            }
                        }
                    } else {
                        // REST Step visual
                        val nextStep = sessionSteps.getOrNull(currentStepIndex + 1)
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(Secondary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SelfImprovement,
                                        contentDescription = null,
                                        tint = Secondary,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Rest & Catch Your Breath",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Hydrate and get ready for the next round",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                if (nextStep != null && nextStep.type == SessionStepType.EXERCISE) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text(
                                                text = "UP NEXT",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = nextStep.title,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Main Active Timer or Rep Done Action
                    if (currentStep.durationSeconds != null) {
                        // Duration-based countdown
                        val totalStepSec = currentStep.durationSeconds
                        val timerFraction = if (totalStepSec > 0) remainingSeconds.toFloat() / totalStepSec.toFloat() else 0f

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.size(140.dp)
                            ) {
                                CircularProgressIndicator(
                                    progress = { timerFraction },
                                    modifier = Modifier.fillMaxSize(),
                                    strokeWidth = 8.dp,
                                    color = if (currentStep.type == SessionStepType.REST) Secondary else Orange500,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    val m = remainingSeconds / 60
                                    val s = remainingSeconds % 60
                                    Text(
                                        text = "%02d:%02d".format(m, s),
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (isPaused) "PAUSED" else "REMAINING",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isPaused) Red500 else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        // Rep-based exercise: manual DONE button
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.padding(bottom = 16.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Timelapse,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Elapsed: ${currentStepElapsedSeconds}s",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            Button(
                                onClick = { advanceStep() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Primary)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Done Exercise", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Playback Controls (Pause / Resume / Skip)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Pause / Resume
                        OutlinedButton(
                            onClick = { isPaused = !isPaused },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Icon(
                                imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                contentDescription = if (isPaused) "Resume" else "Pause"
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isPaused) "Resume" else "Pause", fontWeight = FontWeight.SemiBold)
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // Skip Step
                        Button(
                            onClick = { advanceStep() },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Icon(Icons.Default.SkipNext, contentDescription = "Skip step")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Skip", fontWeight = FontWeight.SemiBold)
                        }
                    }

                    // Live Calorie Burn Footer (if user weight is available)
                    val liveCals = computeCalories()
                    if (liveCals != null && liveCals > 0.1) {
                        Spacer(modifier = Modifier.height(20.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(AccentTokens.caloriesAccent(isDark).bg)
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                Icons.Default.LocalFireDepartment,
                                contentDescription = null,
                                tint = Orange500,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Est. Burn: ${"%.1f".format(liveCals)} kcal",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentTokens.caloriesAccent(isDark).onBg
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("End Workout Session?") },
            text = { Text("Are you sure you want to exit? Your current session progress will not be saved.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExitDialog = false
                        onExit()
                    }
                ) {
                    Text("End Session", color = Red500, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("Continue Workout")
                }
            }
        )
    }
}

@Composable
fun WorkoutMetricBadge(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun WorkoutCompletionSummary(
    planTitle: String,
    totalSeconds: Int,
    totalCalories: Double?,
    hasWeight: Boolean,
    completedSteps: List<SessionStep>,
    exerciseElapsedTimes: Map<Int, Int>,
    isDark: Boolean,
    onFinish: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(Secondary.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.EmojiEvents,
                contentDescription = null,
                tint = Secondary,
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Workout Completed!",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = planTitle,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Big Stats Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Total Time
                val minutes = totalSeconds / 60
                val seconds = totalSeconds % 60
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Timer,
                        contentDescription = null,
                        tint = AccentTokens.waterAccent(isDark).onBg,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "%02d:%02d".format(minutes, seconds),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Total Time",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                HorizontalDivider(
                    modifier = Modifier
                        .height(40.dp)
                        .width(1.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                )

                // Calories
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.LocalFireDepartment,
                        contentDescription = null,
                        tint = Orange500,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    if (totalCalories != null && hasWeight) {
                        Text(
                            text = "${totalCalories.toInt()} kcal",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "MET Est. Burn",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = "--",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "No Weight Set",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (!hasWeight) {
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Calorie estimate unavailable — add your weight in Settings",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Exercises Breakdown
        Text(
            text = "Exercises Completed",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        )

        completedSteps.forEach { step ->
            val sec = exerciseElapsedTimes[step.stepId] ?: 0
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = step.title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = step.sectionName,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "${sec}s",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Exit / Back Button
        Button(
            onClick = onFinish,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Primary)
        ) {
            Text("Finish Session", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
