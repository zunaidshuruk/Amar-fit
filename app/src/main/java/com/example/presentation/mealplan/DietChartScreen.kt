package com.example.presentation.mealplan

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.SavedDietChart
import com.example.presentation.common.appSafeInsets
import com.example.presentation.viewmodel.ShasthoViewModel
import com.example.ui.components.MarkdownText
import org.json.JSONArray
import org.json.JSONObject

@Composable
fun DietChartScreen(viewModel: ShasthoViewModel, onNavigateBack: () -> Unit = {}) {
    val profile by viewModel.userProfile.collectAsState()
    val isDark = true

    val dietChart by viewModel.dietChart.collectAsState()
    val isGenerating by viewModel.isGeneratingDiet.collectAsState()
    val shoppingList by viewModel.shoppingList.collectAsState()
    val savedCharts by viewModel.savedDietCharts.collectAsState()

    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.syncErrorEvent.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    var selectedTab by remember { mutableStateOf(0) } // 0 = New, 1 = Saved
    var selectedSavedChart by remember { mutableStateOf<SavedDietChart?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(appSafeInsets())
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
            .imePadding()
    ) {
        if (selectedSavedChart != null) {
            SavedChartDetailView(
                chart = selectedSavedChart!!, 
                isDark = isDark,
                onBack = { selectedSavedChart = null },
                onUpdate = { updated -> viewModel.updateSavedDietChart(updated); selectedSavedChart = updated }
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Text(
                    text = "Diet Plans",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // Pill-shaped Segmented Control
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .clip(RoundedCornerShape(50.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(4.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(50.dp))
                            .background(if (selectedTab == 0) MaterialTheme.colorScheme.primary else Color.Unspecified)
                            .clickable { selectedTab = 0 }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Generate New",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (selectedTab == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Clip
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(50.dp))
                            .background(if (selectedTab == 1) MaterialTheme.colorScheme.primary else Color.Unspecified)
                            .clickable { selectedTab = 1 }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Saved Plans",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (selectedTab == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Clip
                        )
                    }
                }
            }

            if (selectedTab == 0) {
                GenerateNewDietView(viewModel, dietChart, isGenerating, shoppingList, isDark)
            } else {
                SavedChartsListView(savedCharts, isDark = isDark, onSelect = { selectedSavedChart = it }, onDelete = { viewModel.deleteSavedDietChart(it) })
            }
        }
    }
}

@Composable
fun GenerateNewDietView(
    viewModel: ShasthoViewModel, 
    dietChart: String?, 
    isGenerating: Boolean, 
    shoppingList: String?,
    isDark: Boolean
) {
    val navController = com.example.LocalNavController.current
    var duration by remember { mutableStateOf("7") }
    var isEditing by remember { mutableStateOf(false) }
    var editedChart by remember { mutableStateOf("") }
    var showShoppingList by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var chartName by remember { mutableStateOf("") }

    LaunchedEffect(dietChart) {
        if (dietChart != null) {
            editedChart = dietChart
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp)
    ) {
        Column {
            Text(
                text = "Create a new plan based on Bangladeshi local foods & your metrics.", 
                fontSize = 14.sp, 
                color = MaterialTheme.colorScheme.onSurfaceVariant, 
                modifier = Modifier.padding(bottom = 16.dp)
            )
            OutlinedTextField(
                value = duration,
                onValueChange = { duration = it },
                label = { Text("Duration (Days)") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )
            Button(
                onClick = { 
                    val days = (duration.toIntOrNull() ?: 7).coerceIn(1, 30)
                    navController?.navigate("universal_assistant?starter=" + android.net.Uri.encode("Create a $days-day diet chart for me using Bangladeshi foods that fit my profile and goals."))
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Ask AI for a Diet Chart", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    if (isGenerating && dietChart.isNullOrEmpty()) {
        Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
    } else if (!dietChart.isNullOrEmpty()) {
        if (showShoppingList && !shoppingList.isNullOrEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        text = "Shopping List", 
                        fontSize = 20.sp, 
                        fontWeight = FontWeight.Bold, 
                        color = MaterialTheme.colorScheme.onSurface, 
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    MarkdownText(text = shoppingList, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { showShoppingList = false },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.background,
                                contentColor = MaterialTheme.colorScheme.onBackground
                            )
                        ) {
                            Text("Back")
                        }
                        Button(
                            onClick = { showSaveDialog = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Save Plan", color = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Your Diet Chart", 
                            fontSize = 20.sp, 
                            fontWeight = FontWeight.Bold, 
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (isEditing) {
                            IconButton(onClick = { 
                                isEditing = false 
                                viewModel.updateDietChart(editedChart)
                            }) {
                                Icon(Icons.Default.Save, contentDescription = "Save", tint = MaterialTheme.colorScheme.primary)
                            }
                        } else {
                            IconButton(onClick = { isEditing = true }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (isEditing) {
                        OutlinedTextField(
                            value = editedChart,
                            onValueChange = { editedChart = it },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp),
                            textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                        )
                    } else {
                        MarkdownText(text = dietChart, color = MaterialTheme.colorScheme.onSurface)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (!isEditing) {
                        Button(
                            onClick = { 
                                viewModel.generateShoppingList(dietChart)
                                showShoppingList = true
                            },
                            modifier = Modifier.fillMaxWidth().height(50.dp).padding(bottom = 8.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generate Shopping List", color = MaterialTheme.colorScheme.onPrimary)
                        }

                        Button(
                            onClick = { showSaveDialog = true },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save Diet Plan", color = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                }
            }
        }
    }
    
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save Diet Plan") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    OutlinedTextField(
                        value = chartName,
                        onValueChange = { chartName = it },
                        label = { Text("Plan Name") }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val finalShoppingList = shoppingList ?: ""
                    val jsonArray = JSONArray()
                    finalShoppingList.lines().forEach { line ->
                        val trimmed = line.trim()
                        if (trimmed.startsWith("-") || trimmed.startsWith("*")) {
                            val item = trimmed.substring(1).trim()
                            if (item.isNotBlank()) {
                                val obj = JSONObject()
                                obj.put("item", item)
                                obj.put("checked", false)
                                jsonArray.put(obj)
                            }
                        }
                    }
                    val shoppingListJson = if (jsonArray.length() > 0) jsonArray.toString() else finalShoppingList

                    viewModel.saveDietChart(
                        name = chartName.ifBlank { "My Diet Plan" },
                        content = editedChart,
                        shoppingList = shoppingListJson
                    )
                    showSaveDialog = false
                    chartName = ""
                }) {
                    Text("Save", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    Spacer(modifier = Modifier.height(88.dp))
}

@Composable
fun SavedChartsListView(
    savedCharts: List<SavedDietChart>, 
    isDark: Boolean,
    onSelect: (SavedDietChart) -> Unit, 
    onDelete: (SavedDietChart) -> Unit
) {
    if (savedCharts.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(top = 40.dp), contentAlignment = Alignment.Center) {
            Text("No saved diet plans yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        savedCharts.forEach { chart ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onSelect(chart) }
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = chart.name, 
                            fontSize = 16.sp, 
                            fontWeight = FontWeight.Bold, 
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = java.text.SimpleDateFormat("MMM dd, yyyy").format(java.util.Date(chart.createdAt)), 
                            fontSize = 12.sp, 
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { onDelete(chart) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
    Spacer(modifier = Modifier.height(88.dp))
}

@Composable
fun SavedChartDetailView(
    chart: SavedDietChart, 
    isDark: Boolean,
    onBack: () -> Unit, 
    onUpdate: (SavedDietChart) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically, 
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack, 
                contentDescription = "Back", 
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
        Text(
            text = chart.name, 
            fontSize = 24.sp, 
            fontWeight = FontWeight.Bold, 
            color = MaterialTheme.colorScheme.onBackground
        )
    }

    if (chart.shoppingList.startsWith("[")) {
        // Render Checklist
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(16.dp)
                .padding(bottom = 8.dp)
        ) {
            Column {
                Text(
                    text = "Shopping List", 
                    fontSize = 20.sp, 
                    fontWeight = FontWeight.Bold, 
                    color = MaterialTheme.colorScheme.onSurface, 
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                val items = remember(chart.shoppingList) {
                    val list = mutableListOf<Pair<String, Boolean>>()
                    try {
                        val jsonArray = JSONArray(chart.shoppingList)
                        for (i in 0 until jsonArray.length()) {
                            val itemObj = jsonArray.getJSONObject(i)
                            list.add(Pair(itemObj.getString("item"), itemObj.getBoolean("checked")))
                        }
                    } catch (e: Exception) {}
                    list
                }
                
                if (items.isEmpty()) {
                    Text("Could not parse checklist.", color = MaterialTheme.colorScheme.error)
                } else {
                    items.forEachIndexed { i, (text, isChecked) ->
                        if (i > 0) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    try {
                                        val jsonArray = JSONArray(chart.shoppingList)
                                        val obj = jsonArray.getJSONObject(i)
                                        obj.put("checked", !isChecked)
                                        jsonArray.put(i, obj)
                                        onUpdate(chart.copy(shoppingList = jsonArray.toString()))
                                    } catch (e: Exception) {}
                                }
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    try {
                                        val jsonArray = JSONArray(chart.shoppingList)
                                        val obj = jsonArray.getJSONObject(i)
                                        obj.put("checked", checked)
                                        jsonArray.put(i, obj)
                                        onUpdate(chart.copy(shoppingList = jsonArray.toString()))
                                    } catch (e: Exception) {}
                                },
                                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                            )
                            Text(
                                text = text, 
                                fontSize = 16.sp, 
                                color = if (isChecked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                                textDecoration = if (isChecked) TextDecoration.LineThrough else TextDecoration.None
                            )
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    } else if (chart.shoppingList.isNotBlank()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(16.dp)
        ) {
            Column {
                Text(
                    text = "Shopping List", 
                    fontSize = 20.sp, 
                    fontWeight = FontWeight.Bold, 
                    color = MaterialTheme.colorScheme.onSurface, 
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                MarkdownText(text = chart.shoppingList, color = MaterialTheme.colorScheme.onSurface)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp)
    ) {
        Column {
            Text(
                text = "Diet Chart", 
                fontSize = 20.sp, 
                fontWeight = FontWeight.Bold, 
                color = MaterialTheme.colorScheme.onSurface, 
                modifier = Modifier.padding(bottom = 12.dp)
            )
            MarkdownText(text = chart.chartContent, color = MaterialTheme.colorScheme.onSurface)
        }
    }

    Spacer(modifier = Modifier.height(88.dp))
}

