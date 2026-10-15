package com.example.presentation.nutrition

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.data.health.HealthGoalCalculator
import com.example.data.local.DailyMetric
import com.example.data.nutrition.NutritionView
import java.time.LocalDate
import com.example.presentation.viewmodel.ShasthoViewModel
import com.example.ui.components.NavListCard
import com.example.ui.theme.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutritionScreen(viewModel: ShasthoViewModel, navController: NavController) {
    val profile by viewModel.userProfile.collectAsState()
    val isDark = true

    val foodLogAccent = AccentTokens.foodLogAccent(isDark)
    val mealPlanAccent = AccentTokens.mealPlanAccent(isDark)
    val dietChartAccent = AccentTokens.dietChartAccent(isDark)
    val recipeAccent = AccentTokens.recipeAccent(isDark)

    val todayMetrics by viewModel.todayMetrics.collectAsState()
    val todayStr by viewModel.todayDate.collectAsState()
    val today = remember(todayStr) { try { LocalDate.parse(todayStr) } catch (e: Exception) { LocalDate.now() } }
    val weekHistoryFlow = remember { viewModel.getMetricsHistoryFlow(7) }
    val weekHistory by weekHistoryFlow.collectAsState(initial = emptyList())
    val weekDays = remember(weekHistory, today) {
        NutritionView.lastSevenDays(today, weekHistory.associate { it.date to it.caloriesConsumed })
    }

    val calorieGoal = profile?.let { if (it.dailyCalorieLimit > 0) it.dailyCalorieLimit else HealthGoalCalculator.calculateCalorieGoal(it) } ?: 2000
    val macroGoals = profile?.let { HealthGoalCalculator.calculateMacroGoals(it) }
        ?: HealthGoalCalculator.MacroGoals(
            carbsG = ((calorieGoal * 0.45f) / 4f).roundToInt(),
            proteinG = ((calorieGoal * 0.25f) / 4f).roundToInt(),
            fatG = ((calorieGoal * 0.30f) / 9f).roundToInt()
        )

    val consumed = todayMetrics?.caloriesConsumed ?: 0
    val todayCarbs = todayMetrics?.carbsG ?: 0f
    val todayProtein = todayMetrics?.proteinG ?: 0f
    val todayFat = todayMetrics?.fatG ?: 0f

    val isSyncing by viewModel.isSyncing.collectAsState()
    PullToRefreshBox(
        isRefreshing = isSyncing,
        onRefresh = { viewModel.syncWithHealthConnect(navController.context) },
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Nutrition & Diet", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)

            // today at a glance (tap: food log), the week, the calories against the target, the macros and their split
            IntakeHeroCard(consumed = consumed, goal = calorieGoal, onClick = { navController.navigate("foodlog") })
            WeeklyCaloriesCard(days = weekDays, goal = calorieGoal)
            DailyCaloriesCard(consumed = consumed, goal = calorieGoal)
            NutritionsCard(carbsG = todayCarbs, proteinG = todayProtein, fatG = todayFat, goals = macroGoals)
            NutritionInfoCard(goals = macroGoals, carbsG = todayCarbs, proteinG = todayProtein, fatG = todayFat)

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
            // keeps the last items clear of the assistant button
            Spacer(modifier = Modifier.height(88.dp))
        }
    }
}
