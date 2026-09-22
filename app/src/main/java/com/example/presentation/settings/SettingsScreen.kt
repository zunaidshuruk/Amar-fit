package com.example.presentation.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.presentation.viewmodel.ShasthoViewModel
import com.example.ui.theme.*
import com.example.data.health.HealthConnectManager

import androidx.compose.ui.platform.LocalContext
import androidx.health.connect.client.PermissionController
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import com.google.android.gms.auth.api.identity.Identity
import com.example.data.repository.DriveBackupPayload
import com.example.util.formatHeight
import com.example.util.formatWeight


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: ShasthoViewModel,
    onNavigateBack: () -> Unit = {},
    onLogout: () -> Unit = {},
    onNavigateToHealthGoals: () -> Unit = {}
) {
    val profile by viewModel.userProfile.collectAsState()
    val isDark = true

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

    if (showWeightDialog) {
        com.example.ui.components.HeightWeightPickerDialog(
            mode = com.example.ui.components.PickerMode.WEIGHT,
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
        com.example.ui.components.HeightWeightPickerDialog(
            mode = com.example.ui.components.PickerMode.HEIGHT,
            initialValue = height.toFloatOrNull() ?: 170f,
            useImperialUnits = profile?.useImperialUnits ?: false,
            onDismiss = { showHeightDialog = false },
            onConfirm = { cm ->
                height = cm.toString()
                showHeightDialog = false
            }
        )
    }

    var calorieLimit by remember { mutableStateOf("") }
    var showSavedMessage by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showPasswordReauthDialog by remember { mutableStateOf(false) }
    var reauthPassword by remember { mutableStateOf("") }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var currentPasswordInput by remember { mutableStateOf("") }
    var newPasswordInput by remember { mutableStateOf("") }
    var confirmPasswordInput by remember { mutableStateOf("") }
    var showChangeEmailDialog by remember { mutableStateOf(false) }
    var currentPasswordForEmailChange by remember { mutableStateOf("") }
    var newEmailInput by remember { mutableStateOf("") }


    var isDarkMode by remember { mutableStateOf(false) }
    var useImperialUnits by remember { mutableStateOf(false) }
    var notificationsEnabled by remember { mutableStateOf(true) }
    var remindersEnabled by remember { mutableStateOf(true) }

    var profilePictureUri by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isHealthConnectAvailable by remember { mutableStateOf(HealthConnectManager.isAvailable(context)) }

    var showMissingPermissionsDialog by remember { mutableStateOf(false) }
    var missingPermissionCategories by remember { mutableStateOf<List<String>>(emptyList()) }

    val requestPermissionLauncher = rememberLauncherForActivityResult(PermissionController.createRequestPermissionResultContract()) { granted ->
        val missingPermissions = HealthConnectManager.REQUIRED_PERMISSIONS.filter { it !in granted }
        val missingCount = missingPermissions.size

        if (missingCount > 0) {
            missingPermissionCategories = missingPermissions.map { HealthConnectManager.getPermissionDisplayName(it) }.distinct()
            showMissingPermissionsDialog = true
            if (granted.isNotEmpty()) {
                viewModel.syncWithHealthConnect(context)
            }
        } else {
            Toast.makeText(context, "Health Connect connected!", Toast.LENGTH_SHORT).show()
            viewModel.syncWithHealthConnect(context)
        }
    }


    val launcher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri? ->
        profilePictureUri = uri?.toString()
    }

    var isBackingUp by remember { mutableStateOf(false) }
    var isRestoring by remember { mutableStateOf(false) }
    var showRestoreConfirmDialog by remember { mutableStateOf(false) }
    var pendingRestorePayload by remember { mutableStateOf<DriveBackupPayload?>(null) }

    val driveAuthLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK && result.data != null) {
            try {
                val authResult = Identity.getAuthorizationClient(context).getAuthorizationResultFromIntent(result.data)
                val token = authResult.accessToken
                if (!token.isNullOrBlank()) {
                    viewModel.performDriveBackupWithToken(token) { success, msg ->
                        isBackingUp = false
                        Toast.makeText(context, if (success) "Backup saved to Google Drive" else msg, Toast.LENGTH_SHORT).show()
                    }
                } else {
                    isBackingUp = false
                    Toast.makeText(context, "Google Drive authorization was not granted", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                isBackingUp = false
                Toast.makeText(context, "Failed to get authorization: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            isBackingUp = false
            Toast.makeText(context, "Google Drive authorization cancelled", Toast.LENGTH_SHORT).show()
        }
    }

    val driveRestoreAuthLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK && result.data != null) {
            try {
                val authResult = Identity.getAuthorizationClient(context).getAuthorizationResultFromIntent(result.data)
                val token = authResult.accessToken
                if (!token.isNullOrBlank()) {
                    viewModel.performFetchDriveBackupWithToken(token) { payload, msg ->
                        isRestoring = false
                        if (payload != null) {
                            pendingRestorePayload = payload
                            showRestoreConfirmDialog = true
                        } else {
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    isRestoring = false
                    Toast.makeText(context, "Google Drive authorization was not granted", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                isRestoring = false
                Toast.makeText(context, "Failed to get authorization: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            isRestoring = false
            Toast.makeText(context, "Google Drive authorization cancelled", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(profile) {
        if (profile != null) {
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
            isDarkMode = profile!!.isDarkMode
            useImperialUnits = profile!!.useImperialUnits
            notificationsEnabled = profile!!.notificationsEnabled
            remindersEnabled = profile!!.remindersEnabled
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()).imePadding()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp, top = 24.dp)
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
            }
            Text(
                text = "Profile & Settings",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Profile Picture Selector
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
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
                            Icon(Icons.Default.CameraAlt, contentDescription = "Select Profile Picture", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(40.dp))
                        } else {
                            var decodedBitmap: androidx.compose.ui.graphics.ImageBitmap? = null
                            if (!profilePictureUri!!.startsWith("http") && !profilePictureUri!!.startsWith("content://")) {
                                try {
                                    val decodedBytes = android.util.Base64.decode(profilePictureUri, android.util.Base64.DEFAULT)
                                    decodedBitmap = android.graphics.BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)?.let { it.asImageBitmap() }
                                } catch (e: Exception) { }
                            }
                            
                            if (decodedBitmap != null) {
                                androidx.compose.foundation.Image(
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

                Button(
                    onClick = {
                        profile?.let {
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
                                        finalPhotoUrl = it.profilePictureUri
                                        profilePictureUri = finalPhotoUrl
                                    }
                                }
                                val updated = it.copy(
                                    name = name,
                                    age = age.toIntOrNull() ?: it.age,
                                    weightKg = weight.toFloatOrNull() ?: it.weightKg,
                                    heightCm = height.toFloatOrNull() ?: it.heightCm,
                                    waistCm = waist.toFloatOrNull() ?: it.waistCm,
                                    neckCm = neck.toFloatOrNull() ?: it.neckCm,
                                    hipCm = hip.toFloatOrNull() ?: it.hipCm,
                                    bodyFatCalcGender = bodyFatCalcGender,
                                    activityLevel = activityLevel,
                                    dailyCalorieLimit = calorieLimit.toIntOrNull() ?: it.dailyCalorieLimit,
                                    profilePictureUri = finalPhotoUrl,
                                    isDarkMode = isDarkMode,
                                    useImperialUnits = useImperialUnits,
                                    notificationsEnabled = notificationsEnabled,
                                    remindersEnabled = remindersEnabled
                                )
                                viewModel.saveProfile(updated)
                                showSavedMessage = true
                                isSaving = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    enabled = !isSaving
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                    } else {
                        Text("Save Profile")
                    }
                }

                if (showSavedMessage) {
                    Text(
                        text = "Profile updated successfully!",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "App Settings",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Use Imperial Units (ft/in, lbs)", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                    Switch(
                        checked = useImperialUnits,
                        onCheckedChange = { useImperialUnits = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary, checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Push Notifications", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                    Switch(
                        checked = notificationsEnabled,
                        onCheckedChange = { notificationsEnabled = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary, checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Daily Reminders", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                    Switch(
                        checked = remindersEnabled,
                        onCheckedChange = { remindersEnabled = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary, checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                    )
                }
            }
        }

        
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Health Connect", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onBackground)
                        Text("Sync steps, sleep & vitals", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Button(
                        onClick = {
                            if (isHealthConnectAvailable) {
                                requestPermissionLauncher.launch(HealthConnectManager.REQUIRED_PERMISSIONS)
                            } else {
                                Toast.makeText(context, "Health Connect is not available on this device", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Connect")
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                ) {
                    Text("Google Drive Backup", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onBackground)
                    Text("Save a copy of your data or restore from your Google Drive", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                isBackingUp = true
                                viewModel.backupToDrive(
                                    context = context,
                                    onRequiresResolution = { pendingIntent ->
                                        try {
                                            val intentSenderRequest = IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                                            driveAuthLauncher.launch(intentSenderRequest)
                                        } catch (e: Exception) {
                                            isBackingUp = false
                                            Toast.makeText(context, "Could not start authorization: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    onResult = { success, msg ->
                                        isBackingUp = false
                                        Toast.makeText(context, if (success) "Backup saved to Google Drive" else msg, Toast.LENGTH_SHORT).show()
                                    }
                                )
                            },
                            enabled = !isBackingUp && !isRestoring,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            if (isBackingUp) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Backup Now")
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                isRestoring = true
                                viewModel.fetchDriveBackupForRestore(
                                    context = context,
                                    onRequiresResolution = { pendingIntent ->
                                        try {
                                            val intentSenderRequest = IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                                            driveRestoreAuthLauncher.launch(intentSenderRequest)
                                        } catch (e: Exception) {
                                            isRestoring = false
                                            Toast.makeText(context, "Could not start authorization: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    onResult = { payload, msg ->
                                        isRestoring = false
                                        if (payload != null) {
                                            pendingRestorePayload = payload
                                            showRestoreConfirmDialog = true
                                        } else {
                                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )
                            },
                            enabled = !isBackingUp && !isRestoring,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                        ) {
                            if (isRestoring) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Restore Now")
                            }
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToHealthGoals() }
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Health Goals", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onBackground)
                        Text("Auto-calculated from your profile", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Health Goals",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                val currentUserProviders = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.providerData?.map { it.providerId } ?: emptyList()
                if (currentUserProviders.contains("password")) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showChangePasswordDialog = true }
                            .padding(vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Change Password", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onBackground)
                            Text("Update your account password", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Change Password",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showChangeEmailDialog = true }
                            .padding(vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Change Email", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onBackground)
                            Text("Update your account email", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Change Email",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = { showLogoutDialog = true },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) {
            Text("Log Out")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { showDeleteDialog = true },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Text("Delete Account", color = MaterialTheme.colorScheme.onError)
        }
        Spacer(modifier = Modifier.height(100.dp))
    }

    if (showRestoreConfirmDialog && pendingRestorePayload != null) {
        val backupDate = pendingRestorePayload?.backupDate ?: "unknown date"
        AlertDialog(
            onDismissRequest = {
                showRestoreConfirmDialog = false
                pendingRestorePayload = null
            },
            title = { Text("Restore Backup?") },
            text = {
                Text("This will replace your local data with the backup from $backupDate — continue?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        val payloadToApply = pendingRestorePayload
                        showRestoreConfirmDialog = false
                        pendingRestorePayload = null
                        if (payloadToApply != null) {
                            isRestoring = true
                            viewModel.applyDriveBackup(payloadToApply) { success, msg ->
                                isRestoring = false
                                Toast.makeText(context, if (success) "Restore complete" else msg, Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showRestoreConfirmDialog = false
                        pendingRestorePayload = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Log Out") },
            text = { Text("Are you sure you want to log out?") },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    viewModel.logout { onLogout() }
                }) {
                    Text("Log Out")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("Cancel") }
            }
        )
    }
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Account") },
            text = { Text("This will permanently delete your account and all data. Are you sure?") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    viewModel.deleteAccount { result ->
                        when (result) {
                            is com.example.data.repository.DeleteAccountResult.Success -> {
                                onLogout()
                            }
                            is com.example.data.repository.DeleteAccountResult.NeedsReauth -> {
                                val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
                                val user = auth.currentUser
                                val providers = user?.providerData?.map { it.providerId } ?: emptyList()
                                if (providers.contains("google.com")) {
                                    coroutineScope.launch {
                                        try {
                                            val googleIdOption = com.google.android.libraries.identity.googleid.GetGoogleIdOption.Builder()
                                                .setFilterByAuthorizedAccounts(false)
                                                .setServerClientId(context.getString(com.example.R.string.default_web_client_id))
                                                .build()
                                            val request = androidx.credentials.GetCredentialRequest.Builder()
                                                .addCredentialOption(googleIdOption)
                                                .build()
                                            val credentialManager = androidx.credentials.CredentialManager.create(context)
                                            val credResult = credentialManager.getCredential(context = context, request = request)
                                            if (credResult.credential is androidx.credentials.CustomCredential &&
                                                credResult.credential.type == com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                                            ) {
                                                val googleIdTokenCredential = com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.createFrom(credResult.credential.data)
                                                val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
                                                user?.reauthenticate(credential)?.await()
                                                viewModel.deleteAccount { retryResult ->
                                                    when (retryResult) {
                                                        is com.example.data.repository.DeleteAccountResult.Success -> onLogout()
                                                        is com.example.data.repository.DeleteAccountResult.NeedsReauth -> Toast.makeText(context, "Reauthentication succeeded, but deletion still requires recent login.", Toast.LENGTH_LONG).show()
                                                        is com.example.data.repository.DeleteAccountResult.Error -> Toast.makeText(context, retryResult.message, Toast.LENGTH_LONG).show()
                                                    }
                                                }
                                            }
                                        } catch (e: androidx.credentials.exceptions.GetCredentialCancellationException) {
                                            // Cancelled, do nothing
                                        } catch (e: Exception) {
                                            Toast.makeText(context, e.message ?: "Google reauthentication failed", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                } else {
                                    showPasswordReauthDialog = true
                                }
                            }
                            is com.example.data.repository.DeleteAccountResult.Error -> {
                                Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showPasswordReauthDialog) {
        AlertDialog(
            onDismissRequest = { 
                showPasswordReauthDialog = false
                reauthPassword = ""
            },
            title = { Text("Re-authentication Required") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Please enter your current password to confirm account deletion.")
                    OutlinedTextField(
                        value = reauthPassword,
                        onValueChange = { reauthPassword = it },
                        label = { Text("Password") },
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    coroutineScope.launch {
                        try {
                            val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
                            val user = auth.currentUser
                            val email = user?.email
                            if (!email.isNullOrBlank() && reauthPassword.isNotBlank()) {
                                val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(email, reauthPassword)
                                user.reauthenticate(credential).await()
                                showPasswordReauthDialog = false
                                reauthPassword = ""
                                viewModel.deleteAccount { retryResult ->
                                    when (retryResult) {
                                        is com.example.data.repository.DeleteAccountResult.Success -> onLogout()
                                        is com.example.data.repository.DeleteAccountResult.NeedsReauth -> Toast.makeText(context, "Reauthentication succeeded, but deletion still requires recent login.", Toast.LENGTH_LONG).show()
                                        is com.example.data.repository.DeleteAccountResult.Error -> Toast.makeText(context, retryResult.message, Toast.LENGTH_LONG).show()
                                    }
                                }
                            } else {
                                Toast.makeText(context, "Please enter your password", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, e.message ?: "Reauthentication failed", Toast.LENGTH_LONG).show()
                        }
                    }
                }) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPasswordReauthDialog = false
                    reauthPassword = ""
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showChangePasswordDialog) {
        AlertDialog(
            onDismissRequest = {
                showChangePasswordDialog = false
                currentPasswordInput = ""
                newPasswordInput = ""
                confirmPasswordInput = ""
            },
            title = { Text("Change Password") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = currentPasswordInput,
                        onValueChange = { currentPasswordInput = it },
                        label = { Text("Current Password") },
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newPasswordInput,
                        onValueChange = { newPasswordInput = it },
                        label = { Text("New Password") },
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = confirmPasswordInput,
                        onValueChange = { confirmPasswordInput = it },
                        label = { Text("Confirm New Password") },
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    when {
                        currentPasswordInput.isBlank() || newPasswordInput.isBlank() -> {
                            Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                        }
                        newPasswordInput.length < 6 -> {
                            Toast.makeText(context, "New password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                        }
                        newPasswordInput != confirmPasswordInput -> {
                            Toast.makeText(context, "New passwords do not match", Toast.LENGTH_SHORT).show()
                        }
                        else -> {
                            coroutineScope.launch {
                                try {
                                    val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
                                    val user = auth.currentUser
                                    val email = user?.email
                                    if (!email.isNullOrBlank()) {
                                        val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(email, currentPasswordInput)
                                        user.reauthenticate(credential).await()
                                        user.updatePassword(newPasswordInput).await()
                                        showChangePasswordDialog = false
                                        currentPasswordInput = ""
                                        newPasswordInput = ""
                                        confirmPasswordInput = ""
                                        Toast.makeText(context, "Password changed successfully", Toast.LENGTH_LONG).show()
                                    } else {
                                        Toast.makeText(context, "No email associated with this account", Toast.LENGTH_LONG).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, e.message ?: "Failed to change password. Check your current password.", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }
                }) {
                    Text("Change Password")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showChangePasswordDialog = false
                    currentPasswordInput = ""
                    newPasswordInput = ""
                    confirmPasswordInput = ""
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showChangeEmailDialog) {
        AlertDialog(
            onDismissRequest = {
                showChangeEmailDialog = false
                currentPasswordForEmailChange = ""
                newEmailInput = ""
            },
            title = { Text("Change Email") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = currentPasswordForEmailChange,
                        onValueChange = { currentPasswordForEmailChange = it },
                        label = { Text("Current Password") },
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newEmailInput,
                        onValueChange = { newEmailInput = it },
                        label = { Text("New Email") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val trimmedNewEmail = newEmailInput.trim()
                    when {
                        currentPasswordForEmailChange.isBlank() || trimmedNewEmail.isBlank() -> {
                            Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                        }
                        !"^[A-Za-z0-9+_.-]+@(.+)$".toRegex().matches(trimmedNewEmail) -> {
                            Toast.makeText(context, "Please enter a valid email address", Toast.LENGTH_SHORT).show()
                        }
                        trimmedNewEmail.equals(com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.email, ignoreCase = true) -> {
                            Toast.makeText(context, "New email must be different from your current email", Toast.LENGTH_SHORT).show()
                        }
                        else -> {
                            coroutineScope.launch {
                                try {
                                    val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
                                    val user = auth.currentUser
                                    val email = user?.email ?: ""
                                    val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(email, currentPasswordForEmailChange)
                                    user?.reauthenticate(credential)?.await()
                                    user?.verifyBeforeUpdateEmail(trimmedNewEmail)?.await()
                                    showChangeEmailDialog = false
                                    currentPasswordForEmailChange = ""
                                    newEmailInput = ""
                                    Toast.makeText(context, "Verification email sent to $trimmedNewEmail — check your inbox to confirm the change", Toast.LENGTH_LONG).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, e.message ?: "Failed to update email. Check your current password.", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }
                }) {
                    Text("Change Email")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showChangeEmailDialog = false
                    currentPasswordForEmailChange = ""
                    newEmailInput = ""
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showMissingPermissionsDialog) {
        AlertDialog(
            onDismissRequest = { showMissingPermissionsDialog = false },
            title = {
                Text(
                    text = "Health Connect Permissions",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Some permissions were not granted or may need to be enabled manually in Health Connect settings:",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (missingPermissionCategories.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                missingPermissionCategories.forEach { category ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("• ", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        Text(
                                            category,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Text(
                        text = "Note: Some record types (such as HRV, Skin Temperature, or Mindfulness) may be unavailable on this device if not supported by the installed Health Connect version or hardware.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val launchIntent = context.packageManager.getLaunchIntentForPackage("com.google.android.apps.healthdata")
                        if (launchIntent != null) {
                            context.startActivity(launchIntent)
                        } else {
                            Toast.makeText(context, "Look under Permissions > Health Connect", Toast.LENGTH_LONG).show()
                            val appSettingsIntent = android.content.Intent(
                                android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                            ).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(appSettingsIntent)
                        }
                        showMissingPermissionsDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Open Health Connect Settings", color = MaterialTheme.colorScheme.onPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showMissingPermissionsDialog = false }) {
                    Text("Dismiss", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}
