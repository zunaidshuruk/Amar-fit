package com.example.presentation.mealplan

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.SavedDietChart
import com.example.presentation.viewmodel.ShasthoViewModel
import com.example.ui.components.MarkdownText
import com.example.ui.theme.*
import org.json.JSONArray
import org.json.JSONObject

@Composable
fun DietChartScreen(viewModel: ShasthoViewModel, onNavigateBack: () -> Unit = {}) {
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
            .background(Background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()).imePadding()
    ) {
        if (selectedSavedChart != null) {
            SavedChartDetailView(
                chart = selectedSavedChart!!, 
                onBack = { selectedSavedChart = null },
                onUpdate = { updated -> viewModel.updateSavedDietChart(updated); selectedSavedChart = updated }
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp, top = 16.dp)
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Emerald900
                    )
                }
                Text(
                    text = "Diet Plans",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Emerald900
                )
            }

            // Tabs
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                Button(
                    onClick = { selectedTab = 0 },
                    colors = ButtonDefaults.buttonColors(containerColor = if (selectedTab == 0) Emerald600 else Slate200),
                    modifier = Modifier.weight(1f).padding(end = 4.dp)
                ) {
                    Text("Generate New", color = if (selectedTab == 0) Color.White else Slate500)
                }
                Button(
                    onClick = { selectedTab = 1 },
                    colors = ButtonDefaults.buttonColors(containerColor = if (selectedTab == 1) Emerald600 else Slate200),
                    modifier = Modifier.weight(1f).padding(start = 4.dp)
                ) {
                    Text("Saved Plans", color = if (selectedTab == 1) Color.White else Slate500)
                }
            }

            if (selectedTab == 0) {
                GenerateNewDietView(viewModel, dietChart, isGenerating, shoppingList)
            } else {
                SavedChartsListView(savedCharts, onSelect = { selectedSavedChart = it }, onDelete = { viewModel.deleteSavedDietChart(it) })
            }
        }
    }
}

@Composable
fun GenerateNewDietView(
    viewModel: ShasthoViewModel, 
    dietChart: String?, 
    isGenerating: Boolean, 
    shoppingList: String?
) {
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

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Create a new plan based on Bangladeshi local foods & your metrics.", fontSize = 14.sp, color = Slate600, modifier = Modifier.padding(bottom = 16.dp))
            OutlinedTextField(
                value = duration,
                onValueChange = { duration = it },
                label = { Text("Duration (Days)") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            )
            Button(
                onClick = { 
                    viewModel.generateDietChart(duration.toIntOrNull() ?: 7) 
                    showShoppingList = false
                    isEditing = false
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                enabled = !isGenerating
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("Generate AI Diet Chart")
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(24.dp))

    if (!dietChart.isNullOrEmpty() && !isGenerating) {
        if (showShoppingList && !shoppingList.isNullOrEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Shopping List", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Emerald900, modifier = Modifier.padding(bottom = 12.dp))
                    MarkdownText(text = shoppingList, color = TextPrimary)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { showShoppingList = false },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Slate500)
                        ) {
                            Text("Back")
                        }
                        Button(
                            onClick = { showSaveDialog = true },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Emerald600)
                        ) {
                            Text("Save Plan")
                        }
                    }
                }
            }
        } else {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Your Diet Chart", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Emerald900)
                        if (isEditing) {
                            IconButton(onClick = { 
                                isEditing = false 
                                viewModel.updateDietChart(editedChart)
                            }) {
                                Icon(Icons.Default.Save, contentDescription = "Save", tint = Emerald600)
                            }
                        } else {
                            IconButton(onClick = { isEditing = true }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Slate500)
                            }
                        }
                    }

                    if (isEditing) {
                        OutlinedTextField(
                            value = editedChart,
                            onValueChange = { editedChart = it },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp),
                            textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                        )
                    } else {
                        MarkdownText(text = dietChart, color = TextPrimary)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (!isEditing) {
                        Button(
                            onClick = { 
                                viewModel.generateShoppingList(dietChart)
                                showShoppingList = true
                            },
                            modifier = Modifier.fillMaxWidth().height(50.dp).padding(bottom = 8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Emerald500)
                        ) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generate Shopping List")
                        }
                        
                        Button(
                            onClick = { showSaveDialog = true },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Emerald700)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save Diet Plan")
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
                OutlinedTextField(
                    value = chartName,
                    onValueChange = { chartName = it },
                    label = { Text("Plan Name") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val finalShoppingList = shoppingList ?: ""
                    // Convert markdown shopping list to JSON
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
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    Spacer(modifier = Modifier.height(100.dp))
}

@Composable
fun SavedChartsListView(savedCharts: List<SavedDietChart>, onSelect: (SavedDietChart) -> Unit, onDelete: (SavedDietChart) -> Unit) {
    if (savedCharts.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(top = 40.dp), contentAlignment = Alignment.Center) {
            Text("No saved diet plans yet.", color = Slate500)
        }
    } else {
        savedCharts.forEach { chart ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).clickable { onSelect(chart) },
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(chart.name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Emerald900)
                        Text(
                            java.text.SimpleDateFormat("MMM dd, yyyy").format(java.util.Date(chart.createdAt)), 
                            fontSize = 12.sp, 
                            color = Slate500
                        )
                    }
                    IconButton(onClick = { onDelete(chart) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Red500)
                    }
                }
            }
        }
    }
    Spacer(modifier = Modifier.height(100.dp))
}

@Composable
fun SavedChartDetailView(chart: SavedDietChart, onBack: () -> Unit, onUpdate: (SavedDietChart) -> Unit) {
    var isEditingList by remember { mutableStateOf(false) }

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 16.dp)) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Emerald900)
        }
        Text(text = chart.name, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Emerald900)
    }

    if (chart.shoppingList.startsWith("[")) {
        // Render Checklist
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).padding(bottom = 24.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Shopping List", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Emerald900, modifier = Modifier.padding(bottom = 12.dp))
                
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
                    Text("Could not parse checklist.", color = Red500)
                } else {
                    items.forEachIndexed { i, (text, isChecked) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clickable {
                                try {
                                    val jsonArray = JSONArray(chart.shoppingList)
                                    val obj = jsonArray.getJSONObject(i)
                                    obj.put("checked", !isChecked)
                                    jsonArray.put(i, obj)
                                    onUpdate(chart.copy(shoppingList = jsonArray.toString()))
                                } catch (e: Exception) {}
                            }.padding(vertical = 4.dp)
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
                                colors = CheckboxDefaults.colors(checkedColor = Emerald600)
                            )
                            Text(
                                text = text, 
                                fontSize = 16.sp, 
                                color = if (isChecked) Slate400 else TextPrimary,
                                textDecoration = if (isChecked) TextDecoration.LineThrough else TextDecoration.None
                            )
                        }
                    }
                }
            }
        }
    } else if (chart.shoppingList.isNotBlank()) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).padding(bottom = 24.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Shopping List", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Emerald900, modifier = Modifier.padding(bottom = 12.dp))
                MarkdownText(text = chart.shoppingList, color = TextPrimary)
            }
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Diet Chart", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Emerald900, modifier = Modifier.padding(bottom = 12.dp))
            MarkdownText(text = chart.chartContent, color = TextPrimary)
        }
    }

    Spacer(modifier = Modifier.height(100.dp))
}
