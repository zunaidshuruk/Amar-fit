package com.example.presentation.lifestyle

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.presentation.viewmodel.ShasthoViewModel
import com.example.ui.theme.*

data class LifestylePhase(val title: String, val duration: String, val objective: String, val rules: String)

val phases = listOf(
    LifestylePhase(
        "Phase I: Fat Adaptation", 
        "5 to 7 Days", 
        "Shift primary cellular fuel from glucose to dietary and stored fat. Suppress insulin spikes.", 
        "Strictly avoid ALL carbohydrates including grains (rice, wheat, corn), sugars, lentils (dal), and toxic seed oils (soybean, canola). Eat ONLY approved healthy fats (ghee, coconut oil, butter, mustard oil), organic eggs, wild-caught fish, grass-fed meat, and low-carb leafy greens (shak). Maintain macros: 70% Fat, 25% Protein, 5% Carbs. Eat only when genuinely hungry until full."
    ),
    LifestylePhase(
        "Phase II: Dry Fasting (Roza)", 
        "7 Consecutive Days", 
        "Upregulate fat-burning, induce rapid visceral fat reduction, initiate deep cellular autophagy.", 
        "Perform dawn-to-dusk dry fasting without food or water (similar to Islamic Roza). Break the fast at Iftar strictly with approved Phase I LCHF foods. Do not consume fried snacks or sugars. For Sehri, consume nutrient-dense fats (e.g., ghee-fried eggs, bulletproof coffee, or pink Himalayan salt water) to sustain energy without triggering insulin."
    ),
    LifestylePhase(
        "Phase III: Mixed Fasting & Healing", 
        "Indefinite / Until Goal Weight", 
        "Optimize mitochondrial health, maintain cellular self-cleaning, and maximize fat loss.", 
        "Alternate between different fasting protocols based on bodily feedback. Incorporate Dry Fasting (1-2 days/week), Water Fasting (23-hour OMAD window), and standard Intermittent Fasting (18/6 schedule). Continue strict adherence to LCHF dietary principles during feeding windows."
    ),
    LifestylePhase(
        "Phase IV: Maintenance", 
        "Lifetime", 
        "Introduce metabolic flexibility, preserve weight loss, and sustain hormonal health.", 
        "Slowly reintroduce very small portions of complex, low-glycemic carbohydrates: unpolished red rice, sour yogurt, and low-sugar seasonal fruits. Refined grains, processed sugar, and seed oils remain strictly prohibited for life. Continue daily physical activity and intermittent fasting."
    )
)

val corePillars = listOf(
    "Nutritional Discipline (LCHF)",
    "Restorative Fasting & Autophagy",
    "Restorative Sleep Hygiene",
    "Daily Physical Exercise",
    "Mental Stress Management"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LifestyleScreen(viewModel: ShasthoViewModel, onNavigateBack: () -> Unit = {}) {
    val profile by viewModel.userProfile.collectAsState()
    val isDark = profile?.isDarkMode ?: isSystemInDarkTheme()
    val pointsAccent = AccentTokens.pointsAccent(isDark)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp)
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                text = "Lifestyle Goals",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        Text(
            text = "The Lifestyle Modification protocol for metabolic reversal",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = pointsAccent.bg)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("The 5 Core Pillars", fontWeight = FontWeight.Bold, color = pointsAccent.onBg)
                        Spacer(modifier = Modifier.height(8.dp))
                        corePillars.forEach { pillar ->
                            Text("• $pillar", fontSize = 14.sp, color = pointsAccent.onBg, modifier = Modifier.padding(vertical = 2.dp))
                        }
                    }
                }
            }
            
            item {
                Text("Phase-by-Phase Protocol", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(top = 8.dp))
            }

            items(phases) { phase ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text(phase.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = if (isDark) MaterialTheme.colorScheme.onSurface else Emerald700)
                            Text(phase.duration, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Objective: ${phase.objective}", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(phase.rules, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 20.sp)
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(100.dp)) }
        }
    }
}
