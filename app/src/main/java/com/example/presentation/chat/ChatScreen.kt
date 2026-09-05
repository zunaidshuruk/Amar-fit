package com.example.presentation.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.presentation.viewmodel.ShasthoViewModel
import com.example.ui.theme.*
import com.example.ui.components.MarkdownText

@Composable
fun ChatScreen(viewModel: ShasthoViewModel) {
    val chatHistory by viewModel.chatHistory.collectAsState()
    val isLoading by viewModel.isLoadingChat.collectAsState()
    var currentMessage by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 16.dp, vertical = 24.dp)
        ) {
            Text(
                text = "AI Diet & Recipe Chat",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Emerald900
            )
        }
        
        HorizontalDivider(color = Slate100)

        // Chat list
        LazyColumn(
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
                        color = Slate500,
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
                                .background(if (message.isUser) Emerald600 else Color.White)
                                .padding(16.dp)
                                .widthIn(max = 280.dp)
                        ) {
                            if (message.isUser) {
                                Text(
                                    text = displayMsg,
                                    color = Color.White
                                )
                            } else {
                                MarkdownText(
                                    text = displayMsg,
                                    color = TextPrimary
                                )
                            }
                        }
                        
                        if (hasYouTubeSearch && youtubeQuery.isNotBlank()) {
                            val context = androidx.compose.ui.platform.LocalContext.current
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
                .background(Color.White)
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
                    unfocusedBorderColor = Slate100
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
    }
}
