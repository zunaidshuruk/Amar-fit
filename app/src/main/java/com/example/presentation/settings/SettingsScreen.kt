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


@Composable
fun SettingsScreen(viewModel: ShasthoViewModel, onNavigateBack: () -> Unit = {}, onLogout: () -> Unit = {}) {
    val profile by viewModel.userProfile.collectAsState()
    val isDark = profile?.isDarkMode ?: isSystemInDarkTheme()

    var name by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }

    var calorieLimit by remember { mutableStateOf("") }
    var showSavedMessage by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }


    var isDarkMode by remember { mutableStateOf(false) }
    var notificationsEnabled by remember { mutableStateOf(true) }
    var remindersEnabled by remember { mutableStateOf(true) }

    var expandedLanguage by remember { mutableStateOf(false) }
    val languages = listOf("English", "Bengali")
    var selectedLanguage by remember { mutableStateOf(languages[0]) }

    var profilePictureUri by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isHealthConnectAvailable by remember { mutableStateOf(HealthConnectManager.isAvailable(context)) }

    val requestPermissionLauncher = rememberLauncherForActivityResult(PermissionController.createRequestPermissionResultContract()) { granted ->
        if (granted.containsAll(HealthConnectManager.REQUIRED_PERMISSIONS)) {
            Toast.makeText(context, "Health Connect connected!", Toast.LENGTH_SHORT).show()
            viewModel.syncWithHealthConnect(context)
        } else {
            Toast.makeText(context, "Permissions denied", Toast.LENGTH_SHORT).show()
        }
    }


    val launcher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri? ->
        profilePictureUri = uri?.toString()
    }

    LaunchedEffect(profile) {
        if (profile != null) {
            name = profile!!.name
            age = profile!!.age.toString()
            weight = profile!!.weightKg.toString()
            height = profile!!.heightCm.toString()
            calorieLimit = profile!!.dailyCalorieLimit.toString()
            profilePictureUri = profile!!.profilePictureUri
            isDarkMode = profile!!.isDarkMode
            notificationsEnabled = profile!!.notificationsEnabled
            remindersEnabled = profile!!.remindersEnabled
            selectedLanguage = profile!!.selectedLanguage
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
                            .border(2.dp, Emerald500, CircleShape)
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
                                    dailyCalorieLimit = calorieLimit.toIntOrNull() ?: it.dailyCalorieLimit,
                                    profilePictureUri = finalPhotoUrl,
                                    isDarkMode = isDarkMode,
                                    notificationsEnabled = notificationsEnabled,
                                    remindersEnabled = remindersEnabled,
                                    selectedLanguage = selectedLanguage
                                )
                                viewModel.saveProfile(updated)
                                showSavedMessage = true
                                isSaving = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                    enabled = !isSaving
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text("Save Profile")
                    }
                }

                if (showSavedMessage) {
                    Text(
                        text = "Profile updated successfully!",
                        color = if (isDark) Emerald300 else Emerald600,
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
                    Text("Dark Mode", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                    Switch(
                        checked = isDarkMode,
                        onCheckedChange = { isDarkMode = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Emerald600, checkedTrackColor = Emerald200)
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
                        colors = SwitchDefaults.colors(checkedThumbColor = Emerald600, checkedTrackColor = Emerald200)
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
                        colors = SwitchDefaults.colors(checkedThumbColor = Emerald600, checkedTrackColor = Emerald200)
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp).clickable { expandedLanguage = true },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Language", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(selectedLanguage, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Language", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    DropdownMenu(
                        expanded = expandedLanguage,
                        onDismissRequest = { expandedLanguage = false }
                    ) {
                        languages.forEach { lang ->
                            DropdownMenuItem(
                                text = { Text(lang) },
                                onClick = {
                                    selectedLanguage = lang
                                    expandedLanguage = false
                                }
                            )
                        }
                    }
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
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald600)
                    ) {
                        Text("Connect")
                    }
                }

        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = { showLogoutDialog = true },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Slate500)
        ) {
            Text("Log Out")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { showDeleteDialog = true },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
        ) {
            Text("Delete Account", color = Color.White)
        }
        Spacer(modifier = Modifier.height(100.dp))
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
                    viewModel.deleteAccount { success ->
                        if (success) {
                            onLogout()
                        } else {
                            Toast.makeText(context, "Please log out, log back in, and try deleting again immediately.", Toast.LENGTH_LONG).show()
                        }
                    }
                }) {
                    Text("Delete", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }
}
