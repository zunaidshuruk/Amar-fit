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
    useImperialUnits: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (Float) -> Unit
) {
    var heightUnit by remember { mutableStateOf(if (useImperialUnits) HeightUnit.FT else HeightUnit.CM) }
    var weightUnit by remember { mutableStateOf(if (useImperialUnits) WeightUnit.LB else WeightUnit.KG) }

    var currentCm by remember { mutableStateOf(if (initialValue > 0f) initialValue else 170f) }
    var currentKg by remember { mutableStateOf(if (initialValue > 0f) initialValue else 70f) }

    var cmVal by remember { mutableStateOf(PickerMath.cmToWheel(currentCm)) }
    var ftVal by remember { mutableStateOf(PickerMath.cmToFtIn(currentCm)) }

    var kgVal by remember { mutableStateOf(PickerMath.kgToWheel(currentKg)) }
    var lbVal by remember { mutableStateOf(PickerMath.kgToLb(currentKg)) }
    var stVal by remember { mutableStateOf(PickerMath.kgToStLb(currentKg)) }

    // what the wheels showed when the dialog opened: pressing OK without moving anything must give back the exact value that came in
    val startHeightUnit = remember { heightUnit }
    val startWeightUnit = remember { weightUnit }
    val startCm = remember { cmVal }
    val startFtIn = remember { ftVal }
    val startKg = remember { kgVal }
    val startLb = remember { lbVal }
    val startStLb = remember { stVal }

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
                                    .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                    .clickable {
                                        when (heightUnit) {
                                            HeightUnit.CM -> currentCm = cmVal.toFloat()
                                            HeightUnit.FT -> currentCm = PickerMath.ftInToCm(ftVal.first, ftVal.second)
                                        }
                                        heightUnit = unit
                                        when (unit) {
                                            HeightUnit.CM -> cmVal = PickerMath.cmToWheel(currentCm)
                                            HeightUnit.FT -> ftVal = PickerMath.cmToFtIn(currentCm)
                                        }
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (selected) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text(
                                        text = unit.name.lowercase(),
                                        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
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
                                    .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                    .clickable {
                                        when (weightUnit) {
                                            WeightUnit.KG -> currentKg = kgVal.toFloat()
                                            WeightUnit.LB -> currentKg = PickerMath.lbToKg(lbVal)
                                            WeightUnit.ST -> currentKg = PickerMath.stLbToKg(stVal.first, stVal.second)
                                        }
                                        weightUnit = unit
                                        when (unit) {
                                            WeightUnit.KG -> kgVal = PickerMath.kgToWheel(currentKg)
                                            WeightUnit.LB -> lbVal = PickerMath.kgToLb(currentKg)
                                            WeightUnit.ST -> stVal = PickerMath.kgToStLb(currentKg)
                                        }
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (selected) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text(
                                        text = unit.name.lowercase(),
                                        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
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
                        HeightUnit.FT -> PickerMath.ftInToCm(ftVal.first, ftVal.second)
                    }
                } else {
                    when (weightUnit) {
                        WeightUnit.KG -> kgVal.toFloat()
                        WeightUnit.LB -> PickerMath.lbToKg(lbVal)
                        WeightUnit.ST -> PickerMath.stLbToKg(stVal.first, stVal.second)
                    }
                }
                val untouched = initialValue > 0f && if (mode == PickerMode.HEIGHT) {
                    heightUnit == startHeightUnit && when (heightUnit) {
                        HeightUnit.CM -> cmVal == startCm
                        HeightUnit.FT -> ftVal == startFtIn
                    }
                } else {
                    weightUnit == startWeightUnit && when (weightUnit) {
                        WeightUnit.KG -> kgVal == startKg
                        WeightUnit.LB -> lbVal == startLb
                        WeightUnit.ST -> stVal == startStLb
                    }
                }
                // nothing was moved: give back exactly what came in (72.5 kg stays 72.5, not 72)
                onConfirm(if (untouched) initialValue else finalCanonical)
            }) {
                Text("OK", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
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
