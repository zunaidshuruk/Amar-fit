package com.example.presentation.assistant

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.SavedChat
import com.example.presentation.viewmodel.ChatMessage
import com.example.ui.components.AiMessageCard

@Composable
fun LegacyChatViewer(chat: SavedChat, onBack: () -> Unit) {
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = chat.title.ifBlank { "Saved chat" },
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
                if (message.isUser) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(
                                    RoundedCornerShape(
                                        topStart = 16.dp,
                                        topEnd = 16.dp,
                                        bottomStart = 16.dp,
                                        bottomEnd = 0.dp
                                    )
                                )
                                .background(MaterialTheme.colorScheme.primary)
                                .padding(16.dp)
                                .widthIn(max = 280.dp)
                        ) {
                            Text(text = message.text, color = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                } else {
                    AiMessageCard(text = message.text, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}
