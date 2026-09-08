package com.example.presentation.today

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.presentation.viewmodel.ShasthoViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditFocusScreen(
    viewModel: ShasthoViewModel,
    onNavigateBack: () -> Unit
) {
    val profile by viewModel.userProfile.collectAsState()
    val isDark = profile?.isDarkMode ?: isSystemInDarkTheme()

    val metrics by viewModel.todayMetrics.collectAsState()
    val last7Metrics by viewModel.getMetricsHistoryFlow(7).collectAsState(initial = emptyList())
    val todayFoodLogs by viewModel.todayFoodLogs.collectAsState()

    val (activeLargeIds, activeSmallIds) = remember(profile?.todayTileSlots) {
        parseTodayTileSlots(profile?.todayTileSlots)
    }
    val allActiveIds = remember(activeLargeIds, activeSmallIds) {
        activeLargeIds + activeSmallIds
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Edit focus",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) MaterialTheme.colorScheme.onSurface else TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("edit_focus_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = if (isDark) MaterialTheme.colorScheme.onSurface else TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isDark) MaterialTheme.colorScheme.background else Background
                )
            )
        },
        containerColor = if (isDark) MaterialTheme.colorScheme.background else Background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(bottom = 32.dp, top = 8.dp)
        ) {
            // Section 1: Your Tiles
            item {
                Text(
                    text = "Your tiles",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) MaterialTheme.colorScheme.onSurface else TextPrimary
                )
                Text(
                    text = "Tiles currently showing on your Today tab.",
                    fontSize = 13.sp,
                    color = if (isDark) Slate400 else Slate500,
                    modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                )

                if (allActiveIds.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isDark) MaterialTheme.colorScheme.surfaceVariant else Slate100)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No tiles added. Choose from below to customize your focus.",
                            fontSize = 14.sp,
                            color = if (isDark) Slate400 else Slate600
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        allActiveIds.forEach { tileId ->
                            ActiveTileRow(
                                tileId = tileId,
                                metrics = metrics,
                                profile = profile,
                                last7Metrics = last7Metrics,
                                todayFoodLogs = todayFoodLogs,
                                isDark = isDark,
                                onRemove = {
                                    val newAll = allActiveIds.filter { it != tileId }
                                    viewModel.updateTodayTileSlots(newAll)
                                }
                            )
                        }
                    }
                }
            }

            // Section 2: Large Tiles
            item {
                Text(
                    text = "Large tiles",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) MaterialTheme.colorScheme.onSurface else TextPrimary
                )
                Text(
                    text = "Full-width circular ring gauges for high-priority daily goals.",
                    fontSize = 13.sp,
                    color = if (isDark) Slate400 else Slate500,
                    modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ALL_LARGE_TILE_IDS.forEach { largeId ->
                        val isAdded = largeId in activeLargeIds
                        val resolved = resolveLargeTile(
                            id = largeId,
                            metrics = metrics,
                            last7Metrics = last7Metrics,
                            isDark = isDark,
                            stepGoal = profile?.stepGoal ?: 10000,
                            onOpenStepsDialog = {},
                            onNavigateToFitness = {}
                        )

                        Box(modifier = Modifier.weight(1f)) {
                            TileOptionCard(
                                title = resolved?.title ?: largeId,
                                value = resolved?.insideSubtext ?: "",
                                accent = resolved?.accent ?: AccentTokens.stepsAccent(isDark),
                                isAdded = isAdded,
                                isDark = isDark,
                                onAdd = {
                                    if (!isAdded) {
                                        val newAll = allActiveIds + largeId
                                        viewModel.updateTodayTileSlots(newAll)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Section 3: Small Tiles
            item {
                Text(
                    text = "Small tiles",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) MaterialTheme.colorScheme.onSurface else TextPrimary
                )
                Text(
                    text = "Compact metric cards displaying health vitals and activity logs.",
                    fontSize = 13.sp,
                    color = if (isDark) Slate400 else Slate500,
                    modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                )

                // 2-column grid
                val smallTilesChunked = ALL_SMALL_TILE_IDS.chunked(2)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    smallTilesChunked.forEach { rowIds ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowIds.forEach { smallId ->
                                val isAdded = smallId in activeSmallIds
                                val resolved = resolveSmallTile(
                                    id = smallId,
                                    metrics = metrics,
                                    profile = profile,
                                    last7Metrics = last7Metrics,
                                    todayFoodLogs = todayFoodLogs,
                                    isDark = isDark,
                                    navController = androidx.navigation.compose.rememberNavController(),
                                    onOpenStepsDialog = {},
                                    onOpenWaterDialog = {},
                                    onNavigateToTab = {}
                                )

                                if (resolved != null) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        SmallTileOptionCard(
                                            label = resolved.label,
                                            value = resolved.value,
                                            icon = resolved.icon,
                                            accent = resolved.accent,
                                            isAdded = isAdded,
                                            isDark = isDark,
                                            onAdd = {
                                                if (!isAdded) {
                                                    val newAll = allActiveIds + smallId
                                                    viewModel.updateTodayTileSlots(newAll)
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                            if (rowIds.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveTileRow(
    tileId: String,
    metrics: com.example.data.local.DailyMetric?,
    profile: com.example.data.local.UserProfile?,
    last7Metrics: List<com.example.data.local.DailyMetric>,
    todayFoodLogs: List<com.example.data.local.FoodLog>,
    isDark: Boolean,
    onRemove: () -> Unit
) {
    val isLarge = tileId.startsWith("large_")
    val title: String
    val value: String
    val accentColor: AccentColors?

    if (isLarge) {
        val resolved = resolveLargeTile(
            id = tileId,
            metrics = metrics,
            last7Metrics = last7Metrics,
            isDark = isDark,
            stepGoal = profile?.stepGoal ?: 10000,
            onOpenStepsDialog = {},
            onNavigateToFitness = {}
        )
        title = resolved?.title ?: tileId
        value = resolved?.insideSubtext ?: ""
        accentColor = resolved?.accent
    } else {
        val resolved = resolveSmallTile(
            id = tileId,
            metrics = metrics,
            profile = profile,
            last7Metrics = last7Metrics,
            todayFoodLogs = todayFoodLogs,
            isDark = isDark,
            navController = androidx.navigation.compose.rememberNavController(),
            onOpenStepsDialog = {},
            onOpenWaterDialog = {},
            onNavigateToTab = {}
        )
        title = resolved?.label ?: tileId
        value = resolved?.value ?: ""
        accentColor = resolved?.accent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(1.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(if (isDark) MaterialTheme.colorScheme.surfaceVariant else Surface)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (accentColor != null) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(accentColor.onBg)
                )
            }
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDark) MaterialTheme.colorScheme.onSurface else TextPrimary
                    )
                    if (isLarge) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isDark) MaterialTheme.colorScheme.surface else Slate200)
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "Large",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Slate300 else Slate600
                            )
                        }
                    }
                }
                if (value.isNotBlank()) {
                    Text(
                        text = value,
                        fontSize = 12.sp,
                        color = if (isDark) Slate400 else Slate500
                    )
                }
            }
        }

        IconButton(
            onClick = onRemove,
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(if (isDark) MaterialTheme.colorScheme.surface else Slate100)
                .testTag("remove_tile_${tileId}")
        ) {
            Icon(
                imageVector = Icons.Default.Remove,
                contentDescription = "Remove $title",
                tint = if (isDark) Slate300 else Slate600,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun TileOptionCard(
    title: String,
    value: String,
    accent: AccentColors,
    isAdded: Boolean,
    isDark: Boolean,
    onAdd: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(if (isAdded) 0.dp else 1.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(if (isDark) MaterialTheme.colorScheme.surfaceVariant else Surface)
            .border(
                width = if (isAdded) 1.5.dp else 0.dp,
                color = if (isAdded) accent.onBg.copy(alpha = 0.5f) else Color.Transparent,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(enabled = !isAdded) { onAdd() }
            .padding(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) MaterialTheme.colorScheme.onSurface else TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (isAdded) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(accent.onBg.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Added",
                            tint = accent.onBg,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                } else {
                    IconButton(
                        onClick = onAdd,
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(if (isDark) MaterialTheme.colorScheme.surface else Slate100)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add $title",
                            tint = if (isDark) Color.White else TextPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Text(
                text = value,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = if (isDark) Slate400 else Slate500,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SmallTileOptionCard(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: AccentColors?,
    isAdded: Boolean,
    isDark: Boolean,
    onAdd: () -> Unit
) {
    val containerBg = when {
        isAdded -> if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f) else Slate100.copy(alpha = 0.7f)
        accent != null -> accent.bg
        else -> if (isDark) MaterialTheme.colorScheme.surfaceVariant else Surface
    }
    val contentTint = when {
        accent != null -> accent.onBg
        else -> if (isDark) MaterialTheme.colorScheme.onSurface else TextPrimary
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(if (isAdded) 0.dp else 1.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(containerBg)
            .border(
                width = if (isAdded) 1.dp else 0.dp,
                color = if (isAdded) (if (isDark) MaterialTheme.colorScheme.outline else Slate300) else Color.Transparent,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(enabled = !isAdded) { onAdd() }
            .padding(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(
                            if (accent != null) accent.onBg.copy(alpha = 0.15f)
                            else if (isDark) MaterialTheme.colorScheme.surface else Slate200
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = contentTint,
                        modifier = Modifier.size(14.dp)
                    )
                }

                if (isAdded) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(if (isDark) MaterialTheme.colorScheme.surface else Slate200),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Added",
                            tint = if (isDark) Slate300 else Slate600,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                } else {
                    IconButton(
                        onClick = onAdd,
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(if (isDark) MaterialTheme.colorScheme.surface else Slate100)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add $label",
                            tint = if (isDark) Color.White else TextPrimary,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }

            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = if (isDark) Slate400 else Slate500,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (accent != null) accent.onBg else if (isDark) MaterialTheme.colorScheme.onSurface else TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
