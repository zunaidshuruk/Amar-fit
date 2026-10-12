package com.example.presentation.profile

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.health.HealthGoalCalculator
import com.example.presentation.viewmodel.ShasthoViewModel
import com.example.ui.components.HeightWeightPickerDialog
import com.example.ui.components.PickerMode
import com.example.util.formatHeight
import com.example.util.formatWeight
import com.google.firebase.auth.FirebaseAuth
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ShasthoViewModel,
    onNavigateBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHealthGoals: () -> Unit
) {
    val profile by viewModel.userProfile.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isEditing by remember { mutableStateOf(false) }

    var name by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("70.0") }
    var height by remember { mutableStateOf("170.0") }
    var waist by remember { mutableStateOf("0.0") }
    var neck by remember { mutableStateOf("0.0") }
    var hip by remember { mutableStateOf("0.0") }
    var bodyFatCalcGender by remember { mutableStateOf("") }
    var activityLevel by remember { mutableStateOf("Moderate") }
    var expandedActivityLevel by remember { mutableStateOf(false) }
    val activityLevelOptions = listOf("Sedentary", "Light", "Moderate", "Active", "Very Active")

    var showWeightDialog by remember { mutableStateOf(false) }
    var showHeightDialog by remember { mutableStateOf(false) }
    var calorieLimit by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }
    var profilePictureUri by remember { mutableStateOf<String?>(null) }
    var showDiscardDialog by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (isEditing) {
            profilePictureUri = uri?.toString()
        }
    }

    LaunchedEffect(profile, isEditing) {
        if (!isEditing && profile != null) {
            name = profile!!.name
            age = profile!!.age.toString()
            weight = profile!!.weightKg.toString()
            height = profile!!.heightCm.toString()
            waist = profile!!.waistCm.toString()
            neck = profile!!.neckCm.toString()
            hip = profile!!.hipCm.toString()
            bodyFatCalcGender = profile!!.bodyFatCalcGender
            activityLevel = profile!!.activityLevel
            calorieLimit = profile!!.dailyCalorieLimit.toString()
            profilePictureUri = profile!!.profilePictureUri
        }
    }

    val hasChanges = remember(
        name, age, weight, height, waist, neck, hip, bodyFatCalcGender,
        activityLevel, calorieLimit, profilePictureUri, profile
    ) {
        profile != null && (
            name != profile!!.name ||
            (age.toIntOrNull() ?: 0) != profile!!.age ||
            (weight.toFloatOrNull() ?: 0f) != profile!!.weightKg ||
            (height.toFloatOrNull() ?: 0f) != profile!!.heightCm ||
            (waist.toFloatOrNull() ?: 0f) != profile!!.waistCm ||
            (neck.toFloatOrNull() ?: 0f) != profile!!.neckCm ||
            (hip.toFloatOrNull() ?: 0f) != profile!!.hipCm ||
            bodyFatCalcGender != profile!!.bodyFatCalcGender ||
            activityLevel != profile!!.activityLevel ||
            (calorieLimit.toIntOrNull() ?: 0) != profile!!.dailyCalorieLimit ||
            profilePictureUri != profile!!.profilePictureUri
        )
    }

    fun attemptCancelEditing() {
        if (hasChanges) {
            showDiscardDialog = true
        } else {
            isEditing = false
        }
    }

    BackHandler(enabled = isEditing) {
        attemptCancelEditing()
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Discard changes?") },
            text = { Text("You have unsaved changes. Are you sure you want to discard them?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardDialog = false
                        profile?.let { p ->
                            name = p.name
                            age = p.age.toString()
                            weight = p.weightKg.toString()
                            height = p.heightCm.toString()
                            waist = p.waistCm.toString()
                            neck = p.neckCm.toString()
                            hip = p.hipCm.toString()
                            bodyFatCalcGender = p.bodyFatCalcGender
                            activityLevel = p.activityLevel
                            calorieLimit = p.dailyCalorieLimit.toString()
                            profilePictureUri = p.profilePictureUri
                        }
                        isEditing = false
                    }
                ) {
                    Text("Discard", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text("Keep editing")
                }
            }
        )
    }

    if (showWeightDialog) {
        HeightWeightPickerDialog(
            mode = PickerMode.WEIGHT,
            initialValue = weight.toFloatOrNull() ?: 70f,
            useImperialUnits = profile?.useImperialUnits ?: false,
            onDismiss = { showWeightDialog = false },
            onConfirm = { kg ->
                weight = kg.toString()
                showWeightDialog = false
            }
        )
    }

    if (showHeightDialog) {
        HeightWeightPickerDialog(
            mode = PickerMode.HEIGHT,
            initialValue = height.toFloatOrNull() ?: 170f,
            useImperialUnits = profile?.useImperialUnits ?: false,
            onDismiss = { showHeightDialog = false },
            onConfirm = { cm ->
                height = cm.toString()
                showHeightDialog = false
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
            .imePadding()
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp, top = 24.dp)
        ) {
            IconButton(onClick = {
                if (isEditing) {
                    attemptCancelEditing()
                } else {
                    onNavigateBack()
                }
            }) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                text = "Profile",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )
            if (!isEditing) {
                OutlinedButton(
                    onClick = {
                        profile?.let { p ->
                            name = p.name
                            age = p.age.toString()
                            weight = p.weightKg.toString()
                            height = p.heightCm.toString()
                            waist = p.waistCm.toString()
                            neck = p.neckCm.toString()
                            hip = p.hipCm.toString()
                            bodyFatCalcGender = p.bodyFatCalcGender
                            activityLevel = p.activityLevel
                            calorieLimit = p.dailyCalorieLimit.toString()
                            profilePictureUri = p.profilePictureUri
                        }
                        isEditing = true
                    },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit Profile",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Edit")
                }
            }
        }

        if (!isEditing) {
            // VIEW MODE
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    val uri = profile?.profilePictureUri
                    if (uri.isNullOrEmpty()) {
                        Text(
                            text = profile?.name?.firstOrNull()?.toString()?.uppercase() ?: "U",
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        var decodedBitmap: androidx.compose.ui.graphics.ImageBitmap? = null
                        if (!uri.startsWith("http") && !uri.startsWith("content://")) {
                            try {
                                val decodedBytes = android.util.Base64.decode(uri, android.util.Base64.DEFAULT)
                                decodedBitmap = android.graphics.BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)?.let { it.asImageBitmap() }
                            } catch (e: Exception) { }
                        }

                        if (decodedBitmap != null) {
                            Image(
                                bitmap = decodedBitmap,
                                contentDescription = "Profile Picture",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            AsyncImage(
                                model = uri,
                                contentDescription = "Profile Picture",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = profile?.name?.ifBlank { "User" } ?: "User",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            val currentUserEmail = FirebaseAuth.getInstance().currentUser?.email
            Text(
                text = currentUserEmail ?: "No email linked",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(20.dp))

            // Card 1: Basics
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Basics",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    ProfileDetailRow("Age", "${profile?.age ?: 0}")
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    ProfileDetailRow("Gender", profile?.gender?.ifBlank { "Not set" } ?: "Not set")
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    ProfileDetailRow("Activity level", profile?.activityLevel?.ifBlank { "Not set" } ?: "Not set")
                }
            }

            // Card 2: Body
            val imperial = profile?.useImperialUnits ?: false
            val formattedHeight = formatHeight(profile?.heightCm ?: 0f, imperial)
            val formattedWeight = formatWeight(profile?.weightKg ?: 0f, imperial)
            val heightM = (profile?.heightCm ?: 0f) / 100f
            val bmiStr = if (heightM > 0f && (profile?.weightKg ?: 0f) > 0f) {
                String.format(Locale.US, "%.1f", (profile?.weightKg ?: 0f) / (heightM * heightM))
            } else "-"
            val bodyFat = profile?.let { HealthGoalCalculator.calculateBodyFatPercent(it) }
            val bodyFatStr = if (bodyFat != null) String.format(Locale.US, "%.1f%%", bodyFat) else "Add waist and neck"

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Body",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    ProfileDetailRow("Height", formattedHeight)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    ProfileDetailRow("Weight", formattedWeight)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    ProfileDetailRow("BMI", bmiStr)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    ProfileDetailRow("Body fat %", bodyFatStr)
                    if ((profile?.waistCm ?: 0f) > 0f) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        ProfileDetailRow("Waist", "${profile?.waistCm} cm")
                    }
                    if ((profile?.neckCm ?: 0f) > 0f) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        ProfileDetailRow("Neck", "${profile?.neckCm} cm")
                    }
                    if ((profile?.hipCm ?: 0f) > 0f) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        ProfileDetailRow("Hip", "${profile?.hipCm} cm")
                    }
                }
            }

            // Card 3: Daily goals
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Daily goals",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        TextButton(onClick = onOpenHealthGoals) {
                            Text("Edit goals")
                        }
                    }
                    ProfileDetailRow("Calorie limit", "${profile?.dailyCalorieLimit ?: 0} kcal")
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    ProfileDetailRow("Step goal", "${profile?.stepGoal ?: 10000}")
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    ProfileDetailRow("Sleep goal", "${profile?.sleepGoalHours ?: 8.0} hours")
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    ProfileDetailRow("Water goal", "${profile?.dailyWaterLimitLiters ?: 2.5f} litres")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onOpenSettings,
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("App settings")
            }
        } else {
            // EDIT MODE
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                    .clickable { launcher.launch("image/*") },
                                contentAlignment = Alignment.Center
                            ) {
                                if (profilePictureUri.isNullOrEmpty()) {
                                    Icon(
                                        Icons.Default.CameraAlt,
                                        contentDescription = "Select Profile Picture",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(40.dp)
                                    )
                                } else {
                                    var decodedBitmap: androidx.compose.ui.graphics.ImageBitmap? = null
                                    if (!profilePictureUri!!.startsWith("http") && !profilePictureUri!!.startsWith("content://")) {
                                        try {
                                            val decodedBytes = android.util.Base64.decode(profilePictureUri, android.util.Base64.DEFAULT)
                                            decodedBitmap = android.graphics.BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)?.let { it.asImageBitmap() }
                                        } catch (e: Exception) { }
                                    }

                                    if (decodedBitmap != null) {
                                        Image(
                                            bitmap = decodedBitmap,
                                            contentDescription = "Profile Picture",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        AsyncImage(
                                            model = profilePictureUri,
                                            contentDescription = "Profile Picture",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tap to change photo",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Name") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = age,
                            onValueChange = { age = it },
                            label = { Text("Age") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = calorieLimit,
                            onValueChange = { calorieLimit = it },
                            label = { Text("Daily Calories") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = formatHeight(height.toFloatOrNull() ?: 170f, profile?.useImperialUnits ?: false),
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Height") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Box(modifier = Modifier.matchParentSize().clickable { showHeightDialog = true })
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = formatWeight(weight.toFloatOrNull() ?: 70f, profile?.useImperialUnits ?: false),
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Weight") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Box(modifier = Modifier.matchParentSize().clickable { showWeightDialog = true })
                        }
                    }

                    Text("Body Measurements (for Body Fat %)", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = waist,
                            onValueChange = { waist = it },
                            label = { Text("Waist (cm)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = neck,
                            onValueChange = { neck = it },
                            label = { Text("Neck (cm)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    val effectiveGenderLower = profile?.gender?.trim()?.lowercase() ?: ""
                    val needsHip = effectiveGenderLower == "female" || (effectiveGenderLower != "male" && effectiveGenderLower != "female" && bodyFatCalcGender.equals("female", ignoreCase = true))
                    if (needsHip) {
                        OutlinedTextField(
                            value = hip,
                            onValueChange = { hip = it },
                            label = { Text("Hip (cm)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (effectiveGenderLower != "male" && effectiveGenderLower != "female") {
                        Text("Calculate my body fat using:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = bodyFatCalcGender.equals("male", ignoreCase = true),
                                onClick = { bodyFatCalcGender = "male" },
                                label = { Text("Male formula") }
                            )
                            FilterChip(
                                selected = bodyFatCalcGender.equals("female", ignoreCase = true),
                                onClick = { bodyFatCalcGender = "female" },
                                label = { Text("Female formula") }
                            )
                        }
                    }

                    ExposedDropdownMenuBox(
                        expanded = expandedActivityLevel,
                        onExpandedChange = { expandedActivityLevel = !expandedActivityLevel },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    ) {
                        OutlinedTextField(
                            value = activityLevel,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Activity Level") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedActivityLevel) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedActivityLevel,
                            onDismissRequest = { expandedActivityLevel = false }
                        ) {
                            activityLevelOptions.forEach { selectionOption ->
                                DropdownMenuItem(
                                    text = { Text(selectionOption) },
                                    onClick = {
                                        activityLevel = selectionOption
                                        expandedActivityLevel = false
                                    }
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.userProfile.value?.let { p ->
                                    coroutineScope.launch {
                                        isSaving = true
                                        var finalPhotoUrl = profilePictureUri
                                        if (finalPhotoUrl != null && finalPhotoUrl.startsWith("content://")) {
                                            try {
                                                val uri = android.net.Uri.parse(finalPhotoUrl)
                                                val inputStream = context.contentResolver.openInputStream(uri)
                                                val originalBitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                                                inputStream?.close()

                                                if (originalBitmap != null) {
                                                    val maxSize = 300
                                                    val ratio = maxSize.toFloat() / Math.max(originalBitmap.width, originalBitmap.height)
                                                    val resizedBitmap = if (ratio < 1) {
                                                        android.graphics.Bitmap.createScaledBitmap(originalBitmap, (originalBitmap.width * ratio).toInt(), (originalBitmap.height * ratio).toInt(), true)
                                                    } else {
                                                        originalBitmap
                                                    }

                                                    var quality = 70
                                                    var base64String = ""
                                                    val out = java.io.ByteArrayOutputStream()

                                                    do {
                                                        out.reset()
                                                        resizedBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, quality, out)
                                                        val bytes = out.toByteArray()
                                                        base64String = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                                                        quality -= 10
                                                    } while (bytes.size > 300 * 1024 && quality > 10)

                                                    if (base64String.length * 2 > 800 * 1024) {
                                                        throw Exception("Image is still too large after compression")
                                                    }

                                                    finalPhotoUrl = base64String
                                                    profilePictureUri = finalPhotoUrl
                                                } else {
                                                    throw Exception("Could not decode image")
                                                }
                                            } catch(e: Exception) {
                                                e.printStackTrace()
                                                Toast.makeText(context, "Failed to compress image: ${e.message}", Toast.LENGTH_LONG).show()
                                                finalPhotoUrl = p.profilePictureUri
                                                profilePictureUri = finalPhotoUrl
                                            }
                                        }
                                        val updated = p.copy(
                                            name = name,
                                            age = age.toIntOrNull() ?: p.age,
                                            weightKg = weight.toFloatOrNull() ?: p.weightKg,
                                            heightCm = height.toFloatOrNull() ?: p.heightCm,
                                            waistCm = waist.toFloatOrNull() ?: p.waistCm,
                                            neckCm = neck.toFloatOrNull() ?: p.neckCm,
                                            hipCm = hip.toFloatOrNull() ?: p.hipCm,
                                            bodyFatCalcGender = bodyFatCalcGender,
                                            activityLevel = activityLevel,
                                            dailyCalorieLimit = calorieLimit.toIntOrNull() ?: p.dailyCalorieLimit,
                                            profilePictureUri = finalPhotoUrl
                                        )
                                        viewModel.saveProfile(updated)
                                        isSaving = false
                                        isEditing = false
                                        Toast.makeText(context, "Profile updated", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f).height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            enabled = !isSaving
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                            } else {
                                Text("Save")
                            }
                        }

                        OutlinedButton(
                            onClick = { attemptCancelEditing() },
                            modifier = Modifier.weight(1f).height(50.dp),
                            enabled = !isSaving
                        ) {
                            Text("Cancel")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
private fun ProfileDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
    }
}
