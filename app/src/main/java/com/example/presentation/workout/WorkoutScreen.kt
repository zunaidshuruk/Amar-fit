package com.example.presentation.workout

import android.content.Intent
import android.net.Uri
import android.text.util.Linkify
import android.widget.TextView
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.util.LinkifyCompat
import com.example.data.local.SavedWorkout
import com.example.data.model.WorkoutExercise
import com.example.data.model.WorkoutPlan
import com.example.presentation.viewmodel.ShasthoViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date

@Composable
fun WorkoutScreen(viewModel: ShasthoViewModel) {
    val navController = com.example.LocalNavController.current
    val profile by viewModel.userProfile.collectAsState()
    val isDark = profile?.isDarkMode ?: isSystemInDarkTheme()

    val structuredPlan by viewModel.structuredWorkoutPlan.collectAsState()
    val rawStructuredJson by viewModel.rawStructuredWorkoutJson.collectAsState()
    val isLoadingStructured by viewModel.isLoadingStructuredWorkout.collectAsState()
    val structuredError by viewModel.structuredWorkoutError.collectAsState()
    val savedWorkouts by viewModel.savedWorkouts.collectAsState()
    
    var selectedTab by remember { mutableStateOf(0) } // 0 = AI Workouts, 1 = Saved
    var selectedSavedWorkout by remember { mutableStateOf<SavedWorkout?>(null) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var workoutTitle by remember { mutableStateOf("") }
    var activeSessionPlan by remember { mutableStateOf<WorkoutPlan?>(null) }

    val bodyTextColor = if (isDark) {
        android.graphics.Color.parseColor("#E2E8F0")
    } else {
        android.graphics.Color.parseColor("#1E293B")
    }
    val linkTextColor = if (isDark) {
        android.graphics.Color.parseColor("#93C5FD")
    } else {
        android.graphics.Color.parseColor("#3B82F6")
    }
    
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.syncErrorEvent.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    if (activeSessionPlan != null) {
        WorkoutSessionScreen(
            plan = activeSessionPlan!!,
            userProfile = profile,
            viewModel = viewModel,
            onExit = { activeSessionPlan = null }
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        if (selectedSavedWorkout != null) {
            SavedWorkoutDetailView(
                workout = selectedSavedWorkout!!,
                isDark = isDark,
                onStartWorkout = { plan -> activeSessionPlan = plan },
                onBack = { selectedSavedWorkout = null }
            )
        } else {
            Text(
                text = "Workouts",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 16.dp, bottom = 16.dp)
            )

            // Tabs
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                Button(
                    onClick = { selectedTab = 0 },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedTab == 0) Orange500 else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (selectedTab == 0) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.weight(1f).padding(end = 4.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("AI Workouts", color = if (selectedTab == 0) Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Button(
                    onClick = { selectedTab = 1 },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedTab == 1) Orange500 else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (selectedTab == 1) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.weight(1f).padding(start = 4.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Saved", color = if (selectedTab == 1) Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            if (selectedTab == 0) {
                // Generate View
                Text(
                    text = "Personalized routines based on your profile",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
                
                Button(
                    onClick = { viewModel.generateAIStructuredWorkout() },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Orange500),
                    shape = RoundedCornerShape(16.dp),
                    enabled = !isLoadingStructured
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isLoadingStructured) "Generating..." else "Generate Today's Workout", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                if (isLoadingStructured) {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Orange500)
                    }
                } else if (structuredError != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = structuredError!!,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(16.dp),
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                } else if (structuredPlan != null) {
                    val plan = structuredPlan!!
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                            Text(
                                text = plan.title,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )

                            if (plan.warmup.isNotEmpty()) {
                                WorkoutSectionView(
                                    sectionTitle = "Warmup",
                                    exercises = plan.warmup,
                                    sectionColor = AccentTokens.stepsAccent(isDark)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                            }

                            if (plan.mainExercises.isNotEmpty()) {
                                WorkoutSectionView(
                                    sectionTitle = "Main Exercises",
                                    exercises = plan.mainExercises,
                                    sectionColor = AccentTokens.caloriesAccent(isDark)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                            }

                            if (plan.cooldown.isNotEmpty()) {
                                WorkoutSectionView(
                                    sectionTitle = "Cooldown",
                                    exercises = plan.cooldown,
                                    sectionColor = AccentTokens.waterAccent(isDark)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Action Buttons: Start Workout, Save Workout, and Browse Exercises
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { activeSessionPlan = plan },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Primary),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Start Workout", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        }

                        OutlinedButton(
                            onClick = { showSaveDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Save Workout", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        }

                        OutlinedButton(
                            onClick = { navController?.navigate("exercise_library") },
                            modifier = Modifier
                                .weight(1.1f)
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            Icon(Icons.Default.FitnessCenter, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Browse Exercises", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                }
                
                Text(
                    text = "Core Protocol Practices",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                ProtocolPracticeCard(
                    title = "Physical Exercise & HIIT",
                    description = "High-Intensity Interval Training to deplete glycogen stores and boost growth hormone.",
                    icon = Icons.Default.FitnessCenter,
                    accent = AccentTokens.caloriesAccent(isDark),
                    videoQuery = "Keto HIIT workout 20 min"
                )
                Spacer(modifier = Modifier.height(16.dp))
                ProtocolPracticeCard(
                    title = "Hormonal Health & Sun",
                    description = "Morning sunlight exposure to regulate circadian rhythm and optimize Vitamin D / Cortisol balance.",
                    icon = Icons.Default.WbSunny,
                    accent = AccentTokens.pointsAccent(isDark),
                    videoQuery = "Morning sunlight circadian rhythm optimization"
                )
                Spacer(modifier = Modifier.height(16.dp))
                ProtocolPracticeCard(
                    title = "Breathwork & Meditation",
                    description = "Diaphragmatic breathing to activate the parasympathetic nervous system and reduce cortisol.",
                    icon = Icons.Default.SelfImprovement,
                    accent = AccentTokens.stepsAccent(isDark),
                    videoQuery = "Wim Hof method breathing tutorial",
                    onStartSession = { navController?.navigate("mindfulness_timer") }
                )
            } else {
                // Saved View
                if (savedWorkouts.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
                        Text("No saved workouts yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    savedWorkouts.forEach { workout ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).clickable { selectedSavedWorkout = workout },
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(workout.title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    Text(
                                        SimpleDateFormat("MMM dd, yyyy").format(Date(workout.createdAt)), 
                                        fontSize = 12.sp, 
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { viewModel.deleteWorkout(workout) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Red500)
                                }
                            }
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(100.dp))
    }

    if (showSaveDialog && (structuredPlan != null || !workoutTitle.isEmpty())) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save Workout") },
            text = {
                OutlinedTextField(
                    value = workoutTitle,
                    onValueChange = { workoutTitle = it },
                    label = { Text("Workout Title") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val defaultTitle = structuredPlan?.title?.ifBlank { "My Workout" } ?: "My Workout"
                    val finalTitle = workoutTitle.ifBlank { defaultTitle }
                    val contentDescription = buildString {
                        structuredPlan?.let { plan ->
                            appendLine(plan.title)
                            if (plan.warmup.isNotEmpty()) {
                                appendLine("\nWarmup:")
                                plan.warmup.forEach { appendLine("• ${it.name} - ${formatExerciseDetails(it)}") }
                            }
                            if (plan.mainExercises.isNotEmpty()) {
                                appendLine("\nMain Exercises:")
                                plan.mainExercises.forEach { appendLine("• ${it.name} - ${formatExerciseDetails(it)}") }
                            }
                            if (plan.cooldown.isNotEmpty()) {
                                appendLine("\nCooldown:")
                                plan.cooldown.forEach { appendLine("• ${it.name} - ${formatExerciseDetails(it)}") }
                            }
                        }
                    }
                    viewModel.saveWorkout(
                        title = finalTitle,
                        content = contentDescription,
                        structuredJson = rawStructuredJson
                    )
                    showSaveDialog = false
                    workoutTitle = ""
                    selectedTab = 1
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) { Text("Cancel") }
            }
        )
    }
}

private fun formatExerciseDetails(exercise: WorkoutExercise): String {
    val details = mutableListOf<String>()
    if (exercise.sets != null) {
        details.add("${exercise.sets} sets")
    }
    if (!exercise.reps.isNullOrBlank()) {
        details.add("${exercise.reps} reps")
    }
    if (exercise.durationSeconds != null && exercise.durationSeconds > 0) {
        details.add("${exercise.durationSeconds}s")
    }
    if (exercise.restSeconds > 0) {
        details.add("${exercise.restSeconds}s rest")
    }
    return details.joinToString(" • ")
}

@Composable
fun WorkoutSectionView(
    sectionTitle: String,
    exercises: List<WorkoutExercise>,
    sectionColor: AccentColors
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp, 16.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(sectionColor.onBg)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = sectionTitle,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        exercises.forEach { exercise ->
            ExerciseItemRow(exercise = exercise)
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun ExerciseItemRow(exercise: WorkoutExercise) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = exercise.name,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            val details = formatExerciseDetails(exercise)
            if (details.isNotBlank()) {
                Text(
                    text = details,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        val query = exercise.youtubeSearchQuery.ifBlank { exercise.name }
        IconButton(
            onClick = {
                val intent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://www.youtube.com/results?search_query=${Uri.encode(query)}")
                )
                context.startActivity(intent)
            },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                Icons.Default.PlayCircle,
                contentDescription = "Watch tutorial on YouTube",
                tint = Red500
            )
        }
    }
}

@Composable
fun SavedWorkoutDetailView(
    workout: SavedWorkout,
    isDark: Boolean,
    onStartWorkout: (WorkoutPlan) -> Unit,
    onBack: () -> Unit
) {
    val bodyTextColor = if (isDark) {
        android.graphics.Color.parseColor("#E2E8F0")
    } else {
        android.graphics.Color.parseColor("#1E293B")
    }
    val linkTextColor = if (isDark) {
        android.graphics.Color.parseColor("#93C5FD")
    } else {
        android.graphics.Color.parseColor("#3B82F6")
    }

    val parsedPlan: WorkoutPlan? = remember(workout.structuredJson) {
        if (workout.structuredJson.isNotBlank()) {
            try {
                com.example.data.remote.RetrofitClient.moshi
                    .adapter(WorkoutPlan::class.java)
                    .fromJson(workout.structuredJson)
            } catch (e: Exception) {
                null
            }
        } else null
    }

    Column {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 16.dp)) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
            }
            Text(text = workout.title, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (parsedPlan != null) {
                    if (parsedPlan.warmup.isNotEmpty()) {
                        WorkoutSectionView(
                            sectionTitle = "Warmup",
                            exercises = parsedPlan.warmup,
                            sectionColor = AccentTokens.stepsAccent(isDark)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    if (parsedPlan.mainExercises.isNotEmpty()) {
                        WorkoutSectionView(
                            sectionTitle = "Main Exercises",
                            exercises = parsedPlan.mainExercises,
                            sectionColor = AccentTokens.caloriesAccent(isDark)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    if (parsedPlan.cooldown.isNotEmpty()) {
                        WorkoutSectionView(
                            sectionTitle = "Cooldown",
                            exercises = parsedPlan.cooldown,
                            sectionColor = AccentTokens.waterAccent(isDark)
                        )
                    }
                } else {
                    AndroidView(
                        factory = { ctx ->
                            TextView(ctx).apply {
                                textSize = 16f
                                setLineSpacing(0f, 1.3f)
                                autoLinkMask = Linkify.WEB_URLS
                                linksClickable = true
                            }
                        },
                        update = { textView ->
                            textView.setTextColor(bodyTextColor)
                            textView.setLinkTextColor(linkTextColor)
                            textView.text = workout.content
                            LinkifyCompat.addLinks(textView, Linkify.WEB_URLS)
                        }
                    )
                }
            }
        }

        if (parsedPlan != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { onStartWorkout(parsedPlan) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Start Workout", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ProtocolPracticeCard(
    title: String,
    description: String,
    icon: ImageVector,
    accent: AccentColors,
    videoQuery: String,
    onStartSession: (() -> Unit)? = null
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(accent.bg)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accent.onBg)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = accent.onBg)
                Text(text = description, fontSize = 12.sp, color = accent.onBg, lineHeight = 16.sp, modifier = Modifier.padding(top = 4.dp))
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (onStartSession != null) {
                IconButton(
                    onClick = onStartSession,
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                ) {
                    Icon(Icons.Default.Timer, contentDescription = "Start Guided Session", tint = accent.onBg)
                }
            }
            IconButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=${Uri.encode(videoQuery)}"))
                    context.startActivity(intent)
                },
                modifier = Modifier.background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Watch", tint = accent.onBg)
            }
        }
    }
}
