package com.example.presentation.today

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(viewModel: ShasthoViewModel, navController: NavController) {
    val profile by viewModel.userProfile.collectAsState()
    val isDark = profile?.isDarkMode ?: isSystemInDarkTheme()

    val streakAccent = AccentTokens.streakAccent(isDark = isDark)
    val pointsAccent = AccentTokens.pointsAccent(isDark = isDark)
    val badgesAccent = AccentTokens.badgesAccent(isDark = isDark)
    val caloriesAccent = AccentTokens.caloriesAccent(isDark = isDark)
    val stepsAccent = AccentTokens.stepsAccent(isDark = isDark)
    val waterAccent = AccentTokens.waterAccent(isDark = isDark)
    val weightAccent = AccentTokens.weightAccent(isDark = isDark)
    val glucoseAccent = AccentTokens.glucoseAccent(isDark = isDark)
    val bloodPressureAccent = AccentTokens.bloodPressureAccent(isDark = isDark)
    val sleepAccent = AccentTokens.sleepAccent(isDark = isDark)

    val metrics by viewModel.todayMetrics.collectAsState()
    val todayFoodLogs by viewModel.todayFoodLogs.collectAsState()
    val todayActivityEvents by viewModel.todayActivityEvents.collectAsState()
    
    var showStepsDialog by remember { mutableStateOf(false) }
    var showStepsOptionDialog by remember { mutableStateOf(false) }
    var showWaterDialog by remember { mutableStateOf(false) }
    var showLogBottomSheet by remember { mutableStateOf(false) }

    val calorieLimit = profile?.dailyCalorieLimit ?: 2000
    val totalCalories = todayFoodLogs.sumOf { it.calories }
    val calorieProgress = if (calorieLimit > 0) (totalCalories.toFloat() / calorieLimit.toFloat()).coerceIn(0f, 1f) else 0f
    val waterLimit = profile?.dailyWaterLimitLiters ?: 3.0f
    val waterConsumed = metrics?.waterLiters ?: 0f
    val steps = metrics?.steps ?: 0
    val badges = profile?.badges?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
    val points = profile?.points ?: 0
    val currentStreak = profile?.currentStreak ?: 0

    val pagerState = rememberPagerState(pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 3-Page Swipeable Stat Carousel
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
            pageSpacing = 12.dp
        ) { page ->
            when (page) {
                0 -> {
                    // Page 1 ("Today"): Streak / Points / Badges Hero Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(148.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .shadow(2.dp, RoundedCornerShape(20.dp))
                                .clip(RoundedCornerShape(20.dp))
                                .background(streakAccent.bg)
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.LocalFireDepartment, contentDescription = "Streak", tint = streakAccent.onBg)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(text = "$currentStreak Days", fontWeight = FontWeight.Bold, color = streakAccent.onBg, fontSize = 14.sp)
                                Text(text = "Streak", fontSize = 10.sp, color = TextPrimary)
                            }
                        }
                        
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .shadow(2.dp, RoundedCornerShape(20.dp))
                                .clip(RoundedCornerShape(20.dp))
                                .background(pointsAccent.bg)
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Star, contentDescription = "Points", tint = pointsAccent.onBg)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(text = "$points", fontWeight = FontWeight.Bold, color = pointsAccent.onBg, fontSize = 14.sp)
                                Text(text = "Points", fontSize = 10.sp, color = TextPrimary)
                            }
                        }
                        
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .shadow(2.dp, RoundedCornerShape(20.dp))
                                .clip(RoundedCornerShape(20.dp))
                                .background(badgesAccent.bg)
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.EmojiEvents, contentDescription = "Badges", tint = badgesAccent.onBg)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(text = "${badges.size}", fontWeight = FontWeight.Bold, color = badgesAccent.onBg, fontSize = 14.sp)
                                Text(text = "Badges", fontSize = 10.sp, color = TextPrimary)
                            }
                        }
                    }
                }
                1 -> {
                    // Page 2 ("Activity"): Steps & Water Tiles
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(148.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 2. Steps Tile
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .shadow(2.dp, RoundedCornerShape(20.dp))
                                .clip(RoundedCornerShape(20.dp))
                                .background(stepsAccent.bg)
                                .clickable { showStepsOptionDialog = true }
                                .padding(20.dp)
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.AutoMirrored.Filled.DirectionsWalk, contentDescription = null, tint = stepsAccent.onBg, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = "Steps", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(text = "$steps", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = stepsAccent.onBg)
                            }
                        }
                        
                        // 3. Water Tile
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .shadow(2.dp, RoundedCornerShape(20.dp))
                                .clip(RoundedCornerShape(20.dp))
                                .background(waterAccent.bg)
                                .clickable { showWaterDialog = true }
                                .padding(20.dp)
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.LocalDrink, contentDescription = null, tint = waterAccent.onBg, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = "Water", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(text = String.format("%.1f", waterConsumed), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = waterAccent.onBg)
                                    Text(text = " L", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = waterAccent.onBg, modifier = Modifier.padding(bottom = 4.dp))
                                }
                            }
                        }
                    }
                }
                2 -> {
                    // Page 3 ("Nutrition"): Calories Tile
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(148.dp)
                            .shadow(2.dp, RoundedCornerShape(20.dp))
                            .clip(RoundedCornerShape(20.dp))
                            .background(caloriesAccent.bg)
                            .clickable { navController.navigate("nutrition") }
                            .padding(20.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = caloriesAccent.onBg, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Calories", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(text = "$totalCalories", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = caloriesAccent.onBg)
                                Text(text = " / $calorieLimit kcal", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = caloriesAccent.onBg, modifier = Modifier.padding(bottom = 4.dp))
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
                }
            }
        }

        // Page Indicator Dots
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 0.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(3) { pageIndex ->
                val isSelected = pagerState.currentPage == pageIndex
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .height(8.dp)
                        .width(if (isSelected) 24.dp else 8.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant
                        )
                        .clickable {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pageIndex)
                            }
                        }
                )
            }
        }

        // Action Row (+ Log / Start)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { showLogBottomSheet = true },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDark) Emerald700 else Primary,
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Log",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Button(
                onClick = { navController.navigate("fitness") },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDark) Emerald800 else Emerald600,
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Start",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
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

        // Today's Activity Timeline Feed
        Text(
            text = "Today's activity",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        if (todayActivityEvents.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(1.dp, RoundedCornerShape(20.dp))
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No activities logged yet today",
                    color = Slate500,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
                todayActivityEvents.forEach { event ->
                    val (icon, accent) = when (event.type) {
                        "food" -> Pair(Icons.Default.RestaurantMenu, AccentTokens.caloriesAccent(isDark))
                        "water" -> Pair(Icons.Default.LocalDrink, AccentTokens.waterAccent(isDark))
                        "weight" -> Pair(Icons.Default.MonitorWeight, AccentTokens.weightAccent(isDark))
                        "glucose" -> Pair(Icons.Default.Favorite, AccentTokens.glucoseAccent(isDark))
                        "blood_pressure" -> Pair(Icons.Default.MonitorHeart, AccentTokens.bloodPressureAccent(isDark))
                        "sleep" -> Pair(Icons.Default.Bedtime, AccentTokens.sleepAccent(isDark))
                        "workout" -> Pair(Icons.Default.FitnessCenter, AccentTokens.stepsAccent(isDark))
                        "diet_chart" -> Pair(Icons.Default.MenuBook, AccentTokens.pointsAccent(isDark))
                        else -> Pair(Icons.Default.CheckCircle, AccentTokens.badgesAccent(isDark))
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(2.dp, RoundedCornerShape(20.dp))
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(accent.bg),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = event.type,
                                    tint = accent.onBg,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = event.description,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = timeFormat.format(Date(event.timestamp)),
                                    fontSize = 12.sp,
                                    color = Slate500
                                )
                            }
                        }
                    }
                }
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

    if (showLogBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showLogBottomSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = if (isDark) MaterialTheme.colorScheme.surface else Surface,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            dragHandle = {
                BottomSheetDefaults.DragHandle(
                    color = if (isDark) MaterialTheme.colorScheme.outlineVariant else Slate200
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 36.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Log Health Activity",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) MaterialTheme.colorScheme.onSurface else TextPrimary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LogActionRowItem(
                    title = "Water",
                    subtitle = "Log hydration intake",
                    icon = Icons.Default.LocalDrink,
                    accent = waterAccent,
                    isDark = isDark,
                    onClick = {
                        showLogBottomSheet = false
                        showWaterDialog = true
                    }
                )

                LogActionRowItem(
                    title = "Weight",
                    subtitle = "Record body weight & height",
                    icon = Icons.Default.MonitorWeight,
                    accent = weightAccent,
                    isDark = isDark,
                    onClick = {
                        showLogBottomSheet = false
                        navController.navigate("weightlog")
                    }
                )

                LogActionRowItem(
                    title = "Blood Glucose",
                    subtitle = "Morning fasting or post-meal reading",
                    icon = Icons.Default.Favorite,
                    accent = glucoseAccent,
                    isDark = isDark,
                    onClick = {
                        showLogBottomSheet = false
                        navController.navigate("glucoselog")
                    }
                )

                LogActionRowItem(
                    title = "Blood Pressure",
                    subtitle = "Log systolic & diastolic measurement",
                    icon = Icons.Default.MonitorHeart,
                    accent = bloodPressureAccent,
                    isDark = isDark,
                    onClick = {
                        showLogBottomSheet = false
                        navController.navigate("health")
                    }
                )

                LogActionRowItem(
                    title = "Sleep",
                    subtitle = "Track last night's sleep hours",
                    icon = Icons.Default.Bedtime,
                    accent = sleepAccent,
                    isDark = isDark,
                    onClick = {
                        showLogBottomSheet = false
                        navController.navigate("sleep")
                    }
                )
            }
        }
    }
}

@Composable
private fun LogActionRowItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: AccentColors,
    isDark: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f) else Slate100.copy(alpha = 0.7f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(accent.bg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accent.onBg,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = if (isDark) MaterialTheme.colorScheme.onSurface else TextPrimary
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Slate500
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Slate400,
                modifier = Modifier.size(20.dp)
            )
        }
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
