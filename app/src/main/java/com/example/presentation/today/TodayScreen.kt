package com.example.presentation.today

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.presentation.viewmodel.ShasthoViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun TodayScreen(viewModel: ShasthoViewModel, navController: NavController) {
    val profile by viewModel.userProfile.collectAsState()
    val metrics by viewModel.todayMetrics.collectAsState()
    val todayFoodLogs by viewModel.todayFoodLogs.collectAsState()
    
    var showStepsDialog by remember { mutableStateOf(false) }
    var showStepsOptionDialog by remember { mutableStateOf(false) }
    var showWaterDialog by remember { mutableStateOf(false) }

    val calorieLimit = profile?.dailyCalorieLimit ?: 2000
    val totalCalories = todayFoodLogs.sumOf { it.calories }
    val calorieProgress = if (calorieLimit > 0) (totalCalories.toFloat() / calorieLimit.toFloat()).coerceIn(0f, 1f) else 0f
    val waterLimit = profile?.dailyWaterLimitLiters ?: 3.0f
    val waterConsumed = metrics?.waterLiters ?: 0f
    val steps = metrics?.steps ?: 0
    val badges = profile?.badges?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
    val points = profile?.points ?: 0
    val currentStreak = profile?.currentStreak ?: 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero: Gamification Points, Badges, & Streak Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(OrangeBg)
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.LocalFireDepartment, contentDescription = "Streak", tint = Orange500)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "$currentStreak Days", fontWeight = FontWeight.Bold, color = Orange900, fontSize = 14.sp)
                    Text(text = "Streak", fontSize = 10.sp, color = Orange700)
                }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(IndigoBg)
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Star, contentDescription = "Points", tint = Indigo600)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "$points", fontWeight = FontWeight.Bold, color = Indigo900, fontSize = 14.sp)
                    Text(text = "Points", fontSize = 10.sp, color = Indigo700)
                }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Emerald50)
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.EmojiEvents, contentDescription = "Badges", tint = Emerald500)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "${badges.size}", fontWeight = FontWeight.Bold, color = Emerald900, fontSize = 14.sp)
                    Text(text = "Badges", fontSize = 10.sp, color = Emerald700)
                }
            }
        }

        // Glanceable Stats: Calories, Water, Steps
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White)
                .border(1.dp, Slate200, RoundedCornerShape(24.dp))
                .clickable { navController.navigate("nutrition") } // Or just keep it as stat
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("CALORIES", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Emerald700)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(text = "$totalCalories", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text(text = " / $calorieLimit kcal", fontSize = 14.sp, color = Slate500, modifier = Modifier.padding(bottom = 6.dp))
                    }
                }
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(72.dp)) {
                    CircularProgressIndicator(
                        progress = { 1f },
                        modifier = Modifier.fillMaxSize(),
                        color = Emerald50,
                        strokeWidth = 8.dp,
                    )
                    CircularProgressIndicator(
                        progress = { calorieProgress },
                        modifier = Modifier.fillMaxSize(),
                        color = Emerald500,
                        strokeWidth = 8.dp,
                        strokeCap = StrokeCap.Round
                    )
                    val pct = (calorieProgress * 100).toInt()
                    Text(text = "$pct%", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Emerald700)
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(BlueBg)
                    .clickable { showWaterDialog = true }
                    .padding(16.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Blue500))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "WATER", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Blue700)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(text = String.format("%.1f", waterConsumed), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Blue900)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Ltrs", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Blue900, modifier = Modifier.padding(bottom = 2.dp))
                    }
                }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(OrangeBg)
                    .clickable { showStepsOptionDialog = true }
                    .padding(16.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Orange500))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "STEPS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Orange700)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "$steps", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Orange900)
                }
            }
        }

        // Quick Actions 2x2 Grid
        Text("Quick Actions", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionCard(
                modifier = Modifier.weight(1f),
                title = "Scan Food",
                icon = Icons.Default.AddAPhoto,
                bgColor = Slate900,
                iconColor = Emerald500,
                textColor = Color.White,
                onClick = { navController.navigate("scanner") }
            )
            QuickActionCard(
                modifier = Modifier.weight(1f),
                title = "Log Workout",
                icon = Icons.Default.FitnessCenter,
                bgColor = Color.White,
                iconColor = Orange700,
                textColor = TextPrimary,
                borderColor = Slate200,
                onClick = { navController.navigate("fitness") }
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionCard(
                modifier = Modifier.weight(1f),
                title = "Diet Chart",
                icon = Icons.Default.RestaurantMenu,
                bgColor = Emerald900,
                iconColor = Color.White,
                textColor = Color.White,
                onClick = { navController.navigate("dietplan") }
            )
            QuickActionCard(
                modifier = Modifier.weight(1f),
                title = "Water Log",
                icon = Icons.Default.LocalDrink,
                bgColor = BlueBg,
                iconColor = Blue700,
                textColor = Blue900,
                onClick = { showWaterDialog = true }
            )
        }

        // AI Chat / Coach Entry Point
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White)
                .border(1.dp, Emerald200, RoundedCornerShape(24.dp))
                .clickable { navController.navigate("chat") }
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Emerald100),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.ChatBubbleOutline, contentDescription = "AI Assistant", tint = Emerald700)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Ask AI Assistant", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(text = "Get instant diet & health answers", color = Slate500, fontSize = 14.sp)
                    }
                }
                Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Go", tint = Slate400)
            }
        }
    }

    // Dialogs
    if (showWaterDialog) {
        var waterInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showWaterDialog = false },
            title = { Text("Log Water") },
            text = {
                OutlinedTextField(
                    value = waterInput,
                    onValueChange = { waterInput = it },
                    label = { Text("Water (Liters)") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val w = waterInput.toFloatOrNull()
                    if (w != null) {
                        viewModel.addWater(w)
                    }
                    showWaterDialog = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showWaterDialog = false }) { Text("Cancel") }
            }
        )
    }
    
    if (showStepsOptionDialog) {
        AlertDialog(
            onDismissRequest = { showStepsOptionDialog = false },
            title = { Text("Log Steps") },
            text = { Text("How would you like to update your step count today?") },
            confirmButton = {
                TextButton(onClick = {
                    showStepsOptionDialog = false
                    android.widget.Toast.makeText(navController.context, "Syncing steps via Health Connect...", android.widget.Toast.LENGTH_SHORT).show()
                    viewModel.syncWithHealthConnect(navController.context)
                }) { Text("Sync Device Steps") }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showStepsOptionDialog = false
                    showStepsDialog = true 
                }) { Text("Enter Manually") }
            }
        )
    }

    if (showStepsDialog) {
        var stepsInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showStepsDialog = false },
            title = { Text("Log Steps Manually") },
            text = {
                OutlinedTextField(
                    value = stepsInput,
                    onValueChange = { stepsInput = it },
                    label = { Text("Steps walked") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val s = stepsInput.toIntOrNull()
                    if (s != null) {
                        viewModel.addSteps(s)
                    }
                    showStepsDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showStepsDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun QuickActionCard(
    modifier: Modifier,
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    bgColor: Color,
    iconColor: Color,
    textColor: Color,
    borderColor: Color = Color.Transparent,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .then(if (borderColor != Color.Transparent) Modifier.border(1.dp, borderColor, RoundedCornerShape(16.dp)) else Modifier)
            .clickable { onClick() }
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(imageVector = icon, contentDescription = title, tint = iconColor, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = title, fontWeight = FontWeight.Medium, color = textColor, fontSize = 14.sp)
        }
    }
}
