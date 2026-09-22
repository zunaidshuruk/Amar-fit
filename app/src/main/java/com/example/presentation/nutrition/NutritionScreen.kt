package com.example.presentation.nutrition

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.presentation.viewmodel.ShasthoViewModel
import com.example.ui.components.NavListCard
import com.example.ui.theme.*

@Composable
fun NutritionScreen(viewModel: ShasthoViewModel, navController: NavController) {
    val profile by viewModel.userProfile.collectAsState()
    val isDark = true

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

        NavListCard(
            icon = Icons.Default.Restaurant,
            title = "Food Log & Scanner",
            subtitle = "Track your daily meals",
            accent = foodLogAccent,
            onClick = { navController.navigate("foodlog") }
        )

        NavListCard(
            icon = Icons.Default.RestaurantMenu,
            title = "Meal Plan",
            subtitle = "Your customized diet plan",
            accent = mealPlanAccent,
            onClick = { navController.navigate("mealplan") }
        )

        NavListCard(
            icon = Icons.AutoMirrored.Filled.Assignment,
            title = "Diet Chart",
            subtitle = "Weekly diet breakdown",
            accent = dietChartAccent,
            onClick = { navController.navigate("dietplan") }
        )

        NavListCard(
            icon = Icons.Default.LocalDining,
            title = "Medicinal Recipes",
            subtitle = "Healthy recipes for your goals",
            accent = recipeAccent,
            onClick = { navController.navigate("recipe") }
        )
    }
}
