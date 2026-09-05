package com.example.presentation.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.UserProfile
import com.example.presentation.viewmodel.ShasthoViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(viewModel: ShasthoViewModel, onComplete: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("Male") }
    var height by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    
    val dietOptions = listOf("None", "Halal", "Vegan", "Vegetarian", "Keto", "Gluten-Free", "Other")
    var selectedDiets by remember { mutableStateOf(setOf<String>()) }
    var customDiet by remember { mutableStateOf("") }

    val goalOptions = listOf("Lose weight", "Build muscle", "Stay healthy", "Improve sleep", "Other")
    var selectedGoals by remember { mutableStateOf(setOf<String>()) }
    var customGoal by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()).imePadding()
    ) {
        Text(
            text = "Welcome to Shastho AI",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Emerald900,
            modifier = Modifier.padding(top = 24.dp)
        )
        Text(
            text = "Let's set up your profile to personalize your diet & health plans.",
            fontSize = 14.sp,
            color = Slate500,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
        )

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        )

        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = age,
                onValueChange = { age = it },
                label = { Text("Age") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = gender,
                onValueChange = { gender = it },
                label = { Text("Gender") },
                modifier = Modifier.weight(1f)
            )
        }

        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = height,
                onValueChange = { height = it },
                label = { Text("Height (cm)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = weight,
                onValueChange = { weight = it },
                label = { Text("Weight (kg)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
        }

        Text("Dietary Restrictions", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp, bottom = 8.dp))
        FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            dietOptions.forEach { option ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = selectedDiets.contains(option),
                        onCheckedChange = { checked ->
                            if (checked) {
                                if (option == "None") {
                                    selectedDiets = setOf("None")
                                } else {
                                    val newSet = selectedDiets.toMutableSet()
                                    newSet.remove("None")
                                    newSet.add(option)
                                    selectedDiets = newSet
                                }
                            } else {
                                selectedDiets = selectedDiets.minus(option)
                            }
                        }
                    )
                    Text(option, fontSize = 14.sp)
                }
            }
        }
        if (selectedDiets.contains("Other")) {
            OutlinedTextField(
                value = customDiet,
                onValueChange = { customDiet = it },
                label = { Text("Specify other diet") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text("Health Goals", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp, bottom = 8.dp))
        FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            goalOptions.forEach { option ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = selectedGoals.contains(option),
                        onCheckedChange = { checked ->
                            if (checked) selectedGoals = selectedGoals.plus(option)
                            else selectedGoals = selectedGoals.minus(option)
                        }
                    )
                    Text(option, fontSize = 14.sp)
                }
            }
        }
        if (selectedGoals.contains("Other")) {
            OutlinedTextField(
                value = customGoal,
                onValueChange = { customGoal = it },
                label = { Text("Specify other goal") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (name.isNotBlank() && age.isNotBlank() && height.isNotBlank() && weight.isNotBlank()) {
                    val finalDiets = selectedDiets.toMutableSet()
                    if (finalDiets.contains("Other") && customDiet.isNotBlank()) {
                        finalDiets.remove("Other")
                        finalDiets.add(customDiet)
                    }
                    val finalGoals = selectedGoals.toMutableSet()
                    if (finalGoals.contains("Other") && customGoal.isNotBlank()) {
                        finalGoals.remove("Other")
                        finalGoals.add(customGoal)
                    }

                    val profile = UserProfile(
                        name = name,
                        age = age.toIntOrNull() ?: 25,
                        gender = gender,
                        heightCm = height.toFloatOrNull() ?: 170f,
                        weightKg = weight.toFloatOrNull() ?: 70f,
                        dietaryRestrictions = if (finalDiets.isEmpty()) "None" else finalDiets.joinToString(", "),
                        healthGoals = if (finalGoals.isEmpty()) "Stay healthy" else finalGoals.joinToString(", "),
                        dailyCalorieLimit = 2000,
                        dailyWaterLimitLiters = 3.0f,
                        currentStreak = 1
                    )
                    viewModel.saveProfile(profile)
                    onComplete()
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Complete Setup", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(100.dp))
    }
}
