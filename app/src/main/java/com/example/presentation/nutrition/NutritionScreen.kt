package com.example.presentation.nutrition

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.presentation.viewmodel.ShasthoViewModel
import com.example.ui.theme.*

@Composable
fun NutritionScreen(viewModel: ShasthoViewModel, navController: NavController) {
    val profile by viewModel.userProfile.collectAsState()
    val isDark = profile?.isDarkMode ?: isSystemInDarkTheme()

    val foodLogAccent = AccentTokens.foodLogAccent(isDark)
    val mealPlanAccent = AccentTokens.mealPlanAccent(isDark)
    val dietChartAccent = AccentTokens.dietChartAccent(isDark)
    val recipeAccent = AccentTokens.recipeAccent(isDark)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Nutrition & Diet", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        
        NutritionCard(
            title = "Food Log & Scanner",
            subtitle = "Track your daily meals",
            icon = Icons.Default.Restaurant,
            accent = foodLogAccent,
            onClick = { navController.navigate("foodlog") }
        )
        
        NutritionCard(
            title = "Meal Plan",
            subtitle = "Your customized diet plan",
            icon = Icons.Default.RestaurantMenu,
            accent = mealPlanAccent,
            onClick = { navController.navigate("mealplan") }
        )
        
        NutritionCard(
            title = "Diet Chart",
            subtitle = "Weekly diet breakdown",
            icon = Icons.AutoMirrored.Filled.Assignment,
            accent = dietChartAccent,
            onClick = { navController.navigate("dietplan") }
        )
        
        NutritionCard(
            title = "Medicinal Recipes",
            subtitle = "Healthy recipes for your goals",
            icon = Icons.Default.LocalDining,
            accent = recipeAccent,
            onClick = { navController.navigate("recipe") }
        )
    }
}

@Composable
fun NutritionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: AccentColors,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(accent.bg)
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = title, tint = accent.onBg)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = accent.onBg)
                Text(text = subtitle, fontSize = 14.sp, color = accent.onBg)
            }
            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Go", tint = Slate400)
        }
    }
}
