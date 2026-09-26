package com.example.presentation.foodlog

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
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
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun FoodChatScreen(viewModel: ShasthoViewModel, onNavigateBack: () -> Unit = {}) {
    val messages by viewModel.foodChatHistory.collectAsState()
    val isLoading by viewModel.isLoadingFoodChat.collectAsState()
    val loggedConfirmation by viewModel.foodChatLoggedConfirmation.collectAsState()
    val pendingEntry by viewModel.pendingFoodLogEntry.collectAsState()
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val context = androidx.compose.ui.platform.LocalContext.current

    val micPermissionState = rememberPermissionState(android.Manifest.permission.RECORD_AUDIO)

    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                input = if (input.isBlank()) spokenText else "$input $spokenText"
            }
        }
    }

    fun launchVoiceInput() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Ki khaisen? / What did you eat?")
        }
        try {
            speechRecognizerLauncher.launch(intent)
        } catch (e: Exception) {
            android.widget.Toast.makeText(context, "Voice input isn't available on this device.", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    BackHandler(enabled = pendingEntry != null) {
        viewModel.discardPendingFoodLog()
    }

    LaunchedEffect(Unit) { viewModel.resetFoodChat() }
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }
    LaunchedEffect(loggedConfirmation) {
        loggedConfirmation?.let {
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_LONG).show()
            viewModel.clearFoodChatLoggedConfirmation()
        }
    }
    LaunchedEffect(micPermissionState.status) {
        if (micPermissionState.status.isGranted) {
            // Permission just became granted -- no auto-launch here to avoid surprising the
            // user with a mic prompt they didn't just tap for; they'll tap the mic again.
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Log Food with AI") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = {
                        if (micPermissionState.status.isGranted) {
                            launchVoiceInput()
                        } else {
                            micPermissionState.launchPermissionRequest()
                        }
                    },
                    enabled = !isLoading && pendingEntry == null
                ) {
                    Icon(
                        Icons.Default.Mic,
                        contentDescription = "Voice input",
                        tint = if (!isLoading && pendingEntry == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                }
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    placeholder = { Text("Ki khaisen? / What did you eat?") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    enabled = !isLoading && pendingEntry == null
                )
                IconButton(
                    onClick = {
                        if (input.isNotBlank() && !isLoading && pendingEntry == null) {
                            viewModel.sendFoodChatMessage(input)
                            input = ""
                        }
                    },
                    enabled = !isLoading && pendingEntry == null
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send, 
                        contentDescription = "Send", 
                        tint = if (!isLoading && pendingEntry == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (messages.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "Tell me what you ate -- in English, Bangla, or Banglish. e.g. \"ami 2 ta porota r ek cup cha khaisi\"",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(24.dp),
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(messages) { msg ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (msg.isUser) Arrangement.End else Arrangement.Start
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (msg.isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = msg.text,
                                    color = if (msg.isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(8.dp)) }
                }
            }

            // Overlay for pending food review
            pendingEntry?.let { entry ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.75f))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(24.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Review Meal",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            IconButton(onClick = { viewModel.discardPendingFoodLog() }) {
                                Icon(Icons.Default.Close, contentDescription = "Close")
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )

                        Text(
                            text = "${entry.name} (${entry.calories} kcal)\n\nCategory: ${entry.category} • Meal: ${entry.mealType}${if (entry.description.isNotBlank()) "\n\n" + entry.description else ""}",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 15.sp,
                            modifier = Modifier.verticalScroll(rememberScrollState()).weight(1f, fill = false)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Carbs", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${entry.carbsG.toInt()}g", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Protein", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${entry.proteinG.toInt()}g", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Fat", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${entry.fatG.toInt()}g", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                viewModel.confirmPendingFoodLog()
                            },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text("Awesome! Log this meal")
                        }
                    }
                }
            }
        }
    }
}
