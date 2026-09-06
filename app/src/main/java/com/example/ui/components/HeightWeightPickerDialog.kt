package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.Emerald600

enum class PickerMode {
    HEIGHT, WEIGHT
}

enum class HeightUnit {
    CM, FT
}

enum class WeightUnit {
    KG, LB, ST
}

@Composable
fun HeightWeightPickerDialog(
    mode: PickerMode,
    initialValue: Float, // height in cm or weight in kg
    onDismiss: () -> Unit,
    onConfirm: (Float) -> Unit
) {
    var heightUnit by remember { mutableStateOf(HeightUnit.CM) }
    var weightUnit by remember { mutableStateOf(WeightUnit.KG) }

    var currentCm by remember { mutableStateOf(if (initialValue > 0f) initialValue else 170f) }
    var currentKg by remember { mutableStateOf(if (initialValue > 0f) initialValue else 70f) }

    var cmVal by remember { mutableStateOf(currentCm.toInt().coerceIn(50, 250)) }
    var ftVal by remember {
        mutableStateOf<Pair<Int, Int>>(
            run {
                val totalInches = (currentCm / 2.54f).toInt()
                val ft = (totalInches / 12).coerceIn(3, 8)
                val inches = (totalInches % 12).coerceIn(0, 11)
                Pair(ft, inches)
            }
        )
    }

    var kgVal by remember { mutableStateOf(currentKg.toInt().coerceIn(20, 300)) }
    var lbVal by remember { mutableStateOf((currentKg * 2.20462f).toInt().coerceIn(44, 660)) }
    var stVal by remember {
        mutableStateOf<Pair<Int, Int>>(
            run {
                val totalLb = currentKg * 2.20462f
                val st = (totalLb / 14f).toInt().coerceIn(3, 47)
                val lb = (totalLb % 14f).toInt().coerceIn(0, 13)
                Pair(st, lb)
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (mode == PickerMode.HEIGHT) "Select Height" else "Select Weight",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Unit Toggle Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    if (mode == PickerMode.HEIGHT) {
                        HeightUnit.values().forEach { unit ->
                            val selected = heightUnit == unit
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selected) Emerald600 else Color.Transparent)
                                    .clickable {
                                        when (heightUnit) {
                                            HeightUnit.CM -> currentCm = cmVal.toFloat()
                                            HeightUnit.FT -> currentCm = (ftVal.first * 30.48f) + (ftVal.second * 2.54f)
                                        }
                                        heightUnit = unit
                                        when (unit) {
                                            HeightUnit.CM -> cmVal = currentCm.toInt().coerceIn(50, 250)
                                            HeightUnit.FT -> {
                                                val totalInches = (currentCm / 2.54f).toInt()
                                                val ft = (totalInches / 12).coerceIn(3, 8)
                                                val inches = (totalInches % 12).coerceIn(0, 11)
                                                ftVal = Pair(ft, inches)
                                            }
                                        }
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (selected) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text(
                                        text = unit.name.lowercase(),
                                        color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    } else {
                        WeightUnit.values().forEach { unit ->
                            val selected = weightUnit == unit
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selected) Emerald600 else Color.Transparent)
                                    .clickable {
                                        when (weightUnit) {
                                            WeightUnit.KG -> currentKg = kgVal.toFloat()
                                            WeightUnit.LB -> currentKg = lbVal / 2.20462f
                                            WeightUnit.ST -> currentKg = ((stVal.first * 14f) + stVal.second) / 2.20462f
                                        }
                                        weightUnit = unit
                                        when (unit) {
                                            WeightUnit.KG -> kgVal = currentKg.toInt().coerceIn(20, 300)
                                            WeightUnit.LB -> lbVal = (currentKg * 2.20462f).toInt().coerceIn(44, 660)
                                            WeightUnit.ST -> {
                                                val totalLb = currentKg * 2.20462f
                                                val st = (totalLb / 14f).toInt().coerceIn(3, 47)
                                                val lb = (totalLb % 14f).toInt().coerceIn(0, 13)
                                                stVal = Pair(st, lb)
                                            }
                                        }
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (selected) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text(
                                        text = unit.name.lowercase(),
                                        color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (mode == PickerMode.HEIGHT) {
                    when (heightUnit) {
                        HeightUnit.CM -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                NumberWheelPicker(
                                    range = 50..250,
                                    selectedValue = cmVal,
                                    onValueChange = { cmVal = it }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("cm", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                        HeightUnit.FT -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    NumberWheelPicker(
                                        range = 3..8,
                                        selectedValue = ftVal.first,
                                        onValueChange = { ftVal = Pair(it, ftVal.second) }
                                    )
                                    Text("ft", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    NumberWheelPicker(
                                        range = 0..11,
                                        selectedValue = ftVal.second,
                                        onValueChange = { ftVal = Pair(ftVal.first, it) }
                                    )
                                    Text("in", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                } else {
                    when (weightUnit) {
                        WeightUnit.KG -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                NumberWheelPicker(
                                    range = 20..300,
                                    selectedValue = kgVal,
                                    onValueChange = { kgVal = it }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("kg", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                        WeightUnit.LB -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                NumberWheelPicker(
                                    range = 44..660,
                                    selectedValue = lbVal,
                                    onValueChange = { lbVal = it }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("lb", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                        WeightUnit.ST -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    NumberWheelPicker(
                                        range = 3..47,
                                        selectedValue = stVal.first,
                                        onValueChange = { stVal = Pair(it, stVal.second) }
                                    )
                                    Text("st", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    NumberWheelPicker(
                                        range = 0..13,
                                        selectedValue = stVal.second,
                                        onValueChange = { stVal = Pair(stVal.first, it) }
                                    )
                                    Text("lb", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val finalCanonical = if (mode == PickerMode.HEIGHT) {
                    when (heightUnit) {
                        HeightUnit.CM -> cmVal.toFloat()
                        HeightUnit.FT -> (ftVal.first * 30.48f) + (ftVal.second * 2.54f)
                    }
                } else {
                    when (weightUnit) {
                        WeightUnit.KG -> kgVal.toFloat()
                        WeightUnit.LB -> lbVal / 2.20462f
                        WeightUnit.ST -> ((stVal.first * 14f) + stVal.second) / 2.20462f
                    }
                }
                onConfirm(finalCanonical)
            }) {
                Text("OK", color = Emerald600, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp)
    )
}
