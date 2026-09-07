package com.example.presentation.mindfulness

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.health.connect.client.records.MindfulnessSessionRecord
import com.example.presentation.viewmodel.ShasthoViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant

data class MindfulnessTypeOption(
    val type: Int,
    val title: String,
    val subtitle: String,
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MindfulnessTimerScreen(
    viewModel: ShasthoViewModel,
    onNavigateBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val userProfile by viewModel.userProfile.collectAsState()
    val isDark = userProfile?.isDarkMode ?: isSystemInDarkTheme()

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

    fun finishSession() {
        if (!sessionSaved && sessionStartTime != null && elapsedSeconds > 0) {
            sessionSaved = true
            val endTime = Instant.now()
            coroutineScope.launch {
                viewModel.saveCompletedMindfulnessSession(
                    sessionType = selectedType,
                    startTime = sessionStartTime!!,
                    endTime = endTime
                )
            }
        }
        isCompleted = true
        isRunning = false
        isPaused = false
    }

    // Interval countdown timer mirroring WorkoutSessionScreen LaunchedEffect
    LaunchedEffect(isRunning, isPaused, isCompleted) {
        while (isRunning && !isPaused && !isCompleted) {
            delay(1000L)
            elapsedSeconds++
            if (remainingSeconds > 1) {
                remainingSeconds--
            } else {
                remainingSeconds = 0
                finishSession()
            }
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
                        finishSession()
                        onNavigateBack()
                    }
                ) {
                    Text("Save & Exit", fontWeight = FontWeight.Bold, color = Emerald600)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showExitDialog = false
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
                        .background(Emerald500.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Completed",
                        tint = Emerald500,
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
                            tint = Emerald500,
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
                                text = "Mindfulness minutes have been recorded to your daily health vitals.",
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
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
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
                                containerColor = if (isSelected) Emerald500.copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.surface
                            ),
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, Emerald500)
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
                                    tint = if (isSelected) Emerald500 else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = option.title,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Emerald500 else MaterialTheme.colorScheme.onSurface
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
                                    if (isSelected) Emerald600
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
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
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
                        sessionStartTime = Instant.now()
                        remainingSeconds = selectedDurationMinutes * 60
                        elapsedSeconds = 0
                        isRunning = true
                        isPaused = false
                        isCompleted = false
                        sessionSaved = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("start_mindfulness_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
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

                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .scale(if (!isPaused) pulseScale else 1.0f)
                        .clip(CircleShape)
                        .background(Emerald500.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(190.dp)
                            .clip(CircleShape)
                            .background(Emerald500.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
                                color = if (isPaused) Orange500 else Emerald600
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = if (isPaused) "Session paused" else if (selectedType == MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_BREATHING) "Inhale deeply... exhale slowly..." else "Focus on your breath and let thoughts pass naturally.",
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

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
                        onClick = { isPaused = !isPaused },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("pause_resume_mindfulness_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isPaused) Emerald600 else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(
                            imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = if (isPaused) "Resume" else "Pause",
                            tint = if (isPaused) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isPaused) "Resume" else "Pause",
                            fontWeight = FontWeight.Bold,
                            color = if (isPaused) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
