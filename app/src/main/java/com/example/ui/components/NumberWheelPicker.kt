package com.example.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.Emerald600
import kotlin.math.abs

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NumberWheelPicker(
    range: IntRange,
    selectedValue: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = remember(range) { range.toList() }
    val itemHeight = 56.dp
    val visibleItemsCount = 5
    val initialIndex = items.indexOf(selectedValue).coerceAtLeast(0)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val snapFlingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            val layoutInfo = listState.layoutInfo
            val viewportCenter = layoutInfo.viewportStartOffset + layoutInfo.viewportSize.height / 2
            val centerItem = layoutInfo.visibleItemsInfo.minByOrNull {
                abs((it.offset + it.size / 2) - viewportCenter)
            }
            centerItem?.let {
                val index = it.index
                if (index in items.indices) {
                    onValueChange(items[index])
                }
            }
        }
    }

    val currentCenterValue by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val viewportCenter = layoutInfo.viewportStartOffset + layoutInfo.viewportSize.height / 2
            val centerItem = layoutInfo.visibleItemsInfo.minByOrNull {
                abs((it.offset + it.size / 2) - viewportCenter)
            }
            centerItem?.let {
                val index = it.index
                if (index in items.indices) items[index] else selectedValue
            } ?: selectedValue
        }
    }

    LaunchedEffect(currentCenterValue) {
        if (currentCenterValue != selectedValue) {
            onValueChange(currentCenterValue)
        }
    }

    Box(
        modifier = modifier
            .height(itemHeight * visibleItemsCount)
            .width(80.dp),
        contentAlignment = Alignment.Center
    ) {
        LazyColumn(
            state = listState,
            flingBehavior = snapFlingBehavior,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = itemHeight * 2),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(items.size) { index ->
                val value = items[index]
                Box(
                    modifier = Modifier
                        .height(itemHeight)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    val isSelected = value == currentCenterValue
                    val alpha = if (isSelected) 1f else 0.5f
                    val fontSize = if (isSelected) 20.sp else 16.sp
                    val fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    val color = if (isSelected) Emerald600 else MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)

                    Text(
                        text = value.toString(),
                        fontSize = fontSize,
                        fontWeight = fontWeight,
                        color = color
                    )
                }
            }
        }
    }
}
