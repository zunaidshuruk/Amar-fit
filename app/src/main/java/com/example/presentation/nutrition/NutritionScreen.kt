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
    val weekHistoryFlow = remember { viewModel.getMetricsHistoryFlow(7) }
    val weekHistory by weekHistoryFlow.collectAsState(initial = emptyList())
    val chronologicalWeek = remember(weekHistory) { weekHistory.reversed() }

    val calorieGoal = profile?.let { if (it.dailyCalorieLimit > 0) it.dailyCalorieLimit else HealthGoalCalculator.calculateCalorieGoal(it) } ?: 2000
    val macroGoals = profile?.let { HealthGoalCalculator.calculateMacroGoals(it) }
        ?: HealthGoalCalculator.MacroGoals(
            carbsG = ((calorieGoal * 0.45f) / 4f).roundToInt(),
            proteinG = ((calorieGoal * 0.25f) / 4f).roundToInt(),
            fatG = ((calorieGoal * 0.30f) / 9f).roundToInt()
        )

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

            // This Week's Calories
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        text = "This Week's Calories",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    if (chronologicalWeek.any { it.caloriesConsumed > 0 }) {
                        WeeklyCaloriesBarChart(
                            data = chronologicalWeek,
                            goalCalories = calorieGoal.toFloat(),
                            isDark = isDark
                        )
                    } else {
                        Text(
                            text = "No calories logged this week yet.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Today's Macros
            if (macroGoals != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(16.dp)
                ) {
                    Column {
                        Text(
                            text = "Today's Macros",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        MacroProgressRow("Carbs", todayCarbs, macroGoals.carbsG.toFloat(), MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(12.dp))
                        MacroProgressRow("Protein", todayProtein, macroGoals.proteinG.toFloat(), MaterialTheme.colorScheme.secondary)
                        Spacer(modifier = Modifier.height(12.dp))
                        MacroProgressRow("Fat", todayFat, macroGoals.fatG.toFloat(), MaterialTheme.colorScheme.tertiary)
                    }
                }

                // Macro Breakdown (by calorie contribution)
                val totalMacroCalories = (todayCarbs * 4f) + (todayProtein * 4f) + (todayFat * 9f)
                if (totalMacroCalories > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            MacroBreakdownPieChart(
                                carbsCal = todayCarbs * 4f,
                                proteinCal = todayProtein * 4f,
                                fatCal = todayFat * 9f
                            )
                            Spacer(modifier = Modifier.width(20.dp))
                            Column {
                                Text("Macro Breakdown", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                                Spacer(modifier = Modifier.height(8.dp))
                                MacroLegendRow("Carbs", MaterialTheme.colorScheme.primary, ((todayCarbs * 4f / totalMacroCalories) * 100).roundToInt())
                                MacroLegendRow("Protein", MaterialTheme.colorScheme.secondary, ((todayProtein * 4f / totalMacroCalories) * 100).roundToInt())
                                MacroLegendRow("Fat", MaterialTheme.colorScheme.tertiary, ((todayFat * 9f / totalMacroCalories) * 100).roundToInt())
                            }
                        }
                    }
                }
            }

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
}

@Composable
private fun WeeklyCaloriesBarChart(data: List<DailyMetric>, goalCalories: Float?, isDark: Boolean) {
    val goalLineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
    val barColor = MaterialTheme.colorScheme.primary
    val mutedColor = MaterialTheme.colorScheme.onSurfaceVariant

    BoxWithConstraints(modifier = Modifier.fillMaxWidth().height(180.dp)) {
        val totalWidth = maxWidth
        val contentWidth: Dp = if (data.size > 10) (data.size * 32).dp.coerceAtLeast(totalWidth) else totalWidth

        Box(
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(rememberScrollState())
        ) {
            Canvas(modifier = Modifier.width(contentWidth).fillMaxHeight().padding(vertical = 12.dp)) {
                val dataMax = (data.maxOfOrNull { it.caloriesConsumed } ?: 2000).toFloat().coerceAtLeast(1f)
                val maxVal = if (goalCalories != null && goalCalories > dataMax) goalCalories else dataMax

                val count = data.size
                val width = size.width
                val height = size.height
                val barWidth = (width / (count * 1.5f)).coerceIn(8.dp.toPx(), 32.dp.toPx())
                val spacing = if (count > 1) (width - (count * barWidth)) / (count - 1) else 0f

                data.forEachIndexed { index, metric ->
                    val x = if (count > 1) index * (barWidth + spacing) else (width - barWidth) / 2f
                    if (metric.caloriesConsumed > 0) {
                        val ratio = (metric.caloriesConsumed / maxVal).coerceIn(0f, 1f)
                        val barHeight = (ratio * (height - 8.dp.toPx())).coerceAtLeast(4.dp.toPx())
                        val y = height - barHeight
                        drawRoundRect(
                            color = barColor,
                            topLeft = Offset(x, y),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )
                    } else {
                        drawRoundRect(
                            color = mutedColor.copy(alpha = 0.35f),
                            topLeft = Offset(x, height - 8.dp.toPx()),
                            size = Size(barWidth, 8.dp.toPx()),
                            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                        )
                    }
                }

                if (goalCalories != null && goalCalories > 0f) {
                    val ratio = (goalCalories / maxVal).coerceIn(0f, 1f)
                    val lineY = height - (ratio * (height - 8.dp.toPx()))
                    drawLine(
                        color = goalLineColor,
                        start = Offset(0f, lineY),
                        end = Offset(width, lineY),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                    )
                }
            }
        }
    }
}

@Composable
private fun MacroProgressRow(label: String, actualG: Float, goalG: Float, color: Color) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                "${actualG.roundToInt()}g / ${goalG.roundToInt()}g",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { if (goalG > 0f) (actualG / goalG).coerceIn(0f, 1f) else 0f },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
            color = color,
            trackColor = MaterialTheme.colorScheme.background,
        )
    }
}

@Composable
private fun MacroBreakdownPieChart(carbsCal: Float, proteinCal: Float, fatCal: Float) {
    val carbsColor = MaterialTheme.colorScheme.primary
    val proteinColor = MaterialTheme.colorScheme.secondary
    val fatColor = MaterialTheme.colorScheme.tertiary
    val total = (carbsCal + proteinCal + fatCal).coerceAtLeast(1f)

    Canvas(modifier = Modifier.size(96.dp)) {
        var startAngle = -90f
        val sweepCarbs = (carbsCal / total) * 360f
        val sweepProtein = (proteinCal / total) * 360f
        val sweepFat = (fatCal / total) * 360f

        drawArc(color = carbsColor, startAngle = startAngle, sweepAngle = sweepCarbs, useCenter = true)
        startAngle += sweepCarbs
        drawArc(color = proteinColor, startAngle = startAngle, sweepAngle = sweepProtein, useCenter = true)
        startAngle += sweepProtein
        drawArc(color = fatColor, startAngle = startAngle, sweepAngle = sweepFat, useCenter = true)
    }
}

@Composable
private fun MacroLegendRow(label: String, color: Color, percent: Int) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text("$label $percent%", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
