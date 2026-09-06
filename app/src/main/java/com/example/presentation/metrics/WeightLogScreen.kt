package com.example.presentation.metrics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.presentation.viewmodel.ShasthoViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeightLogScreen(viewModel: ShasthoViewModel, onNavigateBack: () -> Unit = {}) {
    val history by viewModel.metricsHistory.collectAsState()
    val profile by viewModel.userProfile.collectAsState()
    val isDark = profile?.isDarkMode ?: isSystemInDarkTheme()
    
    var weightInput by remember { mutableStateOf(profile?.weightKg?.takeIf { it > 0 }?.toString() ?: "") }
    var heightInput by remember { mutableStateOf(profile?.heightCm?.takeIf { it > 0 }?.toString() ?: "") }

    val currentWeight = weightInput.toFloatOrNull() ?: 0f
    val currentHeightM = (heightInput.toFloatOrNull() ?: 0f) / 100f
    val bmi = if (currentHeightM > 0) currentWeight / (currentHeightM * currentHeightM) else 0f
    
    // Ideal weight based on BMI 22
    val idealWeight = if (currentHeightM > 0) 22f * currentHeightM * currentHeightM else 0f
    val weightDiff = currentWeight - idealWeight

    val (bmiStatus, statusColor) = when {
        bmi == 0f -> Pair("Enter Data", Slate500)
        bmi < 18.5f -> Pair("Underweight", Orange500)
        bmi < 25f -> Pair("Normal", Emerald500)
        bmi < 30f -> Pair("Overweight", Orange500)
        else -> Pair("Very Overweight", Red500)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                text = "Weight & BMI Log",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = weightInput,
                        onValueChange = { weightInput = it },
                        label = { Text("Weight (kg)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = heightInput,
                        onValueChange = { heightInput = it },
                        label = { Text("Height (cm)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        val w = weightInput.toFloatOrNull()
                        val h = heightInput.toFloatOrNull()
                        if (w != null && h != null) {
                            viewModel.setWeightAndHeight(w, h)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald600)
                ) {
                    Text("Save & Calculate")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        if (bmi > 0) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = statusColor.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Text("Your BMI", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text(bmiStatus, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = statusColor)
                        }
                        Text(String.format("%.1f", bmi), fontSize = 32.sp, fontWeight = FontWeight.Bold, color = statusColor)
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = statusColor.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text("Ideal Weight (BMI 22): ${String.format("%.1f", idealWeight)} kg", fontWeight = FontWeight.Medium)
                    if (weightDiff > 1f) {
                        Text("Target to reduce: ${String.format("%.1f", weightDiff)} kg", color = Red700, fontWeight = FontWeight.Bold)
                    } else if (weightDiff < -1f) {
                        Text("Target to gain: ${String.format("%.1f", -weightDiff)} kg", color = Orange700, fontWeight = FontWeight.Bold)
                    } else {
                        Text("You are at your ideal weight! Great job!", color = Emerald600, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        Text("Weight Log (3 Months)", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.height(12.dp))
        
        val validWeights = history.filter { it.weightKg > 0 }.reversed()
        if (validWeights.isEmpty()) {
            Text("No weight data available.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(24.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val minW = (validWeights.minOfOrNull { it.weightKg } ?: 50f) - 5f
                    val maxW = (validWeights.maxOfOrNull { it.weightKg } ?: 100f) + 5f
                    val range = maxW - minW
                    val width = size.width
                    val height = size.height
                    
                    val stepX = if (validWeights.size > 1) width / (validWeights.size - 1) else width
                    val weightPath = Path()
                    
                    validWeights.forEachIndexed { index, metric ->
                        val x = index * stepX
                        val y = height - (((metric.weightKg - minW) / range) * height)
                        
                        if (index == 0) weightPath.moveTo(x, y) else weightPath.lineTo(x, y)
                        drawCircle(color = Emerald500, radius = 6.dp.toPx(), center = Offset(x, y))
                    }
                    drawPath(weightPath, color = Emerald500, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
                }
            }
        }
        Spacer(modifier = Modifier.height(100.dp))
    }
}
