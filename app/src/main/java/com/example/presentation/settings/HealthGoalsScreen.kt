package com.example.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.presentation.viewmodel.ShasthoViewModel
import com.example.ui.theme.Emerald600
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthGoalsScreen(
    viewModel: ShasthoViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val profile by viewModel.userProfile.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Health Goals",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val currentProfile = profile
                    if (currentProfile != null) {
                        // 1. Steps Goal
                        GoalRow(
                            goalName = "Steps",
                            displayValue = "${String.format(Locale.US, "%,d", currentProfile.stepGoal)} / day",
                            isAuto = currentProfile.stepGoalIsAuto,
                            rawInitialValue = currentProfile.stepGoal.toString(),
                            editLabel = "Target Steps",
                            keyboardType = KeyboardType.Number,
                            onToggleAuto = { auto ->
                                coroutineScope.launch {
                                    viewModel.saveProfile(currentProfile.copy(stepGoalIsAuto = auto))
                                }
                            },
                            onSaveManual = { input ->
                                val cleaned = input.filter { it.isDigit() }
                                val value = cleaned.toIntOrNull()
                                if (value != null && value > 0) {
                                    coroutineScope.launch {
                                        viewModel.saveProfile(
                                            currentProfile.copy(
                                                stepGoal = value,
                                                stepGoalIsAuto = false
                                            )
                                        )
                                    }
                                }
                            }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )

                        // 2. Sleep Goal
                        GoalRow(
                            goalName = "Sleep",
                            displayValue = "${String.format(Locale.US, "%.1f", currentProfile.sleepGoalHours)} hrs",
                            isAuto = currentProfile.sleepGoalIsAuto,
                            rawInitialValue = String.format(Locale.US, "%.1f", currentProfile.sleepGoalHours),
                            editLabel = "Target Sleep (hours)",
                            keyboardType = KeyboardType.Decimal,
                            onToggleAuto = { auto ->
                                coroutineScope.launch {
                                    viewModel.saveProfile(currentProfile.copy(sleepGoalIsAuto = auto))
                                }
                            },
                            onSaveManual = { input ->
                                val value = input.toFloatOrNull()
                                if (value != null && value > 0f) {
                                    coroutineScope.launch {
                                        viewModel.saveProfile(
                                            currentProfile.copy(
                                                sleepGoalHours = value,
                                                sleepGoalIsAuto = false
                                            )
                                        )
                                    }
                                }
                            }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )

                        // 3. Water Goal
                        GoalRow(
                            goalName = "Water",
                            displayValue = "${String.format(Locale.US, "%.1f", currentProfile.dailyWaterLimitLiters)} L",
                            isAuto = currentProfile.waterGoalIsAuto,
                            rawInitialValue = String.format(Locale.US, "%.1f", currentProfile.dailyWaterLimitLiters),
                            editLabel = "Target Water (Liters)",
                            keyboardType = KeyboardType.Decimal,
                            onToggleAuto = { auto ->
                                coroutineScope.launch {
                                    viewModel.saveProfile(currentProfile.copy(waterGoalIsAuto = auto))
                                }
                            },
                            onSaveManual = { input ->
                                val value = input.toFloatOrNull()
                                if (value != null && value > 0f) {
                                    coroutineScope.launch {
                                        viewModel.saveProfile(
                                            currentProfile.copy(
                                                dailyWaterLimitLiters = value,
                                                waterGoalIsAuto = false
                                            )
                                        )
                                    }
                                }
                            }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )

                        // 4. Calories Goal
                        GoalRow(
                            goalName = "Calories",
                            displayValue = "${currentProfile.dailyCalorieLimit} kcal",
                            isAuto = currentProfile.calorieGoalIsAuto,
                            rawInitialValue = currentProfile.dailyCalorieLimit.toString(),
                            editLabel = "Target Calories (kcal)",
                            keyboardType = KeyboardType.Number,
                            onToggleAuto = { auto ->
                                coroutineScope.launch {
                                    viewModel.saveProfile(currentProfile.copy(calorieGoalIsAuto = auto))
                                }
                            },
                            onSaveManual = { input ->
                                val cleaned = input.filter { it.isDigit() }
                                val value = cleaned.toIntOrNull()
                                if (value != null && value > 0) {
                                    coroutineScope.launch {
                                        viewModel.saveProfile(
                                            currentProfile.copy(
                                                dailyCalorieLimit = value,
                                                calorieGoalIsAuto = false
                                            )
                                        )
                                    }
                                }
                            }
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Emerald600)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GoalRow(
    goalName: String,
    displayValue: String,
    isAuto: Boolean,
    rawInitialValue: String,
    editLabel: String,
    keyboardType: KeyboardType,
    onToggleAuto: (Boolean) -> Unit,
    onSaveManual: (String) -> Unit
) {
    var manualInput by remember(rawInitialValue) { mutableStateOf(rawInitialValue) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$goalName — $displayValue",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = if (isAuto) "Calculated from profile" else "Custom target",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Auto / Manual toggle segmented chips
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isAuto) Emerald600 else Color.Transparent)
                        .clickable { onToggleAuto(true) }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Auto",
                        fontSize = 13.sp,
                        fontWeight = if (isAuto) FontWeight.Bold else FontWeight.Normal,
                        color = if (isAuto) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (!isAuto) Emerald600 else Color.Transparent)
                        .clickable { onToggleAuto(false) }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Manual",
                        fontSize = 13.sp,
                        fontWeight = if (!isAuto) FontWeight.Bold else FontWeight.Normal,
                        color = if (!isAuto) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (!isAuto) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = manualInput,
                    onValueChange = { manualInput = it },
                    label = { Text(editLabel) },
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )
                Button(
                    onClick = { onSaveManual(manualInput) },
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(56.dp)
                ) {
                    Text("Save")
                }
            }
        }
    }
}
