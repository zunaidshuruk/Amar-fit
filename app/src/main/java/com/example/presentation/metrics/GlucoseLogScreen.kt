package com.example.presentation.metrics

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.launch
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
import androidx.compose.material.icons.filled.TipsAndUpdates
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.presentation.viewmodel.ShasthoViewModel
import com.example.ui.components.MarkdownText
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlucoseLogScreen(viewModel: ShasthoViewModel, onNavigateBack: () -> Unit = {}, onNavigateToHistoricalEntry: () -> Unit = {}) {
    val history by viewModel.metricsHistory.collectAsState()
    val history30 by remember(viewModel) { viewModel.getMetricsHistoryFlow(30) }.collectAsState(initial = emptyList())
    val today by viewModel.todayMetrics.collectAsState()
    val profile by viewModel.userProfile.collectAsState()
    val isDark = profile?.isDarkMode ?: isSystemInDarkTheme()
    val glucoseAccent = AccentTokens.glucoseAccent(isDark)
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val glucoseGuidance by viewModel.glucoseGuidance.collectAsState()
    val isLoadingGlucoseGuidance by viewModel.isLoadingGlucoseGuidance.collectAsState()
    
    val importLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: android.net.Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                val result = viewModel.importGlucoseCsv(uri)
                val message = "Imported ${result.daysImported} day(s). Skipped ${result.daysSkippedAlreadyLogged} already logged, ${result.rowsSkippedInvalid} invalid row(s)."
                android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }
    
    var beforeBreakfastInput by remember { mutableStateOf(today?.bloodGlucoseBeforeBreakfast?.takeIf { it > 0 }?.toString() ?: "") }
    var afterBreakfastInput by remember { mutableStateOf(today?.bloodGlucoseAfterBreakfast?.takeIf { it > 0 }?.toString() ?: "") }
    var beforeLunchInput by remember { mutableStateOf(today?.bloodGlucoseBeforeLunch?.takeIf { it > 0 }?.toString() ?: "") }
    var afterLunchInput by remember { mutableStateOf(today?.bloodGlucoseAfterLunch?.takeIf { it > 0 }?.toString() ?: "") }
    var beforeDinnerInput by remember { mutableStateOf(today?.bloodGlucoseBeforeDinner?.takeIf { it > 0 }?.toString() ?: "") }
    var afterDinnerInput by remember { mutableStateOf(today?.bloodGlucoseAfterDinner?.takeIf { it > 0 }?.toString() ?: "") }
    var specimenSource by remember { mutableStateOf(today?.bloodGlucoseSpecimenSource?.takeIf { it != "Not set" } ?: "Not set") }
    var expandedSpecimenSource by remember { mutableStateOf(false) }
    val specimenSourceOptions = listOf("Not set", "Interstitial fluid", "Capillary blood", "Plasma", "Serum", "Tears", "Whole blood")

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
                text = "Blood Glucose Trends",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        TextButton(
            onClick = onNavigateToHistoricalEntry,
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("+ Add a Past Day's Reading")
        }
        Spacer(modifier = Modifier.height(4.dp))
        TextButton(
            onClick = {
                coroutineScope.launch {
                    val uri = viewModel.generateGlucoseCsvUri()
                    if (uri != null) {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/csv"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share glucose CSV template"))
                    } else {
                        android.widget.Toast.makeText(context, "Couldn't generate the CSV file.", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Download CSV Template")
        }
        Spacer(modifier = Modifier.height(4.dp))
        TextButton(
            onClick = {
                importLauncher.launch("text/*")
            },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Import CSV")
        }
        Spacer(modifier = Modifier.height(8.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = glucoseAccent.bg),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Today's Readings (mmol/L)", fontWeight = FontWeight.Bold, color = glucoseAccent.onBg)
                Spacer(modifier = Modifier.height(12.dp))

                Text("Breakfast", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = glucoseAccent.onBg)
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = beforeBreakfastInput,
                        onValueChange = { beforeBreakfastInput = it },
                        label = { Text("Before") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = afterBreakfastInput,
                        onValueChange = { afterBreakfastInput = it },
                        label = { Text("After") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text("Lunch", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = glucoseAccent.onBg)
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = beforeLunchInput,
                        onValueChange = { beforeLunchInput = it },
                        label = { Text("Before") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = afterLunchInput,
                        onValueChange = { afterLunchInput = it },
                        label = { Text("After") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text("Dinner", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = glucoseAccent.onBg)
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = beforeDinnerInput,
                        onValueChange = { beforeDinnerInput = it },
                        label = { Text("Before") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = afterDinnerInput,
                        onValueChange = { afterDinnerInput = it },
                        label = { Text("After") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                ExposedDropdownMenuBox(
                    expanded = expandedSpecimenSource,
                    onExpandedChange = { expandedSpecimenSource = !expandedSpecimenSource },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = specimenSource,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Specimen source") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedSpecimenSource) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = expandedSpecimenSource,
                        onDismissRequest = { expandedSpecimenSource = false }
                    ) {
                        specimenSourceOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    specimenSource = option
                                    expandedSpecimenSource = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        val bb = beforeBreakfastInput.toFloatOrNull()
                        val ab = afterBreakfastInput.toFloatOrNull()
                        val bl = beforeLunchInput.toFloatOrNull()
                        val al = afterLunchInput.toFloatOrNull()
                        val bd = beforeDinnerInput.toFloatOrNull()
                        val ad = afterDinnerInput.toFloatOrNull()
                        if (bb == null && ab == null && bl == null && al == null && bd == null && ad == null) {
                            android.widget.Toast.makeText(context, "Enter a valid glucose reading", android.widget.Toast.LENGTH_SHORT).show()
                        } else {
                            coroutineScope.launch {
                                viewModel.saveGlucoseReadingsForDate(
                                    date = viewModel.todayDateString,
                                    beforeBreakfast = bb,
                                    afterBreakfast = ab,
                                    beforeLunch = bl,
                                    afterLunch = al,
                                    beforeDinner = bd,
                                    afterDinner = ad,
                                    specimenSource = specimenSource
                                )
                                android.widget.Toast.makeText(context, "Glucose reading(s) saved", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = glucoseAccent.onBg)
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
                it.bloodGlucoseNight.takeIf { v -> v > 0 },
                it.bloodGlucoseBeforeBreakfast.takeIf { v -> v > 0 },
                it.bloodGlucoseAfterBreakfast.takeIf { v -> v > 0 },
                it.bloodGlucoseBeforeLunch.takeIf { v -> v > 0 },
                it.bloodGlucoseAfterLunch.takeIf { v -> v > 0 },
                it.bloodGlucoseBeforeDinner.takeIf { v -> v > 0 },
                it.bloodGlucoseAfterDinner.takeIf { v -> v > 0 }
            )
        }
        if (validReadings.isNotEmpty()) {
            val avgGlucoseMmol = validReadings.average().toFloat()
            // Formula: HbA1c = (eAG_mg_dl + 46.7) / 28.7 where eAG_mg_dl = mmol * 18
            val hba1c = (avgGlucoseMmol * 18 + 46.7f) / 28.7f
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = glucoseAccent.bg),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Estimated HbA1c", fontWeight = FontWeight.Bold, color = glucoseAccent.onBg)
                        Text("Based on 3 months data", fontSize = 12.sp, color = glucoseAccent.onBg)
                    }
                    Text("${String.format("%.1f", hba1c)}%", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = glucoseAccent.onBg)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        Text("Last 30 Days Trend", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.height(12.dp))
        
        val chartData = remember(history30) { history30.reversed() }
        val dailyAverages = remember(chartData) {
            chartData.map { metric ->
                val readings = listOfNotNull(
                    metric.bloodGlucoseMorning.takeIf { it > 0 },
                    metric.bloodGlucoseNight.takeIf { it > 0 },
                    metric.bloodGlucoseBeforeBreakfast.takeIf { it > 0 },
                    metric.bloodGlucoseAfterBreakfast.takeIf { it > 0 },
                    metric.bloodGlucoseBeforeLunch.takeIf { it > 0 },
                    metric.bloodGlucoseAfterLunch.takeIf { it > 0 },
                    metric.bloodGlucoseBeforeDinner.takeIf { it > 0 },
                    metric.bloodGlucoseAfterDinner.takeIf { it > 0 }
                )
                if (readings.isNotEmpty()) readings.average().toFloat() else null
            }
        }
        val hasChartData = remember(dailyAverages) { dailyAverages.any { it != null && it > 0f } }

        if (!hasChartData) {
            Text("No data available to display.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            val targetMin = profile?.bloodGlucoseTargetMin ?: 0f
            val targetMax = profile?.bloodGlucoseTargetMax ?: 0f
            val hasTargetRange = targetMin > 0f && targetMax > 0f && targetMin < targetMax

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(24.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val validMax = dailyAverages.filterNotNull().maxOrNull() ?: 10f
                    val targetCeiling = if (hasTargetRange) targetMax else 0f
                    val maxVal = maxOf(10f, validMax, targetCeiling)
                    val width = size.width
                    val height = size.height
                    val count = chartData.size

                    if (hasTargetRange) {
                        val bandTopY = height - ((targetMax / maxVal) * height)
                        val bandBottomY = height - ((targetMin / maxVal) * height)
                        drawRect(
                            color = glucoseAccent.onBg.copy(alpha = 0.15f),
                            topLeft = Offset(0f, bandTopY),
                            size = Size(width, bandBottomY - bandTopY)
                        )
                    }
                    
                    val stepX = if (count > 1) width / (count - 1) else width
                    val path = Path()
                    var lastValidIndex: Int? = null

                    dailyAverages.forEachIndexed { index, avg ->
                        if (avg != null && avg > 0f) {
                            val x = if (count > 1) index * stepX else width / 2f
                            val y = height - ((avg / maxVal) * height)

                            if (lastValidIndex == null || lastValidIndex != index - 1) {
                                path.moveTo(x, y)
                            } else {
                                path.lineTo(x, y)
                            }
                            lastValidIndex = index
                            drawCircle(color = glucoseAccent.onBg, radius = 6.dp.toPx(), center = Offset(x, y))
                        }
                    }

                    drawPath(path, color = glucoseAccent.onBg, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            if (hasTargetRange) {
                Text(
                    text = "Shaded band shows your target range: ${targetMin} – ${targetMax} mmol/L",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = "Set a target range in Health Goals to see it shaded here.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.TipsAndUpdates, contentDescription = null, tint = glucoseAccent.onBg)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("AI Glucose Suggestions", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                }
                Spacer(modifier = Modifier.height(12.dp))

                val guidance = glucoseGuidance
                if (guidance != null) {
                    MarkdownText(text = guidance, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "This is general wellness guidance, not medical advice. Consult a healthcare professional for any health concerns.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                } else if (isLoadingGlucoseGuidance) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = glucoseAccent.onBg, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Analyzing your recent data...", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                    }
                } else {
                    Text("Get lifestyle suggestions based on your recent glucose readings and other health data.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { viewModel.fetchGlucoseGuidance() },
                        colors = ButtonDefaults.buttonColors(containerColor = glucoseAccent.onBg),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.TipsAndUpdates, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Get AI Suggestions", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(100.dp))
    }
}
