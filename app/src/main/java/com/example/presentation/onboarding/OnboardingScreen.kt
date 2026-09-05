package com.example.presentation.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.UserProfile
import com.example.presentation.viewmodel.ShasthoViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(viewModel: ShasthoViewModel, onComplete: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    var isSaving by rememberSaveable { mutableStateOf(false) }
    var saveError by rememberSaveable { mutableStateOf<String?>(null) }

    var name by rememberSaveable { mutableStateOf("") }
    
    // Birthday & Age logic
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var dobTimestamp by rememberSaveable { mutableStateOf<Long?>(null) }
    
    val datePickerState = rememberDatePickerState(
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                return utcTimeMillis <= System.currentTimeMillis()
            }
            override fun isSelectableYear(year: Int): Boolean {
                return year <= LocalDate.now().year
            }
        }
    )

    val dobLocalDate = remember(dobTimestamp) {
        if (dobTimestamp != null) {
            Instant.ofEpochMilli(dobTimestamp!!).atZone(ZoneId.of("UTC")).toLocalDate()
        } else {
            null
        }
    }

    val calculatedAge = remember(dobLocalDate) {
        if (dobLocalDate != null) {
            val now = LocalDate.now()
            ChronoUnit.YEARS.between(dobLocalDate, now).toInt().coerceAtLeast(0)
        } else {
            null
        }
    }

    val dobString = remember(dobLocalDate) {
        dobLocalDate?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) ?: ""
    }
    
    // Gender logic
    var gender by rememberSaveable { mutableStateOf("Male") }
    var expandedGender by rememberSaveable { mutableStateOf(false) }
    val genderOptions = listOf("Male", "Female", "Other")

    // Height logic
    var heightFeet by rememberSaveable { mutableStateOf("") }
    var heightInches by rememberSaveable { mutableStateOf("") }
    var heightError by rememberSaveable { mutableStateOf<String?>(null) }

    // Weight logic
    var weight by rememberSaveable { mutableStateOf("") }
    var weightError by rememberSaveable { mutableStateOf<String?>(null) }
    
    val dietOptions = listOf("None", "Halal", "Vegan", "Vegetarian", "Keto", "Gluten-Free", "Other")
    // Sets aren't directly supported by rememberSaveable out of the box, use string conversion or list
    var selectedDietsList by rememberSaveable { mutableStateOf(listOf<String>()) }
    var customDiet by rememberSaveable { mutableStateOf("") }
    
    val goalOptions = listOf("Lose weight", "Build muscle", "Stay healthy", "Improve sleep", "Other")
    var selectedGoalsList by rememberSaveable { mutableStateOf(listOf<String>()) }
    var customGoal by rememberSaveable { mutableStateOf("") }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    if (datePickerState.selectedDateMillis != null && datePickerState.selectedDateMillis!! <= System.currentTimeMillis()) {
                        dobTimestamp = datePickerState.selectedDateMillis
                        showDatePicker = false
                    }
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState())
            .imePadding()
    ) {
        Text(
            text = "Welcome to Amar-Fit AI",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E1E1E),
            modifier = Modifier.padding(top = 24.dp)
        )
        Text(
            text = "Let's set up your profile to personalize your diet & health plans.",
            fontSize = 14.sp,
            color = Color(0xFF1E1E1E).copy(alpha = 0.7f),
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
        )

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        )
        
        // Birthday Input
        Box(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            OutlinedTextField(
                value = dobString,
                onValueChange = {},
                readOnly = true,
                label = { Text(if (calculatedAge != null) "Birthday (Age: $calculatedAge)" else "Birthday (dd/mm/yyyy)") },
                trailingIcon = { Icon(Icons.Default.DateRange, contentDescription = "Select Date") },
                modifier = Modifier.fillMaxWidth()
            )
            Box(modifier = Modifier.matchParentSize().clickable { showDatePicker = true })
        }

        // Gender Dropdown
        ExposedDropdownMenuBox(
            expanded = expandedGender,
            onExpandedChange = { expandedGender = !expandedGender },
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            OutlinedTextField(
                value = gender,
                onValueChange = {},
                readOnly = true,
                label = { Text("Gender") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedGender) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = expandedGender,
                onDismissRequest = { expandedGender = false }
            ) {
                genderOptions.forEach { selectionOption ->
                    DropdownMenuItem(
                        text = { Text(selectionOption) },
                        onClick = {
                            gender = selectionOption
                            expandedGender = false
                        }
                    )
                }
            }
        }

        // Height
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedTextField(
                value = heightFeet,
                onValueChange = { heightFeet = it },
                label = { Text("Feet") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                isError = heightError != null
            )
            OutlinedTextField(
                value = heightInches,
                onValueChange = { heightInches = it },
                label = { Text("Inch") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                isError = heightError != null
            )
        }
        if (heightError != null) {
            Text(text = heightError!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp, bottom = 8.dp))
        } else {
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Weight
        OutlinedTextField(
            value = weight,
            onValueChange = { weight = it },
            label = { Text("Weight (kg)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            isError = weightError != null
        )
        if (weightError != null) {
            Text(text = weightError!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp, bottom = 8.dp))
        } else {
            Spacer(modifier = Modifier.height(16.dp))
        }

        Text("Dietary Restrictions", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp, bottom = 8.dp), color = Color(0xFF1E1E1E))
        FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            dietOptions.forEach { option ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = selectedDietsList.contains(option),
                        onCheckedChange = { checked ->
                            val current = selectedDietsList.toMutableSet()
                            if (checked) {
                                if (option == "None") current.clear() else current.remove("None")
                                current.add(option)
                            } else {
                                current.remove(option)
                            }
                            selectedDietsList = current.toList()
                        }
                    )
                    Text(option, fontSize = 14.sp, color = Color(0xFF1E1E1E))
                }
            }
        }

        if (selectedDietsList.contains("Other")) {
            OutlinedTextField(
                value = customDiet,
                onValueChange = { customDiet = it },
                label = { Text("Specify other diet") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text("Health Goals", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp, bottom = 8.dp), color = Color(0xFF1E1E1E))
        FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            goalOptions.forEach { option ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = selectedGoalsList.contains(option),
                        onCheckedChange = { checked ->
                            val current = selectedGoalsList.toMutableSet()
                            if (checked) current.add(option) else current.remove(option)
                            selectedGoalsList = current.toList()
                        }
                    )
                    Text(option, fontSize = 14.sp, color = Color(0xFF1E1E1E))
                }
            }
        }

        if (selectedGoalsList.contains("Other")) {
            OutlinedTextField(
                value = customGoal,
                onValueChange = { customGoal = it },
                label = { Text("Specify other goal") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            )
        }

        if (saveError != null) {
            Text(text = saveError!!, color = MaterialTheme.colorScheme.error, fontSize = 14.sp, modifier = Modifier.padding(vertical = 8.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = {
                if (isSaving) return@Button
                
                heightError = null
                weightError = null
                saveError = null
                
                val ft = heightFeet.toIntOrNull()
                val insStr = heightInches.trim()
                val ins = if (insStr.isEmpty()) 0 else insStr.toIntOrNull()
                
                if (ft == null || ft <= 0) {
                    heightError = "Invalid feet"
                } else if (ins == null || ins !in 0..11) {
                    heightError = "Inches must be between 0 and 11"
                }

                val kg = weight.toFloatOrNull()
                if (kg == null || kg <= 0f) {
                    weightError = "Enter a valid weight in kg"
                }

                if (name.isNotBlank() && dobLocalDate != null && heightError == null && weightError == null && kg != null && ft != null && ins != null) {
                    isSaving = true
                    
                    val finalDiets = selectedDietsList.toMutableSet()
                    if (finalDiets.contains("Other") && customDiet.isNotBlank()) {
                        finalDiets.remove("Other")
                        finalDiets.add(customDiet)
                    }
                    val finalGoals = selectedGoalsList.toMutableSet()
                    if (finalGoals.contains("Other") && customGoal.isNotBlank()) {
                        finalGoals.remove("Other")
                        finalGoals.add(customGoal)
                    }
                    
                    val calculatedHeightCm = (ft * 30.48f) + (ins * 2.54f)
                    val dateOfBirthStr = dobLocalDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                    
                    val profile = UserProfile(
                        name = name,
                        age = calculatedAge ?: 0,
                        onboardingCompleted = true,
                        dateOfBirth = dateOfBirthStr,
                        gender = gender,
                        heightCm = calculatedHeightCm,
                        weightKg = kg,
                        dietaryRestrictions = if (finalDiets.isEmpty()) "None" else finalDiets.joinToString(", "),
                        healthGoals = if (finalGoals.isEmpty()) "Stay healthy" else finalGoals.joinToString(", "),
                        dailyCalorieLimit = 2000,
                        dailyWaterLimitLiters = 3.0f,
                        currentStreak = 1
                    )
                    
                    coroutineScope.launch {
                        val success = viewModel.saveProfile(profile)
                        isSaving = false
                        if (success) {
                            onComplete()
                        } else {
                            saveError = "Failed to save profile. Please try again."
                        }
                    }
                } else {
                    if (name.isBlank()) saveError = "Name cannot be empty"
                    else if (dobLocalDate == null) saveError = "Please select a valid birthday"
                }
            },
            enabled = !isSaving,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B4528)),
            shape = RoundedCornerShape(16.dp)
        ) {
            if (isSaving) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text("Complete Setup", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}
