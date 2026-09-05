package com.example.presentation.health

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.MenuBook
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
fun HealthScreen(viewModel: ShasthoViewModel, navController: NavController) {
    val profile by viewModel.userProfile.collectAsState()
    val metrics by viewModel.todayMetrics.collectAsState()
    
    val glucose = maxOf(metrics?.bloodGlucoseMorning ?: 0f, metrics?.bloodGlucoseNight ?: 0f)
    val heartRate = metrics?.heartRate ?: 0
    val bloodPressure = metrics?.bloodPressure ?: ""
    val weight = profile?.weightKg ?: 70f
    val heightM = (profile?.heightCm ?: 170f) / 100f
    val bmi = if (heightM > 0) weight / (heightM * heightM) else 0f
    
    var showBpDialog by remember { mutableStateOf(false) }
    var showBmiDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Health Vitals", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        
        // Row 1: Glucose & Heart Rate
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(IndigoBg)
                    .clickable { navController.navigate("glucoselog") }
                    .padding(16.dp)
            ) {
                Column {
                    Text(text = "BLOOD GLUCOSE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Indigo700)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = if(glucose > 0) "$glucose" else "--", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Indigo900)
                    Text(text = "mmol/L (+ Tap to log)", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Indigo600)
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Red50)
                    .border(1.dp, Red100, RoundedCornerShape(24.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Text(text = "HEART RATE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Red700)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = if(heartRate > 0) "$heartRate bpm" else "--", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Red900)
                    Text(text = "(Synced automatically)", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Red700)
                }
            }
        }

        // Row 2: Weight & Blood Pressure
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Orange50)
                    .border(1.dp, Orange100, RoundedCornerShape(24.dp))
                    .clickable { navController.navigate("weightlog") }
                    .padding(16.dp)
            ) {
                Column {
                    Text(text = "WEIGHT", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Orange700)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "$weight kg", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Orange900)
                    Text(text = "(+ Tap to log)", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Orange700)
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Red50)
                    .border(1.dp, Red100, RoundedCornerShape(24.dp))
                    .clickable { showBpDialog = true }
                    .padding(16.dp)
            ) {
                Column {
                    Text(text = "BLOOD PRESSURE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Red700)
                    Spacer(modifier = Modifier.height(16.dp))
                    val bp = metrics?.bloodPressure?.takeIf { it.isNotBlank() } ?: "--"
                    Text(text = bp, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Red900)
                    Text(text = "mmHg (+ Tap to log)", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Red700)
                }
            }
        }

        // BMI Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Emerald50)
                .border(1.dp, Emerald100, RoundedCornerShape(24.dp))
                .clickable { showBmiDialog = true }
                .padding(16.dp)
        ) {
            Column {
                Text(text = "BMI", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Emerald700)
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = String.format("%.1f", bmi), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Emerald900)
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(Emerald200)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.5f)
                            .fillMaxHeight()
                            .background(Emerald600)
                    )
                }
            }
        }

        // Protocol Module
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White)
                .border(1.dp, Slate200, RoundedCornerShape(24.dp))
                .clickable { navController.navigate("lifestyle") }
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
                            .background(Emerald50),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.MenuBook, contentDescription = "Protocol", tint = Emerald700)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Lifestyle Protocol", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(text = "Read the health guidelines", color = Slate500, fontSize = 14.sp)
                    }
                }
                Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Go", tint = Slate400)
            }
        }
    }

    if (showBpDialog) {
        var bpInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showBpDialog = false },
            title = { Text("Log Blood Pressure") },
            text = {
                OutlinedTextField(
                    value = bpInput,
                    onValueChange = { bpInput = it },
                    label = { Text("Systolic/Diastolic (e.g. 120/80)") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (bpInput.isNotBlank()) {
                        viewModel.setBloodPressure(bpInput)
                    }
                    showBpDialog = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBpDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showBmiDialog) {
        var weightInput by remember { mutableStateOf(profile?.weightKg?.toString() ?: "") }
        var heightInput by remember { mutableStateOf(profile?.heightCm?.toString() ?: "") }
        
        val w = weightInput.toFloatOrNull() ?: 0f
        val h = heightInput.toFloatOrNull()?.div(100f) ?: 0f
        val calcBmi = if (h > 0) w / (h * h) else 0f
        
        AlertDialog(
            onDismissRequest = { showBmiDialog = false },
            title = { Text("BMI Calculator") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = weightInput,
                        onValueChange = { weightInput = it },
                        label = { Text("Weight (kg)") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = heightInput,
                        onValueChange = { heightInput = it },
                        label = { Text("Height (cm)") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "BMI: ${String.format("%.1f", calcBmi)}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Emerald700)
                    val status = when {
                        calcBmi == 0f -> ""
                        calcBmi < 18.5f -> "Underweight"
                        calcBmi < 25f -> "Normal"
                        calcBmi < 30f -> "Overweight"
                        else -> "Obese"
                    }
                    Text(text = status, fontSize = 16.sp, color = Slate600)
                }
            },
            confirmButton = {
                TextButton(onClick = { showBmiDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}
