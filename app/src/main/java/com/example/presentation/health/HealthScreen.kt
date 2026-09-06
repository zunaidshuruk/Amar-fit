package com.example.presentation.health

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
    val isDark = profile?.isDarkMode ?: isSystemInDarkTheme()

    val glucoseAccent = AccentTokens.glucoseAccent(isDark)
    val heartRateAccent = AccentTokens.heartRateAccent(isDark)
    val weightAccent = AccentTokens.weightAccent(isDark)
    val bloodPressureAccent = AccentTokens.bloodPressureAccent(isDark)
    val bmiAccent = AccentTokens.bmiAccent(isDark)

    val bmiTrackColor = if (isDark) Emerald800 else Emerald200
    val bmiFillColor = if (isDark) Emerald300 else Emerald600

    val metrics by viewModel.todayMetrics.collectAsState()
    
    val glucose = maxOf(metrics?.bloodGlucoseMorning ?: 0f, metrics?.bloodGlucoseNight ?: 0f)
    val heartRate = metrics?.heartRate ?: 0
    val bloodPressure = metrics?.bloodPressure ?: ""
    val weight = profile?.weightKg ?: 70f
    val heightM = (profile?.heightCm ?: 170f) / 100f
    val bmi = if (heightM > 0) weight / (heightM * heightM) else 0f
    val bmiFillFraction = if (bmi > 0f) ((bmi - 15f) / (35f - 15f)).coerceIn(0f, 1f) else 0f
    
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
                    .clip(RoundedCornerShape(20.dp))
                    .background(glucoseAccent.bg)
                    .clickable { navController.navigate("glucoselog") }
                    .padding(16.dp)
            ) {
                Column {
                    Text(text = "BLOOD GLUCOSE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = glucoseAccent.onBg)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = if(glucose > 0) "$glucose" else "--", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = glucoseAccent.onBg)
                    Text(text = "mmol/L (+ Tap to log)", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = glucoseAccent.onBg)
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(heartRateAccent.bg)
                    .padding(16.dp)
            ) {
                Column {
                    Text(text = "HEART RATE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = heartRateAccent.onBg)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = if(heartRate > 0) "$heartRate bpm" else "--", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = heartRateAccent.onBg)
                    Text(text = "(Synced automatically)", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = heartRateAccent.onBg)
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
                    .clip(RoundedCornerShape(20.dp))
                    .background(weightAccent.bg)
                    .clickable { navController.navigate("weightlog") }
                    .padding(16.dp)
            ) {
                Column {
                    Text(text = "WEIGHT", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = weightAccent.onBg)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "$weight kg", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = weightAccent.onBg)
                    Text(text = "(+ Tap to log)", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = weightAccent.onBg)
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(bloodPressureAccent.bg)
                    .clickable { showBpDialog = true }
                    .padding(16.dp)
            ) {
                Column {
                    Text(text = "BLOOD PRESSURE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = bloodPressureAccent.onBg)
                    Spacer(modifier = Modifier.height(16.dp))
                    val bp = metrics?.bloodPressure?.takeIf { it.isNotBlank() } ?: "--"
                    Text(text = bp, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = bloodPressureAccent.onBg)
                    Text(text = "mmHg (+ Tap to log)", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = bloodPressureAccent.onBg)
                }
            }
        }

        // BMI Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(bmiAccent.bg)
                .clickable { showBmiDialog = true }
                .padding(16.dp)
        ) {
            Column {
                Text(text = "BMI", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = bmiAccent.onBg)
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = String.format("%.1f", bmi), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = bmiAccent.onBg)
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(bmiTrackColor)
                ) {
                    if (bmiFillFraction > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(bmiFillFraction)
                                .fillMaxHeight()
                                .clip(CircleShape)
                                .background(bmiFillColor)
                        )
                    }
                }
            }
        }

        // Protocol Module
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White)
                .border(1.dp, Slate200, RoundedCornerShape(20.dp))
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
        var weightInput by remember { mutableStateOf(profile?.weightKg?.takeIf { it > 0 }?.toString() ?: "70.0") }
        var heightInput by remember { mutableStateOf(profile?.heightCm?.takeIf { it > 0 }?.toString() ?: "170.0") }
        var showWeightDialog by remember { mutableStateOf(false) }
        var showHeightDialog by remember { mutableStateOf(false) }

        if (showWeightDialog) {
            com.example.ui.components.HeightWeightPickerDialog(
                mode = com.example.ui.components.PickerMode.WEIGHT,
                initialValue = weightInput.toFloatOrNull() ?: 70f,
                onDismiss = { showWeightDialog = false },
                onConfirm = { kg ->
                    weightInput = kg.toString()
                    showWeightDialog = false
                }
            )
        }

        if (showHeightDialog) {
            com.example.ui.components.HeightWeightPickerDialog(
                mode = com.example.ui.components.PickerMode.HEIGHT,
                initialValue = heightInput.toFloatOrNull() ?: 170f,
                onDismiss = { showHeightDialog = false },
                onConfirm = { cm ->
                    heightInput = cm.toString()
                    showHeightDialog = false
                }
            )
        }
        
        val w = weightInput.toFloatOrNull() ?: 0f
        val h = heightInput.toFloatOrNull()?.div(100f) ?: 0f
        val calcBmi = if (h > 0) w / (h * h) else 0f
        
        AlertDialog(
            onDismissRequest = { showBmiDialog = false },
            title = { Text("BMI Calculator") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = "${weightInput.toFloatOrNull() ?: 70f} kg",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Weight") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Box(modifier = Modifier.matchParentSize().clickable { showWeightDialog = true })
                    }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = "${heightInput.toFloatOrNull() ?: 170f} cm",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Height") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Box(modifier = Modifier.matchParentSize().clickable { showHeightDialog = true })
                    }
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
