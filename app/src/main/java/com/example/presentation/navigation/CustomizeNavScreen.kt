package com.example.presentation.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.example.presentation.viewmodel.ShasthoViewModel
import com.example.tabForRoute

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomizeNavScreen(
    viewModel: ShasthoViewModel,
    onNavigateBack: () -> Unit
) {
    val profile by viewModel.userProfile.collectAsState()
    val currentBar = remember(profile?.navTabs) { NavTabs.parse(profile?.navTabs) }
    val hiddenRoutes = remember(currentBar) { NavTabs.hidden(currentBar) }
    val coroutineScope = rememberCoroutineScope()

    fun updateBar(newBar: List<String>) {
        viewModel.userProfile.value?.let { p ->
            coroutineScope.launch {
                viewModel.saveProfile(p.copy(navTabs = NavTabs.toCsv(newBar)))
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
            .imePadding()
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp, top = 24.dp)
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                text = "Customize navigation bar",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Info Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Text(
                text = "Today is always first. Choose up to 3 more tabs; everything else goes under More.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )
        }

        // Live preview Row
        Text(
            text = "Preview",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AssistChip(
                    onClick = {},
                    label = { Text("Today") },
                    leadingIcon = { Icon(Icons.Rounded.Home, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                currentBar.forEach { route ->
                    val tab = tabForRoute(route)
                    if (tab != null) {
                        AssistChip(
                            onClick = {},
                            label = { Text(tab.label) },
                            leadingIcon = { Icon(tab.icon, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        )
                    }
                }
                AssistChip(
                    onClick = {},
                    label = { Text("More") },
                    leadingIcon = { Icon(Icons.Rounded.GridView, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }
        }

        // Section: In the bar
        Text(
            text = "In the bar",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                currentBar.forEachIndexed { index, route ->
                    val tab = tabForRoute(route)
                    if (tab != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                tab.icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = tab.label,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { updateBar(NavTabs.moveUp(currentBar, route)) },
                                enabled = index > 0
                            ) {
                                Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up")
                            }
                            IconButton(
                                onClick = { updateBar(NavTabs.moveDown(currentBar, route)) },
                                enabled = index < currentBar.size - 1
                            ) {
                                Icon(Icons.Default.ArrowDownward, contentDescription = "Move Down")
                            }
                            IconButton(
                                onClick = { updateBar(NavTabs.toggle(currentBar, route)) },
                                enabled = currentBar.size > 1
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Remove")
                            }
                        }
                        if (index < currentBar.size - 1) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
            }
        }

        // Section: In More
        Text(
            text = "In More",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        if (currentBar.size >= NavTabs.MAX_BAR) {
            Text(
                text = "The bar is full: remove a tab first",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                hiddenRoutes.forEachIndexed { index, route ->
                    val tab = tabForRoute(route)
                    if (tab != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                tab.icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = tab.label,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { updateBar(NavTabs.toggle(currentBar, route)) },
                                enabled = currentBar.size < NavTabs.MAX_BAR
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add to Bar")
                            }
                        }
                        if (index < hiddenRoutes.size - 1) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
            }
        }

        TextButton(
            onClick = { updateBar(NavTabs.DEFAULT_BAR) },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("Reset to default")
        }

        Spacer(modifier = Modifier.height(100.dp))
    }
}
