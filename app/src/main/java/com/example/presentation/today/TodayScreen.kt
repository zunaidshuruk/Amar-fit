package com.example.presentation.today

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.data.health.HealthConnectManager
import com.example.data.local.WorkoutProgram
import com.example.data.model.ALL_BADGES
import com.example.data.model.ProgramDaysJson
import com.example.presentation.navigation.navigateToTab
import com.example.presentation.viewmodel.ShasthoViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TodayScreen(viewModel: ShasthoViewModel, navController: NavController) {
    val profile by viewModel.userProfile.collectAsState()
    val isDark = true

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

    LaunchedEffect(Unit) {
        while (true) {
            delay(10_000L)
            if (HealthConnectManager.hasAnyPermissions(navController.context)) {
                viewModel.syncWithHealthConnect(navController.context, showRefreshIndicator = false)
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.startHealthHistoryAutoImport(navController.context)
    }

    var showWaterDialog by remember { mutableStateOf(false) }
    var showLogBottomSheet by remember { mutableStateOf(false) }

    var selectedDate by remember { mutableStateOf(java.time.LocalDate.now()) }
    val selectedDateString = remember(selectedDate) { selectedDate.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE) }

    val metrics by viewModel.getMetricsForDateFlow(selectedDateString).collectAsState(initial = null)
    val last7Metrics by viewModel.getMetricsHistoryFlow(7).collectAsState(initial = emptyList())
    val healthInsight by viewModel.healthInsight.collectAsState()
    val isLoadingHealthInsight by viewModel.isLoadingHealthInsight.collectAsState()
    val todayFoodLogs by viewModel.getFoodLogsForDateFlow(selectedDateString).collectAsState(initial = emptyList())
    val todayActivityEvents by viewModel.getActivityEventsForDateFlow(selectedDateString).collectAsState(initial = emptyList())
    val programs by viewModel.workoutPrograms.collectAsState()
    val activeProgram = remember(programs) {
        programs.filter { ProgramDaysJson.parse(it.daysJson).isNotEmpty() }
            .maxWithOrNull(compareBy<WorkoutProgram>({ it.lastCompletedAt }, { it.createdAt }))
    }

    val calorieLimit = profile?.dailyCalorieLimit ?: 2000
    val totalCalories = todayFoodLogs.sumOf { it.calories }
    val calorieProgress = if (calorieLimit > 0) (totalCalories.toFloat() / calorieLimit.toFloat()).coerceIn(0f, 1f) else 0f
    val waterLimit = profile?.dailyWaterLimitLiters ?: 3.0f
    val waterConsumed = metrics?.waterLiters ?: 0f
    val steps = metrics?.steps ?: 0
    val stepGoal = profile?.stepGoal?.takeIf { it > 0 } ?: 10000
    val stepsProgress = (steps.toFloat() / stepGoal.toFloat()).coerceIn(0f, 1f)
    val exerciseDays = remember(last7Metrics) { last7Metrics.count { it.exerciseMinutes > 0 } }
    val badges = profile?.badges?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
    val points = profile?.points ?: 0
    val currentStreak = profile?.currentStreak ?: 0

    val (activeLargeIds, activeSmallIds) = remember(profile?.todayTileSlots) {
        parseTodayTileSlots(profile?.todayTileSlots)
    }

    val isSyncing by viewModel.isSyncing.collectAsState()
    val updateInfo by viewModel.updateInfo.collectAsState()
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
            updateInfo?.let { update ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("update_banner_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SystemUpdate,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "New Version Available (${update.versionName})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            IconButton(onClick = { viewModel.dismissUpdateBanner() }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (update.releaseNotes.isNotBlank()) {
                            Text(
                                text = update.releaseNotes,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        val context = androidx.compose.ui.platform.LocalContext.current
                        Button(
                            onClick = {
                                com.example.data.repository.UpdateChecker.downloadAndInstallApk(context, update)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Download & Install")
                        }
                    }
                }
            }
        // Large Ring Tiles (Full-width, stacked vertically). Each card is flippable in
        // place -- tap or swipe it to reveal a back face with extra detail.
        val burnedToday by produceState<Pair<Int, Int>?>(initialValue = null, key1 = selectedDate, key2 = metrics?.activeCaloriesBurned) {
            value = if (selectedDate == java.time.LocalDate.now()) viewModel.getCaloriesBurnedForDate(selectedDate) else null
        }
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
                        stepGoal = stepGoal,
                        onOpenStepsDialog = { navController.navigate("metric_detail/steps") },
                        onNavigateToFitness = { navigateToTab(navController, "fitness") },
                        totalCalories = totalCalories,
                        calorieLimit = calorieLimit,
                        calorieProgress = calorieProgress,
                        onNavigateToNutrition = { navigateToTab(navController, "nutrition") },
                        activeBurned = burnedToday?.first ?: 0,
                        totalBurned = burnedToday?.second ?: 0
                    )
                    if (resolved != null) {
                        FlippableHeroCard(
                            tile = resolved,
                            isDark = isDark,
                            heroStyle = true
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
                smallChunked.forEachIndexed { rowIndex, rowIds ->
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
                                onOpenStepsDialog = { navController.navigate("metric_detail/steps") },
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
                                    featured = (rowIndex == 0),
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

        CalendarStripCard(
            selectedDate = selectedDate,
            onDateSelected = { selectedDate = it }
        )

        if (selectedDate == java.time.LocalDate.now() && activeProgram != null) {
            TodayWorkoutCard(
                program = activeProgram,
                onStart = { dayIndex ->
                    viewModel.requestProgramDayStart(activeProgram.cloudId, dayIndex)
                    navigateToTab(navController, "fitness")
                },
                onMarkRestDone = { dayIndex ->
                    viewModel.completeProgramDay(activeProgram.cloudId, dayIndex)
                }
            )
        }

        // Streak, Points & Badges Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Top row: Streak & Points side-by-side
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(streakAccent.bg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = null,
                                tint = streakAccent.onBg,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = if (currentStreak > 0) "$currentStreak day streak" else "No streak yet",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(pointsAccent.bg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = pointsAccent.onBg,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = "${String.format(Locale.US, "%,d", points)} points",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Badges sub-section
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { navController.navigate("badges") },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Badges",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "View All",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDark) MaterialTheme.colorScheme.primary else Emerald600
                            )
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "View All Badges",
                                tint = if (isDark) MaterialTheme.colorScheme.primary else Emerald600,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Text(
                        text = if (badges.isEmpty()) "Keep logging to earn your first badge!" else "${badges.size} badge${if (badges.size == 1) "" else "s"} earned",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(imageVector = Icons.Default.Insights, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Text(text = "Today's Insight", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
                if (healthInsight != null) {
                    Text(text = healthInsight ?: "", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 20.sp)
                    Text(
                        text = "This is an AI-generated wellness summary, not a medical diagnosis. Consult a healthcare professional for any health concerns.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        lineHeight = 14.sp
                    )
                    TextButton(
                        onClick = { viewModel.checkAndGenerateHealthInsight(navController.context) },
                        enabled = !isLoadingHealthInsight
                    ) {
                        Text(if (isLoadingHealthInsight) "Refreshing..." else "Refresh")
                    }
                } else if (isLoadingHealthInsight) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Text("Generating your insight...", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    Text("Get a personalized wellness summary based on your recent activity.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(
                        onClick = { viewModel.checkAndGenerateHealthInsight(navController.context) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Get Today's Insight")
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
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDark) MaterialTheme.colorScheme.primary else Primary,
                    contentColor = if (isDark) MaterialTheme.colorScheme.onPrimary else Color.White
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
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }

            Button(
                onClick = { navigateToTab(navController, "fitness") },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .testTag("today_start_button"),
                shape = RoundedCornerShape(28.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDark) MaterialTheme.colorScheme.secondary else Emerald600,
                    contentColor = if (isDark) MaterialTheme.colorScheme.onSecondary else Color.White
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
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }

            Surface(
                onClick = { navController.navigate("edit_focus") },
                modifier = Modifier
                    .height(56.dp)
                    .testTag("today_edit_tiles_button"),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Focus",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Edit tiles",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = timeFormat.format(Date(event.timestamp)),
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (event.source == "health_connect") {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                        ) {
                                            Text(
                                                text = "From Health Connect",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
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

    // Dialogs
    if (showWaterDialog) {
        var waterInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showWaterDialog = false },
            title = {
                Text(
                    text = "Log Water",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Quick add",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val quickOptions = listOf(
                            Triple("Glass", "+0.25L", 0.25f),
                            Triple("Bottle", "+0.5L", 0.5f),
                            Triple("Large Bottle", "+1.0L", 1.0f)
                        )
                        quickOptions.forEach { (name, label, amount) ->
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        viewModel.addWater(amount) { success ->
                                            if (!success) {
                                                android.widget.Toast.makeText(
                                                    navController.context,
                                                    "Water logged (didn't sync to Health Connect)",
                                                    android.widget.Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                        showWaterDialog = false
                                    },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isDark) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Emerald50,
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else Emerald200)
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = name,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        maxLines = 1
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = label,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) MaterialTheme.colorScheme.primary else Emerald700
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    Text(
                        text = "Other amount",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = waterInput,
                            onValueChange = { waterInput = it },
                            label = { Text("Water (Liters)") },
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                            ),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Button(
                            onClick = {
                                val w = waterInput.toFloatOrNull()
                                if (w != null && w > 0f) {
                                    viewModel.addWater(w) { success ->
                                        if (!success) {
                                            android.widget.Toast.makeText(
                                                navController.context,
                                                "Water logged (didn't sync to Health Connect)",
                                                android.widget.Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                    showWaterDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = if (isDark) MaterialTheme.colorScheme.primary else Emerald600),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(56.dp)
                        ) {
                            Text("Add")
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showWaterDialog = false }) { Text("Cancel") }
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
    featured: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val containerBg = when {
        isMuted -> if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else Slate100.copy(alpha = 0.8f)
        accent != null -> accent.bg
        else -> if (isDark) MaterialTheme.colorScheme.surfaceVariant else Surface
    }
    val contentTint = when {
        isMuted -> if (isDark) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else Slate400
        accent != null -> accent.onBg
        else -> if (isDark) MaterialTheme.colorScheme.onSurface else TextPrimary
    }

    val cardShape = if (featured) RoundedCornerShape(18.dp) else RoundedCornerShape(16.dp)

    Box(
        modifier = modifier
            .shadow(if (isMuted) 0.dp else 1.dp, cardShape)
            .clip(cardShape)
            .background(containerBg)
            .then(
                if (onClick != null && !isMuted) Modifier.clickable { onClick() }
                else Modifier
            )
            .padding(
                horizontal = if (featured) 14.dp else 12.dp,
                vertical = if (featured) 14.dp else 10.dp
            )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(if (featured) 38.dp else 32.dp)
                    .clip(CircleShape)
                    .background(
                        if (isMuted) (if (isDark) MaterialTheme.colorScheme.surface else Slate200)
                        else if (accent != null) accent.onBg.copy(alpha = 0.15f)
                        else (if (isDark) MaterialTheme.colorScheme.surface else Slate100)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentTint,
                    modifier = Modifier.size(if (featured) 19.dp else 16.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    fontSize = if (featured) 12.sp else 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Slate500,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = value,
                    fontSize = if (featured) 16.sp else 14.sp,
                    fontWeight = if (isMuted) FontWeight.Normal else FontWeight.Bold,
                    color = if (isMuted) (if (isDark) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else Slate400) else if (accent != null) accent.onBg else (if (isDark) MaterialTheme.colorScheme.onSurface else TextPrimary),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun CalendarStripCard(
    selectedDate: java.time.LocalDate,
    onDateSelected: (java.time.LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    var weekAnchor by remember(selectedDate) { mutableStateOf(selectedDate) }
    val weekStart = remember(weekAnchor) { weekAnchor.minusDays(weekAnchor.dayOfWeek.value % 7L) }
    val today = remember { java.time.LocalDate.now() }
    val monthFormatter = remember { java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US) }
    val dayLetterFormatter = remember { java.time.format.DateTimeFormatter.ofPattern("EEEEE", Locale.US) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = weekStart.format(monthFormatter),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    if (selectedDate != today) {
                        TextButton(onClick = { onDateSelected(today) }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
                            Text("Today", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    // No filled circle behind these — at small sizes the two backgrounds
                    // touched/overlapped. Plain tinted chevrons read clearly on their own.
                    IconButton(
                        onClick = { weekAnchor = weekAnchor.minusDays(7) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Previous week", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }
                    IconButton(
                        onClick = { weekAnchor = weekAnchor.plusDays(7) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Next week", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                for (i in 0..6) {
                    val day = weekStart.plusDays(i.toLong())
                    val isSelected = day == selectedDate
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onDateSelected(day) }
                            .padding(vertical = 4.dp, horizontal = 2.dp)
                    ) {
                        Text(
                            text = day.format(dayLetterFormatter),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = day.dayOfMonth.toString(),
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * A large hero tile that flips between a front face (the ring/progress view) and a
 * back face (a short list of related detail stats). It flips on either a tap or a
 * horizontal swipe, so the gesture that feels natural just works. The card's original
 * navigation/log action (e.g. opening the steps dialog) moves to a small corner
 * button so it stays reachable without hijacking the flip gesture.
 */
@Composable
private fun FlippableHeroCard(
    tile: ResolvedLargeTile,
    isDark: Boolean,
    heroStyle: Boolean = false,
    modifier: Modifier = Modifier
) {
    var flipped by remember(tile.id) { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (flipped) 180f else 0f,
        animationSpec = tween(durationMillis = 450),
        label = "heroCardFlip"
    )
    val density = LocalDensity.current
    var dragAccumPx by remember(tile.id) { mutableStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(if (heroStyle) MaterialTheme.colorScheme.primary else (if (isDark) MaterialTheme.colorScheme.surfaceVariant else Surface))
            // Swipe (in either direction) toggles the flip, same as a tap.
            .pointerInput(tile.id) {
                detectHorizontalDragGestures(
                    onDragStart = { dragAccumPx = 0f },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        dragAccumPx += dragAmount
                    },
                    onDragEnd = {
                        val thresholdPx = with(density) { 56.dp.toPx() }
                        if (abs(dragAccumPx) > thresholdPx) {
                            flipped = !flipped
                        }
                        dragAccumPx = 0f
                    },
                    onDragCancel = { dragAccumPx = 0f }
                )
            }
            .clickable { flipped = !flipped }
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density.density
            }
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        if (rotation <= 90f) {
            HeroCardFrontFace(tile = tile, isDark = isDark, heroStyle = heroStyle)
        } else {
            // Counter-rotate so the back face's text reads correctly instead of mirrored.
            Box(modifier = Modifier.graphicsLayer { rotationY = 180f }) {
                HeroCardBackFace(tile = tile, isDark = isDark, heroStyle = heroStyle)
            }
        }

        val isStepsTile = tile.id == "large_steps"
        IconButton(
            onClick = { tile.onClick() },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(32.dp)
        ) {
            Icon(
                imageVector = if (isStepsTile) Icons.Default.Add else Icons.Default.ChevronRight,
                contentDescription = if (isStepsTile) "Log steps" else "Open",
                tint = if (heroStyle) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f) else tile.accent.onBg,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun HeroCardFrontFace(
    tile: ResolvedLargeTile,
    isDark: Boolean,
    heroStyle: Boolean
) {
    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary
    val ringBox: @Composable () -> Unit = {
        Box(
            modifier = Modifier
                .size(if (heroStyle) 148.dp else 136.dp)
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 11.dp.toPx()
                if (heroStyle) {
                    val radius = (size.minDimension - strokeWidth) / 2f
                    val centerOffset = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
                    val filledDots = (32 * tile.progress).toInt().coerceIn(0, 32)
                    for (i in 0 until 32) {
                        val angleDeg = -90f + i * (360f / 32f)
                        val angleRad = Math.toRadians(angleDeg.toDouble())
                        val dotCenter = androidx.compose.ui.geometry.Offset(
                            x = (centerOffset.x + radius * cos(angleRad)).toFloat(),
                            y = (centerOffset.y + radius * sin(angleRad)).toFloat()
                        )
                        val isFilled = i < filledDots
                        val dotColor = if (isFilled) onPrimaryColor else onPrimaryColor.copy(alpha = 0.22f)
                        val dotRadius = if (isFilled) strokeWidth / 2.8f else strokeWidth / 3.2f
                        drawCircle(
                            color = dotColor,
                            radius = dotRadius,
                            center = dotCenter
                        )
                    }
                } else {
                    val diameter = size.minDimension - strokeWidth
                    val topLeftOffset = androidx.compose.ui.geometry.Offset(
                        (size.width - diameter) / 2f,
                        (size.height - diameter) / 2f
                    )
                    val arcSize = androidx.compose.ui.geometry.Size(diameter, diameter)

                    // Background Track (Open gauge 270 degrees, gap centered at the top so it doesn't collide with the title text)
                    drawArc(
                        color = tile.accent.onBg.copy(alpha = 0.15f),
                        startAngle = 315f,
                        sweepAngle = 270f,
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
                            startAngle = 315f,
                            sweepAngle = 270f * tile.progress,
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
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = tile.title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (heroStyle) MaterialTheme.colorScheme.onPrimary else tile.accent.onBg
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = tile.insideValue,
                    fontSize = if (heroStyle) 30.sp else 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (heroStyle) MaterialTheme.colorScheme.onPrimary else (if (isDark) MaterialTheme.colorScheme.onSurface else TextPrimary)
                )
                Text(
                    text = tile.insideSubtext,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (heroStyle) MaterialTheme.colorScheme.onPrimary else tile.accent.onBg
                )
            }
        }
    }

    if (tile.frontStats.isEmpty()) {
        ringBox()
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ringBox()
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tile.frontStats.forEach { (label, value) ->
                    Column {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f)
                        )
                        Text(
                            text = value,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroCardBackFace(
    tile: ResolvedLargeTile,
    isDark: Boolean,
    heroStyle: Boolean
) {
    val labelColor = if (heroStyle) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant
    val valueColor = if (heroStyle) MaterialTheme.colorScheme.onPrimary else (if (isDark) MaterialTheme.colorScheme.onSurface else TextPrimary)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = if (heroStyle) 148.dp else 136.dp)
            .padding(vertical = 4.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "${tile.title} detail",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = labelColor
            )
            if (tile.backStats.isEmpty()) {
                Text(
                    text = "No extra detail yet",
                    fontSize = 13.sp,
                    color = labelColor
                )
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(0.8f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    tile.backStats.forEach { (label, value) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = labelColor,
                                maxLines = 1
                            )
                            Text(
                                text = value,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = valueColor,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
            Text(
                text = "Tap to flip back",
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = labelColor.copy(alpha = 0.7f)
            )
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

@Composable
private fun TodayWorkoutCard(
    program: WorkoutProgram,
    onStart: (Int) -> Unit,
    onMarkRestDone: (Int) -> Unit
) {
    val days = remember(program.daysJson) { ProgramDaysJson.parse(program.daysJson) }
    if (days.isEmpty()) return
    val nextIndex = program.nextDayIndex.coerceIn(0, days.lastIndex)
    val day = days[nextIndex]
    val label = day.label.ifBlank { day.plan?.title ?: "Rest day" }
    val startOfToday = remember {
        java.time.LocalDate.now().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
    val doneToday = program.lastCompletedAt >= startOfToday

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.FitnessCenter,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Today's workout",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = program.title,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 140.dp)
                )
            }
            if (doneToday) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Done for today",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Next up: Day ${nextIndex + 1} — $label",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Day ${nextIndex + 1} of ${days.size}: $label",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                val isRest = day.isRestDay || day.plan == null
                val subtitle = if (isRest) {
                    "Rest day"
                } else {
                    val plan = day.plan!!
                    val count = plan.warmup.size + plan.mainExercises.size + plan.cooldown.size
                    if (plan.rounds > 1) {
                        "$count exercises • ${plan.rounds} rounds"
                    } else {
                        "$count exercises"
                    }
                }
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        if (isRest) onMarkRestDone(nextIndex) else onStart(nextIndex)
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(if (isRest) "Mark rest day done" else "Start workout")
                }
            }
        }
    }
}
