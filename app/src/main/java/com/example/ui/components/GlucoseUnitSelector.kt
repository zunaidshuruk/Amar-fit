package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlucoseUnitSelector(
    useMgdl: Boolean,
    onChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Unit", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FilterChip(
            selected = !useMgdl,
            onClick = { if (useMgdl) onChange(false) },
            label = { Text("mmol/L") }
        )
        FilterChip(
            selected = useMgdl,
            onClick = { if (!useMgdl) onChange(true) },
            label = { Text("mg/dL") }
        )
    }
}

fun glucoseLooksWrongUnit(value: Float, useMgdl: Boolean): Boolean = if (useMgdl) value < 20f else value > 40f
