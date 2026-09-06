package com.example.presentation.metrics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
fun GlucoseLogScreen(viewModel: ShasthoViewModel, onNavigateBack: () -> Unit = {}) {
    val history by viewModel.metricsHistory.collectAsState()
    val today by viewModel.todayMetrics.collectAsState()
    
    var morningInput by remember { mutableStateOf(today?.bloodGlucoseMorning?.takeIf { it > 0 }?.toString() ?: "") }
    var nightInput by remember { mutableStateOf(today?.bloodGlucoseNight?.takeIf { it > 0 }?.toString() ?: "") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
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
                    tint = TextPrimary
                )
            }
            Text(
                text = "Blood Glucose Trends",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Today's Readings (mmol/L)", fontWeight = FontWeight.Bold, color = Indigo700)
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = morningInput,
                        onValueChange = { morningInput = it },
                        label = { Text("Fasting (Morning)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = nightInput,
                        onValueChange = { nightInput = it },
                        label = { Text("Post-Meal (Night)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        morningInput.toFloatOrNull()?.let { viewModel.setBloodGlucoseMorning(it) }
                        nightInput.toFloatOrNull()?.let { viewModel.setBloodGlucoseNight(it) }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Indigo600)
                ) {
                    Text("Save Readings")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Calculate HbA1c
        val validReadings = history.flatMap { 
            listOfNotNull(
                it.bloodGlucoseMorning.takeIf { v -> v > 0 },
                it.bloodGlucoseNight.takeIf { v -> v > 0 }
            )
        }
        if (validReadings.isNotEmpty()) {
            val avgGlucoseMmol = validReadings.average().toFloat()
            // Formula: HbA1c = (eAG_mg_dl + 46.7) / 28.7 where eAG_mg_dl = mmol * 18
            val hba1c = (avgGlucoseMmol * 18 + 46.7f) / 28.7f
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Indigo50),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Estimated HbA1c", fontWeight = FontWeight.Bold, color = Indigo900)
                        Text("Based on 3 months data", fontSize = 12.sp, color = Indigo700)
                    }
                    Text("${String.format("%.1f", hba1c)}%", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Indigo600)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        Text("Last 3 Months Trend", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Spacer(modifier = Modifier.height(12.dp))
        
        if (history.isEmpty()) {
            Text("No data available to display.", color = Slate500)
        } else {
            val chartData = history.reversed()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .padding(24.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val maxVal = maxOf(10f, chartData.maxOfOrNull { maxOf(it.bloodGlucoseMorning, it.bloodGlucoseNight) } ?: 10f)
                    val width = size.width
                    val height = size.height
                    
                    val stepX = if (chartData.size > 1) width / (chartData.size - 1) else width
                    
                    // Draw Morning path
                    val morningPath = Path()
                    // Draw Night path
                    val nightPath = Path()
                    
                    chartData.forEachIndexed { index, metric ->
                        val x = index * stepX
                        
                        val mY = height - ((metric.bloodGlucoseMorning / maxVal) * height)
                        if (index == 0) morningPath.moveTo(x, mY) else morningPath.lineTo(x, mY)
                        drawCircle(color = Indigo600, radius = 6.dp.toPx(), center = Offset(x, mY))
                        
                        val nY = height - ((metric.bloodGlucoseNight / maxVal) * height)
                        if (index == 0) nightPath.moveTo(x, nY) else nightPath.lineTo(x, nY)
                        drawCircle(color = Orange500, radius = 6.dp.toPx(), center = Offset(x, nY))
                    }
                    
                    drawPath(morningPath, color = Indigo600, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
                    drawPath(nightPath, color = Orange500, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(12.dp).clip(RoundedCornerShape(6.dp)).background(Indigo600))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Morning", fontSize = 12.sp, color = Slate600)
                }
                Spacer(modifier = Modifier.width(24.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(12.dp).clip(RoundedCornerShape(6.dp)).background(Orange500))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Night", fontSize = 12.sp, color = Slate600)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(100.dp))
    }
}
