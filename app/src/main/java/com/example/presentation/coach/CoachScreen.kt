package com.example.presentation.coach

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.CoachTopic
import com.example.data.local.coachTopics
import com.example.presentation.viewmodel.ShasthoViewModel
import com.example.ui.theme.*
import com.example.ui.components.MarkdownText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoachScreen(viewModel: ShasthoViewModel, onNavigateBack: () -> Unit = {}) {
    val profile by viewModel.userProfile.collectAsState()
    val isDark = profile?.isDarkMode ?: isSystemInDarkTheme()
    val waterAccent = AccentTokens.waterAccent(isDark)

    val coachAdvice by viewModel.coachAdvice.collectAsState()
    val isLoading by viewModel.isLoadingCoach.collectAsState()
    var selectedTopic by remember { mutableStateOf<CoachTopic?>(null) }
    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredTopics = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            coachTopics
        } else {
            val q = searchQuery.trim().lowercase()
            coachTopics.filter { topic ->
                topic.condition.lowercase().contains(q) ||
                topic.habit.lowercase().contains(q) ||
                topic.benefits.lowercase().contains(q)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 8.dp)
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }
            Text(
                text = "Wellness Coach",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { 
                showSearch = !showSearch
                if (!showSearch) {
                    searchQuery = ""
                }
            }) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = TextPrimary
                )
            }
        }

        if (showSearch) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search wellness topics...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                singleLine = true,
                trailingIcon = {
                    IconButton(onClick = {
                        searchQuery = ""
                        showSearch = false
                    }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear search",
                            tint = Slate500
                        )
                    }
                }
            )
        }

        Text(
            text = "Tap a topic to get personalized AI coaching on how to integrate these habits into your life.",
            fontSize = 14.sp,
            color = Slate500,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        if (filteredTopics.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 48.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No matching wellness topics found.",
                    color = Slate500,
                    fontSize = 16.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(filteredTopics) { topic ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedTopic = topic
                            viewModel.requestCoachAdvice(topic.condition, topic.habit, topic.benefits)
                        },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(waterAccent.bg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.MonitorHeart, contentDescription = null, tint = waterAccent.onBg)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = topic.condition, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = topic.habit, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 18.sp)
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(100.dp)) }
        }
    }
}

    if (selectedTopic != null) {
        AlertDialog(
            onDismissRequest = { 
                selectedTopic = null
                viewModel.clearCoachAdvice()
            },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
            modifier = Modifier.fillMaxWidth(0.95f).padding(vertical = 24.dp),
            content = {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Emerald600)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Amar-Fit AI Coach", fontWeight = FontWeight.Bold, color = Emerald600)
                            Spacer(modifier = Modifier.weight(1f))
                            IconButton(onClick = { 
                                selectedTopic = null
                                viewModel.clearCoachAdvice()
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Close")
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(text = selectedTopic!!.condition, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = TextPrimary)
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = Slate100)
                        Spacer(modifier = Modifier.height(16.dp))

                        if (isLoading) {
                            Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = Emerald600)
                            }
                        } else if (coachAdvice != null) {
                            MarkdownText(
                                text = coachAdvice!!,
                                color = TextPrimary,
                                fontSize = 15.sp,
                                lineHeight = 22.sp,
                                modifier = Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState())
                            )
                        }
                    }
                }
            }
        )
    }
}
