package com.example.presentation.sleep

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.presentation.common.appSafeInsets
import com.example.presentation.viewmodel.ShasthoViewModel
import com.example.ui.components.NavListCard
import com.example.ui.theme.AccentColors
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepScreen(viewModel: ShasthoViewModel, navController: NavController) {
    val profile by viewModel.userProfile.collectAsState()
    val sleepGoal = profile?.sleepGoalHours?.takeIf { it > 0f } ?: 8f

    val metrics by viewModel.todayMetrics.collectAsState()
    val sleepHours = metrics?.sleepHours ?: 0f
    var showSleepDialog by remember { mutableStateOf(false) }

    val todayStr by viewModel.todayDate.collectAsState()
    val today = remember(todayStr) {
        try { LocalDate.parse(todayStr) } catch (e: Exception) { LocalDate.now() }
    }
    val sleepHistoryFlow = remember(todayStr) { viewModel.getMetricsHistoryFlow(7) }
    val sleepHistory by sleepHistoryFlow.collectAsState(initial = emptyList())
    val weekDays = remember(sleepHistory, today, sleepHours) {
        val historyMap = sleepHistory.associate { it.date to it.sleepHours }
        (6 downTo 0).map { back ->
            val date = today.minusDays(back.toLong())
            val hours = if (date == today && sleepHours > 0f) sleepHours else (historyMap[date.toString()] ?: 0f)
            date to hours
        }
    }

    val isSyncing by viewModel.isSyncing.collectAsState()
    PullToRefreshBox(
        isRefreshing = isSyncing,
        onRefresh = { viewModel.syncWithHealthConnect(navController.context) },
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(appSafeInsets())
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Sleep & Wellness",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            // 1. Hero card at the top: lime card with ring and status
            SleepHeroCard(
                sleepHours = sleepHours,
                goal = sleepGoal,
                onClick = { showSleepDialog = true }
            )

            // 2. This week card: 7 days as bars, today preselected, tap a bar to inspect
            SleepWeekCard(
                days = weekDays,
                goal = sleepGoal
            )

            // 3. Sleep info card: weekly average, best night, goal reached
            SleepInfoCard(
                days = weekDays,
                goal = sleepGoal
            )

            // AI Wellness Coach Module
            NavListCard(
                icon = Icons.Default.MonitorHeart,
                title = "AI Wellness Coach",
                subtitle = "Optimize your lifestyle",
                accent = AccentColors(bg = MaterialTheme.colorScheme.surfaceVariant, onBg = MaterialTheme.colorScheme.primary),
                onClick = {
                    navController.navigate(
                        "universal_assistant?starter=" + Uri.encode(
                            "Act as my wellness coach. Look at my recent sleep, activity and nutrition and give me 3 specific things to improve this week."
                        )
                    )
                }
            )

            Spacer(modifier = Modifier.height(88.dp))
        }
    }

    if (showSleepDialog) {
        var sleepInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showSleepDialog = false },
            title = {
                Text(
                    text = "Log Sleep",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Quick select",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val quickOptions = listOf(6, 7, 8, 9)
                        quickOptions.forEach { hours ->
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        viewModel.setSleep(hours.toFloat())
                                        showSleepDialog = false
                                    },
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp, horizontal = 2.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$hours Hours",
                                        fontSize = 13.sp,
                                        letterSpacing = 0.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
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
                            value = sleepInput,
                            onValueChange = { sleepInput = it },
                            label = { Text("Sleep (Hours)") },
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                            ),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        Button(
                            onClick = {
                                val s = sleepInput.toFloatOrNull()
                                if (s != null && s > 0f) {
                                    viewModel.setSleep(s)
                                    showSleepDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(56.dp)
                        ) {
                            Text("Set")
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showSleepDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.primary)
                }
            }
        )
    }
}

/** The hero card: last night's sleep hours as a big number with a ring and goal status. Tapping it opens the log sleep dialog. */
@Composable
private fun SleepHeroCard(
    sleepHours: Float,
    goal: Float,
    onClick: () -> Unit
) {
    val onHero = MaterialTheme.colorScheme.onPrimary
    val percent = if (goal > 0f) (sleepHours / goal * 100f).coerceIn(0f, 100f) else 0f
    val statusText = when {
        goal <= 0f -> if (sleepHours > 0f) "Logged today" else "No goal set"
        sleepHours >= goal -> "Goal reached"
        else -> {
            val diff = goal - sleepHours
            String.format(Locale.US, "%.1f hrs to your goal", diff)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.primary)
            .clickable(onClick = onClick)
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(onHero.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Bedtime,
                            contentDescription = null,
                            tint = onHero,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        "Last night",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = onHero,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                Text(
                    String.format(Locale.US, "%.1f hrs", sleepHours),
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Bold,
                    color = onHero,
                    modifier = Modifier.padding(top = 12.dp)
                )
                Text(
                    statusText,
                    fontSize = 13.sp,
                    color = onHero.copy(alpha = 0.85f)
                )
            }
            Box(
                modifier = Modifier
                    .size(124.dp)
                    .semantics {
                        contentDescription = "Sleep tracking ring: ${String.format(Locale.US, "%.1f", sleepHours)} of ${String.format(Locale.US, "%.1f", goal)} hours"
                    },
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                    val stroke = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
                    drawArc(
                        color = onHero.copy(alpha = 0.18f),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = stroke
                    )
                    val sweep = (percent / 100f) * 360f
                    if (sweep > 0f) {
                        drawArc(
                            color = onHero,
                            startAngle = -90f,
                            sweepAngle = sweep,
                            useCenter = false,
                            style = stroke
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        String.format(Locale.US, "%.1fh", sleepHours),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = onHero
                    )
                    Text(
                        String.format(Locale.US, "%.1fh", goal),
                        fontSize = 12.sp,
                        color = onHero.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

/** The last 7 days as bars; today starts selected, tap a bar to inspect that day. Dashed line is sleep goal. */
@Composable
private fun SleepWeekCard(
    days: List<Pair<LocalDate, Float>>,
    goal: Float
) {
    val scheme = MaterialTheme.colorScheme
    var selected by remember(days.size) { mutableIntStateOf(days.lastIndex.coerceAtLeast(0)) }
    val chosen = days.getOrNull(selected)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(scheme.surfaceVariant)
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "This week",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = scheme.onBackground
                )
                if (chosen != null) {
                    Text(
                        if (chosen.second > 0f) "${chosen.first.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.US)} \u00B7 ${String.format(Locale.US, "%.1f", chosen.second)} hrs"
                        else "${chosen.first.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.US)} \u00B7 nothing logged",
                        fontSize = 13.sp,
                        color = scheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            val barSelected = scheme.primary
            val barOther = scheme.onSurfaceVariant.copy(alpha = 0.4f)
            val goalLine = scheme.onSurface.copy(alpha = 0.4f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .semantics { contentDescription = "Sleep for the last 7 days; tap a bar to see its day" }
                    .pointerInput(days.size) {
                        detectTapGestures { offset ->
                            val slot = size.width / days.size.coerceAtLeast(1)
                            selected = (offset.x / slot).toInt().coerceIn(0, (days.size - 1).coerceAtLeast(0))
                        }
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    if (days.isEmpty()) return@Canvas
                    val maxVal = maxOf(days.maxOf { it.second }, goal, 1f)
                    val slot = size.width / days.size
                    val barWidth = slot * 0.45f
                    val usable = size.height - 8.dp.toPx()
                    days.forEachIndexed { index, (_, hours) ->
                        val x = index * slot + (slot - barWidth) / 2f
                        val color = if (index == selected) barSelected else barOther
                        if (hours > 0f) {
                            val h = ((hours / maxVal) * usable).coerceAtLeast(6.dp.toPx())
                            drawRoundRect(
                                color = color,
                                topLeft = Offset(x, size.height - h),
                                size = Size(barWidth, h),
                                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                            )
                        } else {
                            drawRoundRect(
                                color = color.copy(alpha = if (index == selected) 0.8f else 0.35f),
                                topLeft = Offset(x, size.height - 6.dp.toPx()),
                                size = Size(barWidth, 6.dp.toPx()),
                                cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                            )
                        }
                    }
                    if (goal > 0f) {
                        val y = size.height - (goal / maxVal) * usable
                        drawLine(
                            color = goalLine,
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                        )
                    }
                }
            }
            Row(modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
                days.forEachIndexed { index, (date, _) ->
                    Text(
                        date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.US),
                        fontSize = 11.sp,
                        fontWeight = if (index == selected) FontWeight.Bold else FontWeight.Normal,
                        color = if (index == selected) scheme.primary else scheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/** Sleep info card with week's average hours, best night and nights meeting the goal. */
@Composable
private fun SleepInfoCard(
    days: List<Pair<LocalDate, Float>>,
    goal: Float
) {
    val scheme = MaterialTheme.colorScheme
    val recordedDays = days.filter { it.second > 0f }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(scheme.surfaceVariant)
            .padding(16.dp)
    ) {
        Column {
            Text(
                "Sleep info",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = scheme.onBackground
            )
            Spacer(Modifier.height(12.dp))
            if (recordedDays.isEmpty()) {
                Text(
                    "No sleep data logged this week yet.",
                    fontSize = 13.sp,
                    color = scheme.onSurfaceVariant
                )
            } else {
                val avgHours = recordedDays.map { it.second }.average().toFloat()
                val best = recordedDays.maxByOrNull { it.second }
                val metGoalCount = if (goal > 0f) days.count { it.second >= goal } else 0

                SleepInfoRow("Weekly average", String.format(Locale.US, "%.1f hrs", avgHours))
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = scheme.outlineVariant)
                SleepInfoRow(
                    "Best night",
                    best?.let { "${it.first.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.US)} \u00B7 ${String.format(Locale.US, "%.1f hrs", it.second)}" } ?: "\u2014"
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = scheme.outlineVariant)
                SleepInfoRow("Goal reached", "$metGoalCount of 7 nights")
            }
        }
    }
}

@Composable
private fun SleepInfoRow(label: String, value: String) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 13.sp, color = scheme.onSurfaceVariant)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = scheme.onBackground)
    }
}

