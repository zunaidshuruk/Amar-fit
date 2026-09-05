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
                    .shadow(2.dp, RoundedCornerShape(20.dp))
                    .clip(RoundedCornerShape(20.dp))
                    .background(Orange50)
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.LocalFireDepartment, contentDescription = "Streak", tint = Orange700)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "$currentStreak Days", fontWeight = FontWeight.Bold, color = Orange700, fontSize = 14.sp)
                    Text(text = "Streak", fontSize = 10.sp, color = TextPrimary)
                }
            }
            
            Box(
                modifier = Modifier
                    .weight(1f)
                    .shadow(2.dp, RoundedCornerShape(20.dp))
                    .clip(RoundedCornerShape(20.dp))
                    .background(Indigo50)
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Star, contentDescription = "Points", tint = Indigo700)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "$points", fontWeight = FontWeight.Bold, color = Indigo700, fontSize = 14.sp)
                    Text(text = "Points", fontSize = 10.sp, color = TextPrimary)
                }
            }
            
            Box(
                modifier = Modifier
                    .weight(1f)
                    .shadow(2.dp, RoundedCornerShape(20.dp))
                    .clip(RoundedCornerShape(20.dp))
                    .background(Emerald50)
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.EmojiEvents, contentDescription = "Badges", tint = Emerald700)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "${badges.size}", fontWeight = FontWeight.Bold, color = Emerald700, fontSize = 14.sp)
                    Text(text = "Badges", fontSize = 10.sp, color = TextPrimary)
                }
            }
        }

        // Glanceable Stats: Google Health Style 
        // 1. Calories Tile
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(2.dp, RoundedCornerShape(20.dp))
                .clip(RoundedCornerShape(20.dp))
                .background(Orange50)
                .clickable { navController.navigate("nutrition") }
                .padding(20.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = Orange700, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Calories", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(text = "$totalCalories", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = Orange700)
                    Text(text = " / $calorieLimit kcal", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Orange700, modifier = Modifier.padding(bottom = 6.dp))
                }
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { calorieProgress },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                    color = Orange500,
                    trackColor = Orange100,
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 2. Steps Tile
            Box(
                modifier = Modifier
                    .weight(1f)
                    .shadow(2.dp, RoundedCornerShape(20.dp))
                    .clip(RoundedCornerShape(20.dp))
                    .background(Emerald50)
                    .clickable { showStepsOptionDialog = true }
                    .padding(20.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DirectionsWalk, contentDescription = null, tint = Emerald700, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Steps", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "$steps", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Emerald700)
                }
            }
            
            // 3. Water Tile
            Box(
                modifier = Modifier
                    .weight(1f)
                    .shadow(2.dp, RoundedCornerShape(20.dp))
                    .clip(RoundedCornerShape(20.dp))
                    .background(BlueBg)
                    .clickable { showWaterDialog = true }
                    .padding(20.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocalDrink, contentDescription = null, tint = Blue700, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Water", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(text = String.format("%.1f", waterConsumed), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Blue700)
                        Text(text = " L", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Blue700, modifier = Modifier.padding(bottom = 4.dp))
                    }
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
                bgColor = Surface,
                iconColor = Primary,
                textColor = TextPrimary,
                onClick = { navController.navigate("scanner") }
            )
            QuickActionCard(
                modifier = Modifier.weight(1f),
                title = "Log Workout",
                icon = Icons.Default.FitnessCenter,
                bgColor = Surface,
                iconColor = Primary,
                textColor = TextPrimary,
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
                bgColor = Surface,
                iconColor = Primary,
                textColor = TextPrimary,
                onClick = { navController.navigate("dietplan") }
            )
            QuickActionCard(
                modifier = Modifier.weight(1f),
                title = "Water Log",
                icon = Icons.Default.LocalDrink,
                bgColor = Surface,
                iconColor = Primary,
                textColor = TextPrimary,
                onClick = { showWaterDialog = true }
            )
        }

        // AI Chat / Coach Entry Point
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(2.dp, RoundedCornerShape(20.dp))
                .clip(RoundedCornerShape(20.dp))
                .background(Surface)
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
                            .background(Emerald50),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.ChatBubbleOutline, contentDescription = "AI Assistant", tint = Primary)
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
            .shadow(2.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .then(if (borderColor != Color.Transparent) Modifier.border(1.dp, borderColor, RoundedCornerShape(20.dp)) else Modifier)
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
