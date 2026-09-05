package com.example.presentation.workout

import android.content.Intent
import android.net.Uri
import android.text.util.Linkify
import android.widget.TextView
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.util.LinkifyCompat
import com.example.data.local.SavedWorkout
import com.example.presentation.viewmodel.ShasthoViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date

@Composable
fun WorkoutScreen(viewModel: ShasthoViewModel) {
    val workoutPlan by viewModel.workoutPlan.collectAsState()
    val isLoading by viewModel.isLoadingWorkout.collectAsState()
    val savedWorkouts by viewModel.savedWorkouts.collectAsState()
    
    var selectedTab by remember { mutableStateOf(0) } // 0 = AI Workouts, 1 = Saved
    var selectedSavedWorkout by remember { mutableStateOf<SavedWorkout?>(null) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var workoutTitle by remember { mutableStateOf("") }
    
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.syncErrorEvent.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        if (selectedSavedWorkout != null) {
            SavedWorkoutDetailView(
                workout = selectedSavedWorkout!!,
                onBack = { selectedSavedWorkout = null }
            )
        } else {
            Text(
                text = "Workouts",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(top = 16.dp, bottom = 16.dp)
            )

            // Tabs
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                Button(
                    onClick = { selectedTab = 0 },
                    colors = ButtonDefaults.buttonColors(containerColor = if (selectedTab == 0) Orange500 else Slate200),
                    modifier = Modifier.weight(1f).padding(end = 4.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("AI Workouts", color = if (selectedTab == 0) Color.White else Slate500)
                }
                Button(
                    onClick = { selectedTab = 1 },
                    colors = ButtonDefaults.buttonColors(containerColor = if (selectedTab == 1) Orange500 else Slate200),
                    modifier = Modifier.weight(1f).padding(start = 4.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Saved", color = if (selectedTab == 1) Color.White else Slate500)
                }
            }

            if (selectedTab == 0) {
                // Generate View
                Text(
                    text = "Personalized routines based on your profile",
                    fontSize = 14.sp,
                    color = Slate500,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
                
                Button(
                    onClick = { viewModel.generateAIWorkout() },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Orange500),
                    shape = RoundedCornerShape(16.dp),
                    enabled = !isLoading
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isLoading) "Generating..." else "Generate Today's Workout", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Orange500)
                    }
                } else if (workoutPlan != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp)
                        ) {
                            AndroidView(
                                factory = { ctx ->
                                    TextView(ctx).apply {
                                        setTextColor(android.graphics.Color.parseColor("#1E293B")) // Slate800
                                        textSize = 16f
                                        setLineSpacing(0f, 1.3f)
                                        autoLinkMask = Linkify.WEB_URLS
                                        linksClickable = true
                                        setLinkTextColor(android.graphics.Color.parseColor("#3B82F6")) // Blue500
                                    }
                                },
                                update = { textView ->
                                    textView.text = workoutPlan
                                    LinkifyCompat.addLinks(textView, Linkify.WEB_URLS)
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Save Button
                    Button(
                        onClick = { showSaveDialog = true },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save Workout", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                }
                
                Text(
                    text = "Core Protocol Practices",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                ProtocolPracticeCard(
                    title = "Physical Exercise & HIIT",
                    description = "High-Intensity Interval Training to deplete glycogen stores and boost growth hormone.",
                    icon = Icons.Default.FitnessCenter,
                    color = OrangeBg,
                    accent = Orange700,
                    videoQuery = "Keto HIIT workout 20 min"
                )
                Spacer(modifier = Modifier.height(16.dp))
                ProtocolPracticeCard(
                    title = "Hormonal Health & Sun",
                    description = "Morning sunlight exposure to regulate circadian rhythm and optimize Vitamin D / Cortisol balance.",
                    icon = Icons.Default.WbSunny,
                    color = IndigoBg,
                    accent = Indigo700,
                    videoQuery = "Morning sunlight circadian rhythm optimization"
                )
                Spacer(modifier = Modifier.height(16.dp))
                ProtocolPracticeCard(
                    title = "Breathwork & Meditation",
                    description = "Diaphragmatic breathing to activate the parasympathetic nervous system and reduce cortisol.",
                    icon = Icons.Default.SelfImprovement,
                    color = Emerald50,
                    accent = Emerald700,
                    videoQuery = "Wim Hof method breathing tutorial"
                )
            } else {
                // Saved View
                if (savedWorkouts.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
                        Text("No saved workouts yet.", color = Slate500)
                    }
                } else {
                    savedWorkouts.forEach { workout ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
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
                                    Text(workout.title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    Text(
                                        SimpleDateFormat("MMM dd, yyyy").format(Date(workout.createdAt)), 
                                        fontSize = 12.sp, 
                                        color = Slate500
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

    if (showSaveDialog && workoutPlan != null) {
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
                    viewModel.saveWorkout(
                        title = workoutTitle.ifBlank { "My Workout" },
                        content = workoutPlan!!
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

@Composable
fun SavedWorkoutDetailView(workout: SavedWorkout, onBack: () -> Unit) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 16.dp)) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Text(text = workout.title, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        }
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                AndroidView(
                    factory = { ctx ->
                        TextView(ctx).apply {
                            setTextColor(android.graphics.Color.parseColor("#1E293B"))
                            textSize = 16f
                            setLineSpacing(0f, 1.3f)
                            autoLinkMask = Linkify.WEB_URLS
                            linksClickable = true
                            setLinkTextColor(android.graphics.Color.parseColor("#3B82F6"))
                        }
                    },
                    update = { textView ->
                        textView.text = workout.content
                        LinkifyCompat.addLinks(textView, Linkify.WEB_URLS)
                    }
                )
            }
        }
    }
}

@Composable
fun ProtocolPracticeCard(title: String, description: String, icon: ImageVector, color: Color, accent: Color, videoQuery: String) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(color)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accent)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                Text(text = description, fontSize = 12.sp, color = Slate600, lineHeight = 16.sp, modifier = Modifier.padding(top = 4.dp))
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        IconButton(
            onClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=${Uri.encode(videoQuery)}"))
                context.startActivity(intent)
            },
            modifier = Modifier.background(Color.White, RoundedCornerShape(12.dp))
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = "Watch", tint = accent)
        }
    }
}
