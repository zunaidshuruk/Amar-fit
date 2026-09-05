package com.example.presentation.workout

import android.content.Intent
import android.net.Uri
import android.text.util.Linkify
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.util.LinkifyCompat
import com.example.presentation.viewmodel.ShasthoViewModel
import com.example.ui.theme.*

@Composable
fun WorkoutScreen(viewModel: ShasthoViewModel) {
    val workoutPlan by viewModel.workoutPlan.collectAsState()
    val isLoading by viewModel.isLoadingWorkout.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "AI Workouts",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
        )
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
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                shape = RoundedCornerShape(24.dp)
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
        
        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
fun ProtocolPracticeCard(title: String, description: String, icon: ImageVector, color: Color, accent: Color, videoQuery: String) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
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
