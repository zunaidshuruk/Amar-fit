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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.presentation.viewmodel.ShasthoViewModel
import com.example.ui.theme.*

@Composable
fun SleepScreen(viewModel: ShasthoViewModel, navController: NavController) {
    val profile by viewModel.userProfile.collectAsState()
    val isDark = profile?.isDarkMode ?: isSystemInDarkTheme()

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
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(sleepAccent.bg)
            .clickable { showSleepDialog = true }
            .padding(24.dp)
        ) {
          Column {
            Text(text = "SLEEP TRACKING", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = sleepAccent.onBg)
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = String.format("%.1f Hours", sleepHours), fontSize = 32.sp, fontWeight = FontWeight.Bold, color = sleepAccent.onBg)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "(+ Tap to log manually)", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = sleepAccent.onBg)
          }
        }

        // Coach Module
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(coachAccent.bg)
            .clickable { navController.navigate("coach") }
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
                  .clip(RoundedCornerShape(12.dp))
                  .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
              ) {
                Icon(imageVector = Icons.Default.MonitorHeart, contentDescription = "Coach", tint = coachAccent.onBg)
              }
              Spacer(modifier = Modifier.width(16.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(text = "AI Wellness Coach", color = coachAccent.onBg, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(text = "Optimize your lifestyle", color = coachAccent.onBg, fontSize = 14.sp)
              }
            }
            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Go", tint = Slate400)
          }
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
                      modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
                      contentAlignment = Alignment.Center
                    ) {
                      Text(
                        text = "$hours Hours",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = sleepAccent.onBg,
                        textAlign = TextAlign.Center,
                        maxLines = 1
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
