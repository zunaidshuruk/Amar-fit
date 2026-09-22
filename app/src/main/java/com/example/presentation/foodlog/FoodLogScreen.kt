package com.example.presentation.foodlog

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.AddAPhoto
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.presentation.viewmodel.ShasthoViewModel
import com.example.ui.theme.*
import com.example.ui.components.MarkdownText
import com.example.ui.components.MealTypeSelector
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.automirrored.filled.ArrowBack

@Composable
fun FoodLogScreen(viewModel: ShasthoViewModel, onNavigateToScanner: () -> Unit = {}, onNavigateBack: () -> Unit = {}) {
    val todayFoodLogs by viewModel.todayFoodLogs.collectAsState()
    val allFoodLogs by viewModel.allFoodLogs.collectAsState()
    val frequentFoods by viewModel.frequentFoods.collectAsState()
    val context = LocalContext.current
    val profile by viewModel.userProfile.collectAsState()
    val isDark = true
    val foodLogAccent = AccentTokens.foodLogAccent(isDark)
    
    val calorieLimit = profile?.dailyCalorieLimit ?: 2000
    val totalCalories = todayFoodLogs.sumOf { it.calories }
    val progressRatio = (totalCalories.toFloat() / calorieLimit.toFloat()).coerceIn(0f, 1f)
    
    var animationPlayed by remember { mutableStateOf(false) }
    var showManualEntry by remember { mutableStateOf(false) }
    var manualEntryMode by remember { mutableStateOf("Describe") } // "Describe" or "Enter Values"
    var manualText by remember { mutableStateOf("") }
    var manualMealType by remember { mutableStateOf("Snack") }
    var manualFoodName by remember { mutableStateOf("") }
    var manualCalories by remember { mutableStateOf("") }
    var manualCarbs by remember { mutableStateOf("") }
    var manualProtein by remember { mutableStateOf("") }
    var manualFat by remember { mutableStateOf("") }
    
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
                    Text("Save", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    if (showManualEntry) {
        val isEnterValuesValid = manualFoodName.isNotBlank() && (manualCalories.toIntOrNull() ?: -1) >= 0
        val isDescribeValid = manualText.isNotBlank()
        val isSaveEnabled = if (manualEntryMode == "Enter Values") isEnterValuesValid else (!isScanning && isDescribeValid)

        AlertDialog(
            onDismissRequest = {
                showManualEntry = false
                manualEntryMode = "Describe"
                manualText = ""
                manualFoodName = ""
                manualCalories = ""
                manualCarbs = ""
                manualProtein = ""
                manualFat = ""
                manualMealType = "Snack"
            },
            title = { Text("Manual Food Entry") },
            text = {
                Column {
                    // Segmented 2-option toggle: "Describe" vs "Enter Values"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(if (isDark) Slate800 else Slate100)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            onClick = { manualEntryMode = "Describe" },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(20.dp),
                            color = if (manualEntryMode == "Describe") MaterialTheme.colorScheme.primary else Color.Transparent,
                            contentColor = if (manualEntryMode == "Describe") MaterialTheme.colorScheme.onPrimary else (if (isDark) Slate300 else Slate700)
                        ) {
                            Text(
                                text = "Describe",
                                modifier = Modifier.padding(vertical = 8.dp),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                        Surface(
                            onClick = { manualEntryMode = "Enter Values" },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(20.dp),
                            color = if (manualEntryMode == "Enter Values") MaterialTheme.colorScheme.primary else Color.Transparent,
                            contentColor = if (manualEntryMode == "Enter Values") MaterialTheme.colorScheme.onPrimary else (if (isDark) Slate300 else Slate700)
                        ) {
                            Text(
                                text = "Enter Values",
                                modifier = Modifier.padding(vertical = 8.dp),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (manualEntryMode == "Describe") {
                        Text("What did you eat?", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = manualText,
                            onValueChange = { manualText = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("e.g. 2 slices of bread and an egg") },
                            enabled = !isScanning
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Meal Type", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        MealTypeSelector(
                            selectedMealType = manualMealType,
                            onMealTypeSelected = { manualMealType = it }
                        )
                        if (isScanning) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(16.dp))
                                Text("Analyzing...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else {
                        // Enter Values mode (Direct macro entry, no AI)
                        OutlinedTextField(
                            value = manualFoodName,
                            onValueChange = { manualFoodName = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Food Name *") },
                            placeholder = { Text("e.g. Boiled Rice") },
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = manualCalories,
                            onValueChange = { manualCalories = it.filter { char -> char.isDigit() } },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Calories (kcal) *") },
                            placeholder = { Text("e.g. 250") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = manualCarbs,
                                onValueChange = { manualCarbs = it.filter { char -> char.isDigit() || char == '.' } },
                                modifier = Modifier.weight(1f),
                                label = { Text("Carbs (g)") },
                                placeholder = { Text("0") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = manualProtein,
                                onValueChange = { manualProtein = it.filter { char -> char.isDigit() || char == '.' } },
                                modifier = Modifier.weight(1f),
                                label = { Text("Protein (g)") },
                                placeholder = { Text("0") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = manualFat,
                                onValueChange = { manualFat = it.filter { char -> char.isDigit() || char == '.' } },
                                modifier = Modifier.weight(1f),
                                label = { Text("Fat (g)") },
                                placeholder = { Text("0") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Meal Type", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        MealTypeSelector(
                            selectedMealType = manualMealType,
                            onMealTypeSelected = { manualMealType = it }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (manualEntryMode == "Enter Values") {
                            val cal = manualCalories.toIntOrNull() ?: 0
                            val carbs = manualCarbs.toFloatOrNull() ?: 0f
                            val protein = manualProtein.toFloatOrNull() ?: 0f
                            val fat = manualFat.toFloatOrNull() ?: 0f
                            viewModel.logScannedFood(
                                name = manualFoodName.trim(),
                                category = "Manual",
                                calories = cal,
                                description = manualFoodName.trim(),
                                mealType = manualMealType,
                                carbsG = carbs,
                                proteinG = protein,
                                fatG = fat
                            )
                            manualEntryMode = "Describe"
                            manualFoodName = ""
                            manualCalories = ""
                            manualCarbs = ""
                            manualProtein = ""
                            manualFat = ""
                            manualText = ""
                            manualMealType = "Snack"
                            showManualEntry = false
                        } else {
                            if (manualText.isNotBlank()) {
                                viewModel.analyzeFoodText(manualText, manualMealType)
                                manualEntryMode = "Describe"
                                manualText = ""
                                manualFoodName = ""
                                manualCalories = ""
                                manualCarbs = ""
                                manualProtein = ""
                                manualFat = ""
                                manualMealType = "Snack"
                                showManualEntry = false
                            }
                        }
                    },
                    enabled = isSaveEnabled
                ) {
                    Text("Save", color = if (isSaveEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showManualEntry = false
                        manualEntryMode = "Describe"
                        manualText = ""
                        manualFoodName = ""
                        manualCalories = ""
                        manualCarbs = ""
                        manualProtein = ""
                        manualFat = ""
                        manualMealType = "Snack"
                    },
                    enabled = !isScanning
                ) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    Scaffold(
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                ExtendedFloatingActionButton(
                    onClick = { showManualEntry = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    icon = { Icon(Icons.Default.Edit, contentDescription = "Type Food") },
                    text = { Text("Manual Entry") }
                )
                ExtendedFloatingActionButton(
                    onClick = onNavigateToScanner,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    icon = { Icon(Icons.Default.AddAPhoto, contentDescription = "Scan Photo") },
                    text = { Text("Scan Photo") }
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 140.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 24.dp)
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Text(
                        text = "Food Log",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        
            // Calorie Progress Chart
            item {
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
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            strokeWidth = 14.dp
                        )
                        CircularProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier.fillMaxSize(),
                            color = if (totalCalories > calorieLimit) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                            strokeWidth = 14.dp,
                            strokeCap = StrokeCap.Round
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$totalCalories",
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "/ $calorieLimit kcal",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (totalCalories > calorieLimit) "Over Limit" else "${calorieLimit - totalCalories} left",
                                fontSize = 12.sp,
                                color = if (totalCalories > calorieLimit) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

        
            // Weekly Nutritional Trends Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Analytics, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Weekly Nutritional Trends", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        if (weeklyInsights != null) {
                            MarkdownText(text = weeklyInsights!!, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                        } else if (isLoadingInsights) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.primary, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Analyzing 7-day logs...", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                            }
                        } else {
                            Text("Discover your macro-nutrient trends and potential deficiencies based on your recent food logs.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { viewModel.fetchWeeklyInsights() },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.primary),
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
            }

            if (frequentFoods.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.padding(bottom = 24.dp)) {
                        Text(
                            text = "Recent Foods",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(frequentFoods) { food ->
                                Column(
                                    modifier = Modifier
                                        .width(120.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(MaterialTheme.colorScheme.surface)
                                        .clickable {
                                            viewModel.relogFood(food)
                                            android.widget.Toast.makeText(
                                                context,
                                                "Logged ${food.name}",
                                                android.widget.Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                        .padding(12.dp)
                                ) {
                                    Icon(Icons.Default.Restaurant, contentDescription = null, tint = foodLogAccent.onBg)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = food.name,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${food.calories} kcal",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (allFoodLogs.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        Text(text = "No food logs yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                val groupedLogs = allFoodLogs.groupBy { it.date }
                groupedLogs.forEach { (date, logs) ->
                    item {
                        Text(
                            text = date,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                        )
                    }
                    items(logs) { log ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(foodLogAccent.bg),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Restaurant, contentDescription = null, tint = foodLogAccent.onBg)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = log.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                                Text(text = "${log.mealType} • ${log.time}", fontSize = 12.sp, color = foodLogAccent.onBg, fontWeight = FontWeight.Medium)
                                Text(text = log.category, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${log.calories} kcal",
                                    fontWeight = FontWeight.Bold,
                                    color = foodLogAccent.onBg
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
                                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = foodLogAccent.onBg.copy(alpha = 0.8f))
                                    }
                                    IconButton(onClick = { viewModel.deleteFoodLog(log) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
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
