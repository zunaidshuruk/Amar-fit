package com.example.presentation.chat

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.SavedChat
import com.example.presentation.viewmodel.ChatMessage
import com.example.presentation.viewmodel.ShasthoViewModel
import com.example.ui.theme.*
import com.example.ui.components.MarkdownText
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ChatScreen(viewModel: ShasthoViewModel, initialTab: Int = 0, onNavigateBack: () -> Unit = {}) {
    val profile by viewModel.userProfile.collectAsState()
    val isDark = profile?.isDarkMode ?: isSystemInDarkTheme()
    val chatHistory by viewModel.chatHistory.collectAsState()
    val isLoading by viewModel.isLoadingChat.collectAsState()
    val savedChats by viewModel.savedChats.collectAsState()

    var selectedTab by remember { mutableStateOf(initialTab) } // 0 = Active Chat, 1 = Saved Chats
    var selectedSavedChat by remember { mutableStateOf<SavedChat?>(null) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var chatTitle by remember { mutableStateOf("") }
    var currentMessage by remember { mutableStateOf("") }

    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.syncErrorEvent.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 16.dp, vertical = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = if (isDark) MaterialTheme.colorScheme.onSurface else Emerald900
                        )
                    }
                    Text(
                        text = "AI Diet & Recipe Chat",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) MaterialTheme.colorScheme.onSurface else Emerald900
                    )
                }

                // Save Conversation Button in Header
                IconButton(
                    onClick = {
                        if (chatHistory.isEmpty()) {
                            Toast.makeText(context, "Chat history is empty", Toast.LENGTH_SHORT).show()
                        } else {
                            val defaultTitle = chatHistory.firstOrNull()?.text?.take(30)?.let { if (it.length == 30) "$it..." else it } ?: "Chat Session"
                            chatTitle = defaultTitle
                            showSaveDialog = true
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = "Save Conversation",
                        tint = Emerald600
                    )
                }
            }
        }
        
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        // Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Button(
                onClick = { selectedTab = 0; selectedSavedChat = null },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedTab == 0) Emerald600 else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (selectedTab == 0) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.weight(1f).padding(end = 4.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Active Chat", color = if (selectedTab == 0) Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(
                onClick = { selectedTab = 1 },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedTab == 1) Emerald600 else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (selectedTab == 1) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.weight(1f).padding(start = 4.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Saved Chats (${savedChats.size})", color = if (selectedTab == 1) Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        if (selectedTab == 0) {
            // Active Chat View
            val listState = rememberLazyListState()

            LaunchedEffect(chatHistory.size, chatHistory.lastOrNull()?.text?.length) {
                if (chatHistory.isNotEmpty()) {
                    listState.animateScrollToItem(chatHistory.size + if (isLoading) 1 else 0)
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (chatHistory.isEmpty()) {
                    item {
                        Text(
                            "Ask me anything! e.g., 'What should I eat for breakfast today?'",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
                
                items(chatHistory) { message ->
                    val hasYouTubeSearch = message.text.contains("YOUTUBE_SEARCH:")
                    val displayMsg = if (hasYouTubeSearch) {
                        message.text.substringBefore("YOUTUBE_SEARCH:").trim()
                    } else {
                        message.text
                    }
                    val youtubeQuery = if (hasYouTubeSearch) {
                        message.text.substringAfter("YOUTUBE_SEARCH:").trim()
                    } else ""
                    
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = if (message.isUser) Alignment.CenterEnd else Alignment.CenterStart
                    ) {
                        Column(horizontalAlignment = if (message.isUser) Alignment.End else Alignment.Start) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(
                                        topStart = 16.dp,
                                        topEnd = 16.dp,
                                        bottomStart = if (message.isUser) 16.dp else 0.dp,
                                        bottomEnd = if (message.isUser) 0.dp else 16.dp
                                    ))
                                    .background(if (message.isUser) Emerald600 else MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(16.dp)
                                    .widthIn(max = 280.dp)
                            ) {
                                if (message.isUser) {
                                    Text(text = displayMsg, color = Color.White)
                                } else {
                                    MarkdownText(text = displayMsg, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            
                            if (hasYouTubeSearch && youtubeQuery.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://www.youtube.com/results?search_query=How+to+cook+$youtubeQuery"))
                                        context.startActivity(intent)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Orange700),
                                    modifier = Modifier.padding(start = 8.dp)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = "Watch", tint = Color.White)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Watch Tutorial on YouTube", color = Color.White)
                                }
                            }
                        }
                    }
                }
                
                if (isLoading) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Emerald500
                            )
                        }
                    }
                }
            }

            // Input
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = currentMessage,
                    onValueChange = { currentMessage = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Ask about recipes...") },
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Emerald500,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (currentMessage.isNotBlank()) {
                            viewModel.sendChatMessage(currentMessage)
                            currentMessage = ""
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(Emerald600, RoundedCornerShape(24.dp))
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White)
                }
            }
        } else {
            // Saved Chats Tab
            if (selectedSavedChat != null) {
                SavedChatDetailView(
                    chat = selectedSavedChat!!,
                    onBack = { selectedSavedChat = null }
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (savedChats.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No saved chats yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else {
                        items(savedChats) { chat ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedSavedChat = chat },
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = chat.title,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.US).format(Date(chat.createdAt)),
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(onClick = { viewModel.deleteChat(chat) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Red500)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save Conversation") },
            text = {
                OutlinedTextField(
                    value = chatTitle,
                    onValueChange = { chatTitle = it },
                    label = { Text("Chat Title") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.saveChat(chatTitle)
                        showSaveDialog = false
                        Toast.makeText(context, "Conversation saved!", Toast.LENGTH_SHORT).show()
                        selectedTab = 1
                    }
                ) {
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
}

@Composable
fun SavedChatDetailView(chat: SavedChat, onBack: () -> Unit) {
    val messages = remember(chat.messages) {
        try {
            val listType = com.squareup.moshi.Types.newParameterizedType(List::class.java, ChatMessage::class.java)
            val adapter = com.example.data.remote.RetrofitClient.moshi.adapter<List<ChatMessage>>(listType)
            adapter.fromJson(chat.messages) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
            }
            Text(
                text = chat.title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(messages) { message ->
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = if (message.isUser) Alignment.CenterEnd else Alignment.CenterStart
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(
                                topStart = 16.dp,
                                topEnd = 16.dp,
                                bottomStart = if (message.isUser) 16.dp else 0.dp,
                                bottomEnd = if (message.isUser) 0.dp else 16.dp
                            ))
                            .background(if (message.isUser) Emerald600 else MaterialTheme.colorScheme.surfaceVariant)
                            .padding(16.dp)
                            .widthIn(max = 280.dp)
                    ) {
                        if (message.isUser) {
                            Text(text = message.text, color = Color.White)
                        } else {
                            MarkdownText(text = message.text, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
