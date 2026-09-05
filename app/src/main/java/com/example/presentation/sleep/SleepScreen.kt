package com.example.presentation.sleep

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.presentation.viewmodel.ShasthoViewModel
import com.example.ui.theme.*

@Composable
fun SleepScreen(viewModel: ShasthoViewModel, navController: NavController) {
    val metrics by viewModel.todayMetrics.collectAsState()
    val sleepHours = metrics?.sleepHours ?: 0f
    var showSleepDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Sleep & Wellness", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary)

        // Sleep Tracking Card
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(IndigoBg)
            .clickable { showSleepDialog = true }
            .padding(24.dp)
        ) {
          Column {
            Text(text = "SLEEP TRACKING", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Indigo700)
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = String.format("%.1f Hours", sleepHours), fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Indigo900)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "(+ Tap to log manually)", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Indigo700)
          }
        }

        // Coach Module
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .border(1.dp, Slate200, RoundedCornerShape(24.dp))
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
                  .background(BlueBg),
                contentAlignment = Alignment.Center
              ) {
                Icon(imageVector = Icons.Default.MonitorHeart, contentDescription = "Coach", tint = Blue700)
              }
              Spacer(modifier = Modifier.width(16.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(text = "AI Wellness Coach", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(text = "Optimize your lifestyle", color = Slate500, fontSize = 14.sp)
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
          title = { Text("Log Sleep") },
          text = {
            OutlinedTextField(
              value = sleepInput,
              onValueChange = { sleepInput = it },
              label = { Text("Sleep (Hours)") },
              keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
            )
          },
          confirmButton = {
            TextButton(onClick = {
              val s = sleepInput.toFloatOrNull()
              if (s != null) {
                viewModel.setSleep(s)
              }
              showSleepDialog = false
            }) {
              Text("Save")
            }
          },
          dismissButton = {
            TextButton(onClick = { showSleepDialog = false }) {
              Text("Cancel")
            }
          }
        )
    }
}
