package com.example.presentation.sleep

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
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
