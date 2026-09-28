package com.example.presentation.health

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.MedicalCategory
import com.example.data.local.MedicalRecord
import com.example.presentation.viewmodel.ShasthoViewModel
import java.time.LocalDate

@Composable
fun MedicalRecordsScreen(
    viewModel: ShasthoViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val allRecords by viewModel.medicalRecords.collectAsState()

    var activeCategoryForDialog by remember { mutableStateOf<MedicalCategory?>(null) }
    var titleInput by remember { mutableStateOf("") }
    var detailsInput by remember { mutableStateOf("") }
    var dateInput by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.syncErrorEvent.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    val categories = remember {
        listOf(
            MedicalCategory.ALLERGY,
            MedicalCategory.CONDITION,
            MedicalCategory.MEDICATION,
            MedicalCategory.VACCINE,
            MedicalCategory.PREGNANCY,
            MedicalCategory.SOCIAL_HISTORY,
            MedicalCategory.PROCEDURE,
            MedicalCategory.VISIT,
            MedicalCategory.LAB_RESULT
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Top Bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Medical Records",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // 9 Category Sections
        categories.forEach { category ->
            val categoryRecords = remember(allRecords, category) {
                allRecords.filter { it.category.equals(category.categoryKey, ignoreCase = true) }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Category Header with + Add button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = category.displayName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    TextButton(
                        onClick = {
                            titleInput = ""
                            detailsInput = ""
                            dateInput = LocalDate.now().toString()
                            activeCategoryForDialog = category
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add ${category.displayName}",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Add",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Records list or Empty State
                if (categoryRecords.isEmpty()) {
                    Text(
                        text = "No entries yet",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                    )
                } else {
                    categoryRecords.forEach { record ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = record.title,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (record.details.isNotBlank()) {
                                        Text(
                                            text = record.details,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Normal,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                                        )
                                    }
                                    if (record.recordDate.isNotBlank()) {
                                        Text(
                                            text = record.recordDate,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = { viewModel.deleteMedicalRecord(record) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete record",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Record Dialog
    activeCategoryForDialog?.let { category ->
        AlertDialog(
            onDismissRequest = { activeCategoryForDialog = null },
            title = {
                Text(
                    text = "Add ${category.displayName.removeSuffix("s")}",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = titleInput,
                        onValueChange = { titleInput = it },
                        label = { Text("Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = detailsInput,
                        onValueChange = { detailsInput = it },
                        label = { Text("Details") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = dateInput,
                        onValueChange = { dateInput = it },
                        label = { Text("Date (e.g. 2026-09-07)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (titleInput.isNotBlank()) {
                            val newRecord = MedicalRecord(
                                category = category.categoryKey,
                                title = titleInput.trim(),
                                details = detailsInput.trim(),
                                recordDate = dateInput.trim()
                            )
                            viewModel.addMedicalRecord(newRecord)
                        }
                        activeCategoryForDialog = null
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { activeCategoryForDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
