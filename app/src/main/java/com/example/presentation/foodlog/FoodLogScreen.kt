package com.example.presentation.foodlog

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.presentation.viewmodel.ShasthoViewModel
import com.example.ui.theme.*
import com.example.ui.components.MarkdownText
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Delete

@Composable
fun FoodLogScreen(viewModel: ShasthoViewModel, onNavigateToScanner: () -> Unit = {}) {
    val todayFoodLogs by viewModel.todayFoodLogs.collectAsState()
    val allFoodLogs by viewModel.allFoodLogs.collectAsState()
    val profile by viewModel.userProfile.collectAsState()
    
    val calorieLimit = profile?.dailyCalorieLimit ?: 2000
    val totalCalories = todayFoodLogs.sumOf { it.calories }
    val progressRatio = (totalCalories.toFloat() / calorieLimit.toFloat()).coerceIn(0f, 1f)
    
    var animationPlayed by remember { mutableStateOf(false) }
    var showManualEntry by remember { mutableStateOf(false) }
    var manualText by remember { mutableStateOf("") }
    var manualMealType by remember { mutableStateOf("Snack") }
    
    var showEditDialog by remember { mutableStateOf(false) }
    var editingLog by remember { mutableStateOf<com.example.data.local.FoodLog?>(null) }
    var editName by remember { mutableStateOf("") }
    var editCategory by remember { mutableStateOf("") }
    var editCalories by remember { mutableStateOf("") }
    var editTime by remember { mutableStateOf("") }
    var editMealType by remember { mutableStateOf("") }

        val isScanning by viewModel.isScanning.collectAsState()
    val weeklyInsights by viewModel.weeklyInsights.collectAsState()
    val isLoadingInsights by viewModel.isLoadingInsights.collectAsState()
    val animatedProgress by animateFloatAsState(
        targetValue = if (animationPlayed) progressRatio else 0f,
        animationSpec = tween(durationMillis = 1000),
        label = "progress"
    )
    
    LaunchedEffect(progressRatio) {
        animationPlayed = true
    }

    
    if (showEditDialog && editingLog != null) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Food Entry") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Food Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editCategory,
                        onValueChange = { editCategory = it },
                        label = { Text("Category") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editCalories,
                        onValueChange = { editCalories = it },
                        label = { Text("Calories") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editTime,
                        onValueChange = { editTime = it },
                        label = { Text("Time (HH:mm)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editMealType,
                        onValueChange = { editMealType = it },
                        label = { Text("Meal Type") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val updatedLog = editingLog!!.copy(
                            name = editName,
                            category = editCategory,
                            calories = editCalories.toIntOrNull() ?: editingLog!!.calories,
                            time = editTime,
                            mealType = editMealType
                        )
                        viewModel.updateFoodLog(updatedLog)
                        showEditDialog = false
                    }
                ) {
                    Text("Save", color = Emerald600)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel", color = Slate500)
                }
            }
        )
    }

    if (showManualEntry) {
        AlertDialog(
            onDismissRequest = { showManualEntry = false },
            title = { Text("Manual Food Entry") },
            text = {
                Column {
                    Text("What did you eat?", color = Slate500, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = manualText,
                        onValueChange = { manualText = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("e.g. 2 slices of bread and an egg") },
                        enabled = !isScanning
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Meal Type", color = Slate500, fontSize = 14.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Breakfast", "Lunch", "Dinner", "Snack").forEach { type ->
                            FilterChip(
                                selected = manualMealType == type,
                                onClick = { manualMealType = type },
                                label = { Text(type, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Emerald100,
                                    selectedLabelColor = Emerald700
                                )
                            )
                        }
                    }
                    if (isScanning) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Emerald500)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("Analyzing...", color = Slate500)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (manualText.isNotBlank()) {
                            viewModel.analyzeFoodText(manualText)
                            manualText = ""
                            showManualEntry = false
                        }
                    },
                    enabled = !isScanning && manualText.isNotBlank()
                ) {
                    Text("Save", color = Emerald600)
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualEntry = false }, enabled = !isScanning) {
                    Text("Cancel", color = Slate500)
                }
            }
        )
    }

    Scaffold(
                floatingActionButton = {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                ExtendedFloatingActionButton(
                    onClick = { showManualEntry = true },
                    containerColor = Emerald500,
                    contentColor = Color.White,
                    icon = { Icon(Icons.Default.Edit, contentDescription = "Type Food") },
                    text = { Text("Manual Entry") }
                )
                ExtendedFloatingActionButton(
                    onClick = onNavigateToScanner,
                    containerColor = Emerald600,
                    contentColor = Color.White,
                    icon = { Icon(Icons.Default.AddAPhoto, contentDescription = "Scan Photo") },
                    text = { Text("Scan Photo") }
                )
            }
        },
        containerColor = Background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
        Text(
            text = "Food Log",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            modifier = Modifier.padding(top = 16.dp, bottom = 24.dp)
        )
        
        // Calorie Progress Chart
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier.size(180.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = { 1f },
                    modifier = Modifier.fillMaxSize(),
                    color = Slate100,
                    strokeWidth = 14.dp
                )
                CircularProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.fillMaxSize(),
                    color = if (totalCalories > calorieLimit) Orange700 else Emerald500,
                    strokeWidth = 14.dp,
                    strokeCap = StrokeCap.Round
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$totalCalories",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "/ $calorieLimit kcal",
                        fontSize = 14.sp,
                        color = Slate500,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (totalCalories > calorieLimit) "Over Limit" else "${calorieLimit - totalCalories} left",
                        fontSize = 12.sp,
                        color = if (totalCalories > calorieLimit) Orange700 else Emerald600,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        
        // Weekly Nutritional Trends Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Analytics, contentDescription = null, tint = Emerald600)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Weekly Nutritional Trends", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
                }
                Spacer(modifier = Modifier.height(12.dp))
                
                if (weeklyInsights != null) {
                    MarkdownText(text = weeklyInsights!!, color = Slate600, fontSize = 14.sp)
                } else if (isLoadingInsights) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Emerald500, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Analyzing 7-day logs...", color = Slate500, fontSize = 14.sp)
                    }
                } else {
                    Text("Discover your macro-nutrient trends and potential deficiencies based on your recent food logs.", color = Slate500, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { viewModel.fetchWeeklyInsights() },
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald100, contentColor = Emerald700),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.AutoGraph, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Generate AI Insights", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        if (allFoodLogs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "No food logs yet.", color = Slate400)
            }
        } else {
            val groupedLogs = allFoodLogs.groupBy { it.date }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 140.dp)) {
                groupedLogs.forEach { (date, logs) ->
                    item {
                        Text(
                            text = date,
                            fontWeight = FontWeight.Bold,
                            color = Slate600,
                            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                        )
                    }
                    items(logs) { log ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White)
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Emerald100),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Restaurant, contentDescription = null, tint = Emerald600)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = log.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                                Text(text = "${log.mealType} • ${log.time}", fontSize = 12.sp, color = Emerald600, fontWeight = FontWeight.Medium)
                                Text(text = log.category, fontSize = 12.sp, color = Slate400)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${log.calories} kcal",
                                    fontWeight = FontWeight.Bold,
                                    color = Emerald600
                                )
                                Row {
                                    IconButton(onClick = {
                                        editingLog = log
                                        editName = log.name
                                        editCategory = log.category
                                        editCalories = log.calories.toString()
                                        editTime = log.time
                                        editMealType = log.mealType
                                        showEditDialog = true
                                    }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Emerald600.copy(alpha = 0.8f))
                                    }
                                    IconButton(onClick = { viewModel.deleteFoodLog(log) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.6f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    }
}
