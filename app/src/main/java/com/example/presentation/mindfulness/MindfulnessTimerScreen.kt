package com.example.presentation.mindfulness

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.health.connect.client.records.MindfulnessSessionRecord
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.presentation.viewmodel.ShasthoViewModel
import com.example.ui.theme.*
import com.example.util.MusicNotificationManager
import com.example.util.StepSessionCounter
import com.example.util.WalkingMetrics
import com.example.util.WorkoutMusic
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import kotlin.math.ceil

data class MindfulnessTypeOption(
    val type: Int,
    val title: String,
    val subtitle: String,
    val icon: ImageVector
)

private fun vibrateTick(context: Context) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            val vibrator = vibratorManager?.defaultVibrator
            if (vibrator?.hasVibrator() == true) {
                vibrator.vibrate(VibrationEffect.createOneShot(30L, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (vibrator?.hasVibrator() == true) {
                vibrator.vibrate(VibrationEffect.createOneShot(30L, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        }
    } catch (e: Exception) {
        // ignore
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MindfulnessTimerScreen(
    viewModel: ShasthoViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()
    val userProfile by viewModel.userProfile.collectAsState()
    val isDark = true
    val mindfulnessAccent = AccentTokens.mindfulnessAccent(isDark)

    val prefs = remember { context.getSharedPreferences("ShasthoPrefs", Context.MODE_PRIVATE) }

    var breathPatternId by remember { mutableStateOf(prefs.getString("breath_pattern", "coherent") ?: "coherent") }
    var breathCustomCsv by remember { mutableStateOf(prefs.getString("breath_custom", "4,2,6,0") ?: "4,2,6,0") }
    var breathHaptics by remember { mutableStateOf(prefs.getBoolean("breath_haptics", true)) }

    val sessionClock = remember { SessionClock { SystemClock.elapsedRealtime() } }
    var tickMs by remember { mutableLongStateOf(0L) }

    val sessionTypes = remember {
        listOf(
            MindfulnessTypeOption(
                type = MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_MEDITATION,
                title = "Meditation",
                subtitle = "Calm focus & mental clarity",
                icon = Icons.Default.SelfImprovement
            ),
            MindfulnessTypeOption(
                type = MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_BREATHING,
                title = "Breathing",
                subtitle = "Parasympathetic activation",
                icon = Icons.Default.Air
            ),
            MindfulnessTypeOption(
                type = MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_MOVEMENT,
                title = "Movement",
                subtitle = "Mindful stretching & walking",
                icon = Icons.Default.DirectionsWalk
            )
        )
    }

    val durationOptions = remember { listOf(1, 3, 5, 10, 15) }

    val savedPlaylists = remember { WorkoutMusic.getPlaylists(context) }
    val initialActivePlaylist = remember { WorkoutMusic.getActivePlaylist(context) }
    var selectedPlaylistForSession by remember { mutableStateOf<WorkoutMusic.Playlist?>(initialActivePlaylist) }
    var musicEnabledForSession by remember { mutableStateOf(false) }

    val stepCounter = remember { StepSessionCounter(context) }
    val walkSteps by stepCounter.steps.collectAsState()
    var finalWalkSteps by remember { mutableIntStateOf(0) }
    var keepScreenOnWalking by remember { mutableStateOf(prefs.getBoolean("walk_keep_screen_on", false)) }
    var activityLogged by remember { mutableStateOf(false) }

    var stepsPermissionGranted by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < 29 || ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val nowPlayingState by MusicNotificationManager.nowPlaying.collectAsState()

    DisposableEffect(context) {
        MusicNotificationManager.register(context)
        onDispose {
            MusicNotificationManager.unregister(context)
        }
    }

    var selectedType by remember { mutableIntStateOf(MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_MEDITATION) }
    var selectedDurationMinutes by remember { mutableIntStateOf(5) }

    var isRunning by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var isCompleted by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }

    var sessionStartTime by remember { mutableStateOf<Instant?>(null) }
    var sessionSaved by remember { mutableStateOf(false) }

    var remainingSeconds by remember { mutableIntStateOf(selectedDurationMinutes * 60) }
    var elapsedSeconds by remember { mutableIntStateOf(0) }

    val currentTypeOption = sessionTypes.find { it.type == selectedType } ?: sessionTypes.first()

    val activePattern = remember(selectedType, breathPatternId, breathCustomCsv) {
        when (selectedType) {
            MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_BREATHING ->
                BreathingPattern.fromPrefs(breathPatternId, breathCustomCsv)
            MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_MEDITATION ->
                BreathingPattern.MEDITATION
            else -> null
        }
    }

    val breathState = remember(activePattern, tickMs) {
        activePattern?.stateAt(tickMs / 1000f)
    }

    var lastPhaseKey by remember { mutableIntStateOf(-1) }
    val currentPhaseKey = remember(breathState) {
        if (breathState != null) (breathState.cycleIndex * 10 + breathState.phaseIndex) else -1
    }

    LaunchedEffect(currentPhaseKey, isRunning, isPaused, isCompleted) {
        if (breathHaptics && isRunning && !isPaused && !isCompleted && activePattern != null && currentPhaseKey != -1) {
            if (lastPhaseKey != -1 && lastPhaseKey != currentPhaseKey) {
                vibrateTick(context)
            }
            lastPhaseKey = currentPhaseKey
        }
    }

    fun startSession() {
        if (musicEnabledForSession && selectedPlaylistForSession != null) {
            WorkoutMusic.openPlaylist(context, selectedPlaylistForSession!!)
        }
        sessionStartTime = Instant.now()
        remainingSeconds = selectedDurationMinutes * 60
        elapsedSeconds = 0
        tickMs = 0L
        lastPhaseKey = -1
        sessionClock.reset()
        sessionClock.start()
        isRunning = true
        isPaused = false
        isCompleted = false
        sessionSaved = false
        activityLogged = false
        if (selectedType == MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_MOVEMENT) {
            if (stepsPermissionGranted && stepCounter.isAvailable()) {
                stepCounter.start()
            }
        }
    }

    val activityPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        stepsPermissionGranted = granted
        startSession()
    }

    DisposableEffect(Unit) {
        onDispose {
            stepCounter.stop()
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, isRunning, isCompleted, selectedType) {
        val observer = LifecycleEventObserver { _, event ->
            if (selectedType == MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_MOVEMENT && isRunning && !isCompleted) {
                if (event == Lifecycle.Event.ON_STOP) {
                    stepCounter.unregisterKeepingBaseline()
                } else if (event == Lifecycle.Event.ON_START) {
                    stepCounter.reRegister()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    DisposableEffect(isRunning, isCompleted, keepScreenOnWalking, selectedType) {
        view.keepScreenOn = isRunning && !isCompleted && (selectedType != MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_MOVEMENT || keepScreenOnWalking)
        onDispose {
            view.keepScreenOn = false
        }
    }

    fun finishSession() {
        elapsedSeconds = (sessionClock.elapsedMs() / 1000).toInt()
        if (selectedType == MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_MOVEMENT) {
            finalWalkSteps = walkSteps
            stepCounter.stop()
        }
        if (!sessionSaved && sessionStartTime != null && elapsedSeconds > 0) {
            sessionSaved = true
            val endTime = Instant.now()
            coroutineScope.launch {
                viewModel.saveCompletedMindfulnessSession(
                    sessionType = selectedType,
                    startTime = sessionStartTime!!,
                    endTime = endTime,
                    steps = if (selectedType == MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_MOVEMENT) finalWalkSteps else 0
                )
            }
        }
        isCompleted = true
        isRunning = false
        isPaused = false
    }

    // Frame loop for real-time UI clock rendering
    LaunchedEffect(isRunning, isPaused, isCompleted) {
        while (isRunning && !isPaused && !isCompleted) {
            withFrameMillis { }
            tickMs = sessionClock.elapsedMs()
            elapsedSeconds = (tickMs / 1000).toInt()
            remainingSeconds = (((selectedDurationMinutes * 60000L - tickMs) + 999) / 1000).toInt().coerceAtLeast(0)
        }
    }

    // Separate end trigger for background & screen-off accuracy
    LaunchedEffect(isRunning, isPaused, isCompleted, selectedDurationMinutes) {
        if (isRunning && !isPaused && !isCompleted) {
            val remainingMs = (selectedDurationMinutes * 60000L - sessionClock.elapsedMs()).coerceAtLeast(0L)
            delay(remainingMs)
            elapsedSeconds = selectedDurationMinutes * 60
            remainingSeconds = 0
            finishSession()
        }
    }

    BackHandler(enabled = isRunning && !isCompleted) {
        showExitDialog = true
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("End Session Early?") },
            text = { Text("Would you like to save your progress (${(elapsedSeconds / 60).coerceAtLeast(if (elapsedSeconds > 0) 1 else 0)}m) or discard?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExitDialog = false
                        if (selectedType == MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_MOVEMENT) {
                            finalWalkSteps = walkSteps
                            stepCounter.stop()
                        }
                        finishSession()
                        onNavigateBack()
                    }
                ) {
                    Text("Save & Exit", fontWeight = FontWeight.Bold, color = mindfulnessAccent.onBg)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showExitDialog = false
                        if (selectedType == MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_MOVEMENT) {
                            stepCounter.stop()
                        }
                        isRunning = false
                        isPaused = false
                        onNavigateBack()
                    }
                ) {
                    Text("Discard", color = MaterialTheme.colorScheme.error)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mindfulness Session", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (isRunning && !isCompleted) {
                                showExitDialog = true
                            } else {
                                onNavigateBack()
                            }
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isCompleted) {
                // Completed View
                Spacer(modifier = Modifier.height(32.dp))
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(mindfulnessAccent.bg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Completed",
                        tint = mindfulnessAccent.onBg,
                        modifier = Modifier.size(56.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Session Complete!",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "You practiced ${currentTypeOption.title} for ${(elapsedSeconds / 60).coerceAtLeast(if (elapsedSeconds > 0) 1 else 0)} minutes.",
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val cycleCount = if (activePattern != null) {
                    activePattern.stateAt(elapsedSeconds.toFloat()).cycleIndex
                } else 0

                if ((selectedType == MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_BREATHING ||
                     selectedType == MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_MEDITATION) &&
                    cycleCount > 0
                ) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$cycleCount full breaths",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                if (selectedType == MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_MOVEMENT && stepCounter.isAvailable() && stepsPermissionGranted) {
                    val heightCm = userProfile?.heightCm ?: 170f
                    val weightKg = userProfile?.weightKg ?: 70f
                    val imperial = userProfile?.useImperialUnits ?: false

                    val distMeters = WalkingMetrics.distanceMeters(finalWalkSteps, heightCm)
                    val formatDist = WalkingMetrics.formatDistance(distMeters, imperial)
                    val kcal = WalkingMetrics.calories(weightKg, elapsedSeconds)
                    val spm = WalkingMetrics.stepsPerMinute(finalWalkSteps, elapsedSeconds)
                    val durationMin = (elapsedSeconds / 60).coerceAtLeast(1)

                    LaunchedEffect(finalWalkSteps) {
                        if (finalWalkSteps > 0 && !activityLogged) {
                            activityLogged = true
                            viewModel.logActivity(
                                "mindfulness",
                                "Mindful walk: $finalWalkSteps steps, $formatDist, about $kcal kcal in $durationMin min"
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Steps", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(text = String.format(java.util.Locale.US, "%,d", finalWalkSteps), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Distance", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(text = formatDist, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Calories (estimate)", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(text = "$kcal kcal", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Average pace", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(text = "$spm steps/min", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = null,
                            tint = mindfulnessAccent.onBg,
                            modifier = Modifier.size(28.dp)
                        )
                        Column {
                            Text(
                                text = "Synced to Health Connect",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (selectedType == MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_MOVEMENT)
                                    "Mindfulness minutes recorded. (Steps are tracked on device and not duplicated to Health Connect)."
                                else
                                    "Mindfulness minutes have been recorded to your daily health vitals.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("mindfulness_done_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = mindfulnessAccent.onBg),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Done", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            } else if (!isRunning) {
                // Setup / Selection View
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Select Practice Type",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    sessionTypes.forEach { option ->
                        val isSelected = selectedType == option.type
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedType = option.type }
                                .testTag("type_${option.title.lowercase()}"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) mindfulnessAccent.bg
                                else MaterialTheme.colorScheme.surface
                            ),
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, mindfulnessAccent.onBg)
                            else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp, horizontal = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = option.icon,
                                    contentDescription = option.title,
                                    tint = if (isSelected) mindfulnessAccent.onBg else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = option.title,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) mindfulnessAccent.onBg else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Select Duration",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    durationOptions.forEach { minutes ->
                        val isSelected = selectedDurationMinutes == minutes
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (isSelected) mindfulnessAccent.onBg
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable {
                                    selectedDurationMinutes = minutes
                                    remainingSeconds = minutes * 60
                                }
                                .padding(vertical = 12.dp)
                                .testTag("duration_${minutes}m"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${minutes}m",
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                if (selectedType == MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_BREATHING) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Breathing pattern",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val patternOptions = remember {
                        listOf(
                            "coherent" to "Coherent 5-5",
                            "box" to "Box 4-4-4-4",
                            "relaxing" to "Relaxing 4-7-8",
                            "custom" to "Custom"
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        patternOptions.forEach { (id, label) ->
                            FilterChip(
                                selected = breathPatternId == id,
                                onClick = {
                                    breathPatternId = id
                                    prefs.edit().putString("breath_pattern", id).apply()
                                },
                                label = { Text(label, fontSize = 13.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = mindfulnessAccent.bg,
                                    selectedLabelColor = mindfulnessAccent.onBg
                                )
                            )
                        }
                    }

                    if (breathPatternId == "custom") {
                        Spacer(modifier = Modifier.height(12.dp))
                        val customParts = remember(breathCustomCsv) {
                            try {
                                val p = breathCustomCsv.split(",").map { it.trim().toInt() }
                                if (p.size == 4) p else listOf(4, 2, 6, 0)
                            } catch (e: Exception) {
                                listOf(4, 2, 6, 0)
                            }
                        }

                        fun updateCustomPart(index: Int, newValue: Int) {
                            val updated = customParts.toMutableList()
                            updated[index] = newValue
                            val newCsv = updated.joinToString(",")
                            breathCustomCsv = newCsv
                            prefs.edit().putString("breath_custom", newCsv).apply()
                        }

                        val steppers = listOf(
                            Triple("Breathe in", 0, 2..10),
                            Triple("Hold", 1, 0..10),
                            Triple("Breathe out", 2, 2..12),
                            Triple("Hold after", 3, 0..10)
                        )

                        steppers.forEach { (label, index, range) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { updateCustomPart(index, (customParts[index] - 1).coerceIn(range.first, range.last)) }
                                    ) {
                                        Icon(Icons.Default.Remove, contentDescription = "Decrease $label", modifier = Modifier.size(18.dp))
                                    }
                                    Text(
                                        text = "${customParts[index]}s",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    IconButton(
                                        onClick = { updateCustomPart(index, (customParts[index] + 1).coerceIn(range.first, range.last)) }
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Increase $label", modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    val currentDisplayPattern = BreathingPattern.fromPrefs(breathPatternId, breathCustomCsv)
                    Text(
                        text = "One breath = ${currentDisplayPattern.cycleSeconds} seconds",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (selectedType == MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_BREATHING ||
                    selectedType == MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_MEDITATION) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Vibrate on each phase change",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Switch(
                            checked = breathHaptics,
                            onCheckedChange = {
                                breathHaptics = it
                                prefs.edit().putBoolean("breath_haptics", it).apply()
                            }
                        )
                    }
                }

                if (selectedType == MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_MOVEMENT) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Keep screen on while walking",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Switch(
                            checked = keepScreenOnWalking,
                            onCheckedChange = { checked ->
                                keepScreenOnWalking = checked
                                prefs.edit().putBoolean("walk_keep_screen_on", checked).apply()
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    if (stepCounter.isAvailable()) {
                        Text(
                            text = "Steps are counted by your phone's step sensor, starting from 0.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = "This phone has no step sensor; the session will only be timed.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Music",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (savedPlaylists.isEmpty()) {
                    Text(
                        text = "Add playlists in Settings > Workout music.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = !musicEnabledForSession,
                            onClick = {
                                musicEnabledForSession = false
                                selectedPlaylistForSession = null
                            },
                            label = { Text("No music", fontSize = 13.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = mindfulnessAccent.bg,
                                selectedLabelColor = mindfulnessAccent.onBg
                            )
                        )
                        savedPlaylists.forEach { playlist ->
                            val isSelected = musicEnabledForSession && selectedPlaylistForSession?.name == playlist.name
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    musicEnabledForSession = true
                                    selectedPlaylistForSession = playlist
                                    WorkoutMusic.setActivePlaylist(context, playlist.name)
                                },
                                label = { Text(playlist.name, fontSize = 13.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = mindfulnessAccent.bg,
                                    selectedLabelColor = mindfulnessAccent.onBg
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = currentTypeOption.title,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = currentTypeOption.subtitle,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = when (selectedType) {
                                MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_BREATHING ->
                                    "Deep diaphragmatic breathing calms the nervous system, decreases resting heart rate, and elevates HRV."
                                MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_MOVEMENT ->
                                    "Mindful movement and gentle dynamic stretching improve body awareness and release physical tension."
                                else ->
                                    "Mindful meditation helps reduce cortisol, enhance mental resilience, and promote deeper restorative sleep."
                            },
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = {
                        if (selectedType == MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_MOVEMENT &&
                            Build.VERSION.SDK_INT >= 29 &&
                            !stepsPermissionGranted
                        ) {
                            activityPermissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
                        } else {
                            startSession()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("start_mindfulness_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = mindfulnessAccent.onBg),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Start Guided Session",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                // Active / In-Progress Timer View
                Spacer(modifier = Modifier.height(24.dp))

                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                val pulseScale by infiniteTransition.animateFloat(
                    initialValue = 0.92f,
                    targetValue = 1.08f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 4000, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "scale"
                )

                val currentCircleScale = if (selectedType == MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_MOVEMENT) {
                    1.0f
                } else {
                    val fullness = breathState?.fullness ?: 0f
                    0.72f + 0.36f * fullness
                }

                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .scale(currentCircleScale)
                        .clip(CircleShape)
                        .background(mindfulnessAccent.onBg.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(190.dp)
                            .clip(CircleShape)
                            .background(mindfulnessAccent.onBg.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            if (selectedType == MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_MOVEMENT && stepCounter.isAvailable() && stepsPermissionGranted) {
                                Text(
                                    text = String.format(java.util.Locale.US, "%,d", walkSteps),
                                    fontSize = 44.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = if (isPaused) "Paused" else "steps",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isPaused) MaterialTheme.colorScheme.tertiary else mindfulnessAccent.onBg
                                )
                            } else {
                                val mins = remainingSeconds / 60
                                val secs = remainingSeconds % 60
                                val timeText = String.format(java.util.Locale.US, "%02d:%02d", mins, secs)

                                Text(
                                    text = timeText,
                                    fontSize = 42.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (isPaused) "Paused" else currentTypeOption.title,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isPaused) MaterialTheme.colorScheme.tertiary else mindfulnessAccent.onBg
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                if (selectedType == MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_MOVEMENT) {
                    val mins = remainingSeconds / 60
                    val secs = remainingSeconds % 60
                    val timeText = String.format(java.util.Locale.US, "%02d:%02d", mins, secs)

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = timeText,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "remaining",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (stepCounter.isAvailable() && stepsPermissionGranted) {
                        Spacer(modifier = Modifier.height(16.dp))
                        val heightCm = userProfile?.heightCm ?: 170f
                        val weightKg = userProfile?.weightKg ?: 70f
                        val imperial = userProfile?.useImperialUnits ?: false

                        val distMeters = WalkingMetrics.distanceMeters(walkSteps, heightCm)
                        val distFormatted = WalkingMetrics.formatDistance(distMeters, imperial)
                        val kcalFormatted = "${WalkingMetrics.calories(weightKg, elapsedSeconds)} kcal"
                        val spmFormatted = "${WalkingMetrics.stepsPerMinute(walkSteps, elapsedSeconds)}"

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = distFormatted, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Text(text = "Distance", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = kcalFormatted, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Text(text = "Calories", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = spmFormatted, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Text(text = "Steps/min", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    if (!isPaused) {
                        Spacer(modifier = Modifier.height(16.dp))
                        val guidanceList = listOf(
                            "Feel your feet meeting the ground.",
                            "Let your arms swing naturally.",
                            "Notice your breathing as you walk.",
                            "Look up and soften your gaze.",
                            "Listen to the sounds around you.",
                            "Relax your shoulders and jaw."
                        )
                        val guidanceIndex = (elapsedSeconds / 30) % guidanceList.size
                        Text(
                            text = guidanceList[guidanceIndex],
                            fontSize = 14.sp,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                } else {
                    if (isPaused) {
                        Text(
                            text = "Paused",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = mindfulnessAccent.onBg,
                            textAlign = TextAlign.Center
                        )
                    } else if (breathState != null) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = breathState.kind.label,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = mindfulnessAccent.onBg,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            val secsLeft = ceil(breathState.phaseSeconds - breathState.secondsIntoPhase).toInt().coerceAtLeast(1)
                            Text(
                                text = "$secsLeft",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { finishSession() },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("stop_mindfulness_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = "Stop")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Stop", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            if (isPaused) {
                                sessionClock.resume()
                                if (selectedType == MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_MOVEMENT) {
                                    stepCounter.resume()
                                }
                                isPaused = false
                            } else {
                                sessionClock.pause()
                                if (selectedType == MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_MOVEMENT) {
                                    stepCounter.pause()
                                }
                                isPaused = true
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("pause_resume_mindfulness_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isPaused) mindfulnessAccent.onBg else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(
                            imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = if (isPaused) "Resume" else "Pause",
                            tint = if (isPaused) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isPaused) "Resume" else "Pause",
                            fontWeight = FontWeight.Bold,
                            color = if (isPaused) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if ((musicEnabledForSession && selectedPlaylistForSession != null) || nowPlayingState.isPlaying || nowPlayingState.title.isNotBlank()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (nowPlayingState.hasPermission) {
                                if (nowPlayingState.title.isNotBlank()) {
                                    Text(
                                        text = nowPlayingState.title,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        textAlign = TextAlign.Center,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (nowPlayingState.artist.isNotBlank()) {
                                        Text(
                                            text = nowPlayingState.artist,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            textAlign = TextAlign.Center,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                } else {
                                    Text(
                                        text = "Music Controls",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Enable Notification Access for track info",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    TextButton(
                                        onClick = { MusicNotificationManager.openNotificationAccessSettings(context) },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text("Grant", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { MusicNotificationManager.skipToPrevious(context) },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SkipPrevious,
                                        contentDescription = "Previous track",
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                IconButton(
                                    onClick = { MusicNotificationManager.togglePlayPause(context) },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = if (nowPlayingState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = if (nowPlayingState.isPlaying) "Pause music" else "Play music",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                IconButton(
                                    onClick = { MusicNotificationManager.skipToNext(context) },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SkipNext,
                                        contentDescription = "Next track",
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        if (selectedPlaylistForSession != null) {
                                            WorkoutMusic.openPlaylist(context, selectedPlaylistForSession!!)
                                        } else {
                                            WorkoutMusic.openPlaylist(context)
                                        }
                                    },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.QueueMusic,
                                        contentDescription = "Open my playlist",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
