package com.example.presentation.mindbody

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.presentation.viewmodel.ShasthoViewModel
import com.example.presentation.workout.ProtocolPracticeCard
import com.example.ui.theme.AccentTokens

/**
 * Mind & Body tab: the daily practices (HIIT, morning sun, breathwork and meditation).
 * These cards used to live at the bottom of the Fitness tab.
 */
@Composable
fun MindBodyScreen(viewModel: ShasthoViewModel) {
    val navController = com.example.LocalNavController.current
    val isDark = true

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Mind & Body",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 16.dp)
        )
        Text(
            text = "Daily practices for energy, sleep and calm.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
        )
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
        Spacer(modifier = Modifier.height(100.dp))
    }
}
