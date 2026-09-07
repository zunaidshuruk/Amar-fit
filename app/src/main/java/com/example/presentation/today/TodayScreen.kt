package com.example.presentation.today

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.presentation.navigation.navigateToTab
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
    val last7Metrics by viewModel.getMetricsHistoryFlow(7).collectAsState(initial = emptyList())
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
    val stepsProgress = (steps.toFloat() / 10000f).coerceIn(0f, 1f)
    val exerciseDays = remember(last7Metrics) { last7Metrics.count { it.exerciseMinutes > 0 } }
    val badges = profile?.badges?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
    val points = profile?.points ?: 0
    val currentStreak = profile?.currentStreak ?: 0

    val (activeLargeIds, activeSmallIds) = remember(profile?.todayTileSlots) {
        parseTodayTileSlots(profile?.todayTileSlots)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Large Ring Tiles (Full-width)
        if (activeLargeIds.isNotEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                activeLargeIds.forEach { largeId ->
                    val resolved = resolveLargeTile(
                        id = largeId,
                        metrics = metrics,
                        last7Metrics = last7Metrics,
                        isDark = isDark,
                        onOpenStepsDialog = { showStepsOptionDialog = true },
                        onNavigateToFitness = { navigateToTab(navController, "fitness") }
                    )
                    if (resolved != null) {
                        LargeRingTileCard(
                            tile = resolved,
                            isDark = isDark
                        )
                    }
                }
            }
        }

        // Small Metric Tiles (2-Column Grid)
        if (activeSmallIds.isNotEmpty()) {
            val smallChunked = activeSmallIds.chunked(2)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                smallChunked.forEach { rowIds ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowIds.forEach { smallId ->
                            val resolved = resolveSmallTile(
                                id = smallId,
                                metrics = metrics,
                                profile = profile,
                                last7Metrics = last7Metrics,
                                todayFoodLogs = todayFoodLogs,
                                isDark = isDark,
                                navController = navController,
                                onOpenStepsDialog = { showStepsOptionDialog = true },
                                onOpenWaterDialog = { showWaterDialog = true },
                                onNavigateToTab = { tab -> navigateToTab(navController, tab) }
                            )
                            if (resolved != null) {
                                TodayPillCard(
                                    modifier = Modifier.weight(1f),
                                    label = resolved.label,
                                    value = resolved.value,
                                    icon = resolved.icon,
                                    accent = resolved.accent,
                                    isMuted = resolved.isMuted,
                                    isDark = isDark,
                                    onClick = resolved.onClick
                                )
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                        if (rowIds.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        // Action Row (+ Log / Start / Edit Focus)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { showLogBottomSheet = true },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .testTag("today_log_button"),
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
                onClick = { navigateToTab(navController, "fitness") },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .testTag("today_start_button"),
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

            IconButton(
                onClick = { navController.navigate("edit_focus") },
                modifier = Modifier
                    .size(56.dp)
                    .shadow(2.dp, CircleShape)
                    .clip(CircleShape)
                    .background(if (isDark) Slate800 else Surface)
                    .testTag("today_edit_tiles_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit Focus",
                    tint = if (isDark) MaterialTheme.colorScheme.onSurface else TextPrimary,
                    modifier = Modifier.size(22.dp)
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
                onClick = { navigateToTab(navController, "fitness") }
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
                        navigateToTab(navController, "health")
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
                        navigateToTab(navController, "sleep")
                    }
                )
            }
        }
    }
}

@Composable
private fun TodayPillCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: AccentColors? = null,
    isMuted: Boolean = false,
    isDark: Boolean,
    onClick: (() -> Unit)? = null
) {
    val containerBg = when {
        isMuted -> if (isDark) Slate800.copy(alpha = 0.5f) else Slate100.copy(alpha = 0.8f)
        accent != null -> accent.bg
        else -> if (isDark) Slate800 else Surface
    }
    val contentTint = when {
        isMuted -> if (isDark) Slate500 else Slate400
        accent != null -> accent.onBg
        else -> if (isDark) MaterialTheme.colorScheme.onSurface else TextPrimary
    }

    Box(
        modifier = modifier
            .shadow(if (isMuted) 0.dp else 1.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(containerBg)
            .then(
                if (onClick != null && !isMuted) Modifier.clickable { onClick() }
                else Modifier
            )
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(
                        if (isMuted) (if (isDark) Slate600 else Slate200)
                        else if (accent != null) accent.onBg.copy(alpha = 0.15f)
                        else (if (isDark) Slate600 else Slate100)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentTint,
                    modifier = Modifier.size(16.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isDark) Slate400 else Slate500,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = value,
                    fontSize = 14.sp,
                    fontWeight = if (isMuted) FontWeight.Normal else FontWeight.Bold,
                    color = if (isMuted) (if (isDark) Slate500 else Slate400) else if (accent != null) accent.onBg else (if (isDark) MaterialTheme.colorScheme.onSurface else TextPrimary),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun LargeRingTileCard(
    tile: ResolvedLargeTile,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(if (isDark) Slate800 else Surface)
            .clickable { tile.onClick() }
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = tile.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) MaterialTheme.colorScheme.onSurface else TextPrimary,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .size(130.dp)
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 11.dp.toPx()
                    val diameter = size.minDimension - strokeWidth
                    val topLeftOffset = androidx.compose.ui.geometry.Offset(
                        (size.width - diameter) / 2f,
                        (size.height - diameter) / 2f
                    )
                    val arcSize = androidx.compose.ui.geometry.Size(diameter, diameter)

                    // Background Track
                    drawArc(
                        color = if (isDark) tile.accent.bg else tile.accent.onBg.copy(alpha = 0.15f),
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeftOffset,
                        size = arcSize,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = strokeWidth,
                            cap = StrokeCap.Round
                        )
                    )

                    // Progress Arc
                    if (tile.progress > 0f) {
                        drawArc(
                            color = tile.accent.onBg,
                            startAngle = -90f,
                            sweepAngle = 360f * tile.progress,
                            useCenter = false,
                            topLeft = topLeftOffset,
                            size = arcSize,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                width = strokeWidth,
                                cap = StrokeCap.Round
                            )
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = tile.insideValue,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) MaterialTheme.colorScheme.onSurface else TextPrimary
                    )
                    Text(
                        text = tile.insideSubtext,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isDark) Slate400 else Slate500
                    )
                }
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
