package com.example.presentation.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Pool
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.LocalNavController
import com.example.data.health.HealthConnectManager
import com.example.data.local.HealthExerciseSession
import com.example.presentation.viewmodel.ShasthoViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivitySection(viewModel: ShasthoViewModel) {
    var daysShown by remember { mutableStateOf(7) }
    var refreshOk by remember { mutableStateOf<Boolean?>(null) }
    val isRefreshing by viewModel.isRefreshingExerciseSessions.collectAsState()
    var historyGranted by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val navController = LocalNavController.current

    LaunchedEffect(daysShown) {
        refreshOk = viewModel.refreshExerciseSessions(context, daysShown)
    }

    LaunchedEffect(Unit) {
        historyGranted = HealthConnectManager.hasHistoryPermission(context)
    }

    val zone = remember { ZoneId.systemDefault() }
    val fromMillis = remember(daysShown) {
        LocalDate.now().minusDays((daysShown - 1).toLong()).atStartOfDay(zone).toInstant().toEpochMilli()
    }
    val toMillis = remember(daysShown) {
        LocalDate.now().plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
    }

    val sessions by remember(fromMillis, toMillis) {
        viewModel.observeExerciseSessions(fromMillis, toMillis)
    }.collectAsState(initial = emptyList())

    fun sessionCalories(s: HealthExerciseSession): Int =
        if (s.activeCalories > 0) s.activeCalories else s.totalCalories

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        if (isRefreshing) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            listOf(7 to "7 days", 30 to "30 days", 90 to "90 days").forEach { (days, label) ->
                FilterChip(
                    selected = daysShown == days,
                    onClick = { daysShown = days },
                    label = { Text(label) }
                )
            }
        }

        if (refreshOk == false && !isRefreshing) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Connect Health Connect to see workouts from Google Fit, Fitbit, Samsung Health and other apps",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Button(
                        onClick = { navController?.navigate("settings") },
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Open Settings")
                    }
                }
            }
        }

        // Today Card
        val startOfToday = remember { LocalDate.now().atStartOfDay(zone).toInstant().toEpochMilli() }
        val todaySessions = sessions.filter { it.startTime >= startOfToday }
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Today",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (todaySessions.isEmpty()) {
                    Text(
                        text = "No workouts yet today.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    val workoutCount = todaySessions.size
                    val totalMinutes = todaySessions.sumOf { it.durationMinutes }
                    val totalKcal = todaySessions.sumOf { sessionCalories(it) }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "$workoutCount",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Workouts",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "$totalMinutes",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Minutes",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (totalKcal > 0) "$totalKcal" else "—",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "kcal",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        val caloriesToday by produceState<Pair<Int, Int>?>(initialValue = null, key1 = isRefreshing) {
            value = viewModel.getCaloriesBurnedForDate(LocalDate.now())
        }
        caloriesToday?.let { (active, total) ->
            if (active > 0 || total > 0) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "Calories burned today",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    if (active > 0) "$active" else "—",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    "Active kcal",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    if (total > 0) "$total" else "—",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    "Total kcal",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Text(
                            "Active comes from movement and workouts. Total also includes the calories your body burns at rest.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(onClick = { navController?.navigate("metric_detail/activeCaloriesBurned") }) {
                            Text("See trend")
                        }
                    }
                }
            }
        }

        // History
        val groupedSessions = remember(sessions) {
            sessions.groupBy { Instant.ofEpochMilli(it.startTime).atZone(zone).toLocalDate() }
                .toList()
                .sortedByDescending { it.first }
        }
        val todayDate = LocalDate.now()
        groupedSessions.forEach { (date, dateSessions) ->
            val dateLabel = when (date) {
                todayDate -> "Today"
                todayDate.minusDays(1) -> "Yesterday"
                else -> date.format(DateTimeFormatter.ofPattern("EEE, d MMM"))
            }
            val nWorkouts = dateSessions.size
            val mMin = dateSessions.sumOf { it.durationMinutes }
            val kKcal = dateSessions.sumOf { sessionCalories(it) }
            val summaryText = buildString {
                append("$nWorkouts workout${if (nWorkouts > 1) "s" else ""} • $mMin min")
                if (kKcal > 0) {
                    append(" • $kKcal kcal")
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dateLabel,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = summaryText,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            dateSessions.forEach { session ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = exerciseTypeIcon(session.typeLabel),
                                contentDescription = session.typeLabel,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = session.title.ifBlank { session.typeLabel },
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val timeStr = Instant.ofEpochMilli(session.startTime).atZone(zone).format(DateTimeFormatter.ofPattern("h:mm a"))
                            val distText = if (session.distanceMeters >= 100f) " • %.1f km".format(session.distanceMeters / 1000f) else ""
                            Text(
                                text = "$timeStr • ${session.durationMinutes} min$distText",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "via ${if (session.isOwnApp) "KardIQ" else session.sourceLabel}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(horizontalAlignment = Alignment.End) {
                            val cal = sessionCalories(session)
                            Text(
                                text = if (cal > 0) "$cal kcal" else "—",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            val calLabel = when {
                                session.activeCalories > 0 -> "active"
                                session.totalCalories > 0 -> "total"
                                else -> ""
                            }
                            if (calLabel.isNotEmpty()) {
                                Text(
                                    text = calLabel,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        if (sessions.isEmpty() && !isRefreshing && refreshOk != false) {
            Text(
                text = "No exercise sessions in the last $daysShown days.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            )
        }

        OutlinedButton(
            onClick = { daysShown = (daysShown + 30).coerceAtMost(3650) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Load earlier")
        }

        if (daysShown >= 30 && !historyGranted) {
            Text(
                text = "Data older than 30 days needs \"Older exercise history\" in Settings (available on newer Health Connect versions).",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(100.dp))
    }
}

private fun exerciseTypeIcon(typeLabel: String): ImageVector {
    return when (typeLabel) {
        "Running" -> Icons.AutoMirrored.Filled.DirectionsRun
        "Walking", "Hiking" -> Icons.AutoMirrored.Filled.DirectionsWalk
        "Cycling" -> Icons.AutoMirrored.Filled.DirectionsBike
        "Swimming" -> Icons.Default.Pool
        "Yoga", "Pilates", "Stretching" -> Icons.Default.SelfImprovement
        else -> Icons.Default.FitnessCenter
    }
}
