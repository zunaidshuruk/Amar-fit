package com.example.presentation.sleep

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.data.local.DailyMetric
import com.example.presentation.viewmodel.ShasthoViewModel
import com.example.ui.components.HeroStatCard
import com.example.ui.components.NavListCard
import com.example.ui.theme.*

@Composable
fun SleepScreen(viewModel: ShasthoViewModel, navController: NavController) {
    val profile by viewModel.userProfile.collectAsState()
    val isDark = true

    val sleepAccent = AccentTokens.sleepAccent(isDark)
    val coachAccent = AccentTokens.coachAccent(isDark)

    val metrics by viewModel.todayMetrics.collectAsState()
    val sleepHours = metrics?.sleepHours ?: 0f
    var showSleepDialog by remember { mutableStateOf(false) }
    val sleepHistoryFlow = remember { viewModel.getMetricsHistoryFlow(7) }
    val sleepHistory by sleepHistoryFlow.collectAsState(initial = emptyList())
    val chronologicalSleepHistory = remember(sleepHistory) { sleepHistory.reversed() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Sleep & Wellness", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)

        // Sleep Tracking Card
        HeroStatCard(
            label = "SLEEP TRACKING",
            value = String.format("%.1f Hours", sleepHours),
            caption = profile?.sleepGoalHours?.let { goal -> String.format("of %.1f hrs goal", goal) },
            onClick = { showSleepDialog = true }
        ) {
            Text(
                text = "(+ Tap to log manually)",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }

        // Last 7 Nights Chart
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(16.dp)
        ) {
            Column {
                Text(
                    text = "Last 7 Nights",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(12.dp))
                if (chronologicalSleepHistory.any { it.sleepHours > 0f }) {
                    SleepWeekBarChart(
                        data = chronologicalSleepHistory,
                        goalHours = profile?.sleepGoalHours,
                        isDark = isDark
                    )
                } else {
                    Text(
                        text = "No sleep data logged this week yet.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Coach Module
        NavListCard(
            icon = Icons.Default.MonitorHeart,
            title = "AI Wellness Coach",
            subtitle = "Optimize your lifestyle",
            accent = coachAccent,
            onClick = { navController.navigate("coach") }
        )
    }

    if (showSleepDialog) {
        var sleepInput by remember { mutableStateOf("") }
        AlertDialog(
          onDismissRequest = { showSleepDialog = false },
          title = {
            Text(
              text = "Log Sleep",
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
                    color = sleepAccent.bg,
                    border = BorderStroke(
                      1.dp,
                      if (isDark) sleepAccent.onBg.copy(alpha = 0.4f) else sleepAccent.onBg.copy(alpha = 0.2f)
                    )
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
                        color = sleepAccent.onBg,
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
                  shape = RoundedCornerShape(12.dp)
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
                    containerColor = sleepAccent.onBg,
                    contentColor = if (isDark) Color(0xFF1E1E1E) else Color.White
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
              Text("Cancel")
            }
          }
        )
    }
}

@Composable
private fun SleepWeekBarChart(data: List<DailyMetric>, goalHours: Float?, isDark: Boolean) {
    val goalLineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
    val barColor = AccentTokens.sleepAccent(isDark).onBg

    BoxWithConstraints(modifier = Modifier.fillMaxWidth().height(180.dp)) {
        val totalWidth = maxWidth
        val contentWidth: Dp = if (data.size > 10) (data.size * 32).dp.coerceAtLeast(totalWidth) else totalWidth

        Box(
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(rememberScrollState())
        ) {
            Canvas(modifier = Modifier.width(contentWidth).fillMaxHeight().padding(vertical = 12.dp)) {
                val dataMax = (data.maxOfOrNull { it.sleepHours } ?: 8f).coerceAtLeast(1f)
                val maxVal = if (goalHours != null && goalHours > dataMax) goalHours else dataMax

                val count = data.size
                val width = size.width
                val height = size.height
                val barWidth = (width / (count * 1.5f)).coerceIn(8.dp.toPx(), 32.dp.toPx())
                val spacing = if (count > 1) (width - (count * barWidth)) / (count - 1) else 0f

                data.forEachIndexed { index, metric ->
                    val x = if (count > 1) index * (barWidth + spacing) else (width - barWidth) / 2f
                    if (metric.sleepHours > 0f) {
                        val ratio = (metric.sleepHours / maxVal).coerceIn(0f, 1f)
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
                            color = (if (isDark) Slate400 else Slate600).copy(alpha = 0.35f),
                            topLeft = Offset(x, height - 8.dp.toPx()),
                            size = Size(barWidth, 8.dp.toPx()),
                            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                        )
                    }
                }

                if (goalHours != null && goalHours > 0f) {
                    val ratio = (goalHours / maxVal).coerceIn(0f, 1f)
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
