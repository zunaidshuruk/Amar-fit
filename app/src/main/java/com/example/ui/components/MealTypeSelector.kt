package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cookie
import androidx.compose.material.icons.filled.DinnerDining
import androidx.compose.material.icons.filled.FreeBreakfast
import androidx.compose.material.icons.filled.LunchDining
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.Emerald100
import com.example.ui.theme.Emerald700

private data class MealTypeItem(
    val type: String,
    val icon: ImageVector
)

private val mealTypes = listOf(
    MealTypeItem("Breakfast", Icons.Default.FreeBreakfast),
    MealTypeItem("Lunch", Icons.Default.LunchDining),
    MealTypeItem("Dinner", Icons.Default.DinnerDining),
    MealTypeItem("Snack", Icons.Default.Cookie)
)

@Composable
fun MealTypeSelector(
    selectedMealType: String,
    onMealTypeSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        mealTypes.forEach { item ->
            val isSelected = selectedMealType.equals(item.type, ignoreCase = true)
            FilterChip(
                selected = isSelected,
                onClick = { onMealTypeSelected(item.type) },
                label = {
                    Text(
                        text = item.type,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.type,
                        modifier = Modifier.size(16.dp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Emerald100,
                    selectedLabelColor = Emerald700,
                    selectedLeadingIconColor = Emerald700,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    iconColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.weight(1f)
            )
        }
    }
}
