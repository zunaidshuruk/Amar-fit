package com.example.presentation.offlineai

import android.net.Uri
import java.util.Locale
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.util.ai.AiEngineSetting

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfflineAiScreen(
    onNavigateBack: () -> Unit,
    viewModel: OfflineAiViewModel = viewModel()
) {
    val modelState by viewModel.modelFileState.collectAsState()
    val downloadState by viewModel.downloadState.collectAsState()
    val loadState by viewModel.loadState.collectAsState()
    val useGpu by viewModel.useGpu.collectAsState()
    val enableMtp by viewModel.enableMtp.collectAsState()
    val aiEngineSetting by viewModel.aiEngineSetting.collectAsState()
    val chatTranscript by viewModel.chatTranscript.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val benchmarkIndex by viewModel.benchmarkIndex.collectAsState()
    val benchmarkResults by viewModel.benchmarkResults.collectAsState()
    val modelUrlInput by viewModel.modelUrlInput.collectAsState()
    val modelSizeLabel by viewModel.modelSizeLabel.collectAsState()

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showReplaceConfirmDialog by remember { mutableStateOf(false) }
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var chatInputText by remember { mutableStateOf("") }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.importModel(uri)
        }
    }

    // Keep screen awake while benchmark is running
    val view = LocalView.current
    val isBenchmarking = benchmarkIndex != null
    DisposableEffect(isBenchmarking) {
        if (isBenchmarking) {
            view.keepScreenOn = true
        }
        onDispose {
            view.keepScreenOn = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Offline AI (beta)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // (a) Note Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Experimental LiteRT-LM Spoke",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Runs a small AI model directly on this phone without network. Nothing here is connected to the Assistant or production features yet.",
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // AI Engine Setting Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "AI Engine Setting",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Choose how the KardIQ Assistant answers your questions.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val options = listOf(
                        Triple(AiEngineSetting.CLOUD, "Cloud (Gemini)", "Default. Best quality for everything."),
                        Triple(AiEngineSetting.AUTO, "Auto", "Simple chat on this phone; food, calories, plans and health numbers use the cloud."),
                        Triple(AiEngineSetting.ON_DEVICE, "On this phone only", "Works offline and never uses the cloud. Food logging, calories, plans and health numbers are not available.")
                    )

                    options.forEach { (setting, title, subtitle) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setAiEngineSetting(setting) }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = aiEngineSetting == setting,
                                onClick = { viewModel.setAiEngineSetting(setting) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = title,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = subtitle,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "The model loads on the first phone answer (about 5 seconds) and unloads after 5 minutes of inactivity.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )
                }
            }

            // (b) Model Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Model: Gemma 4 E2B",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    val sizeMb = modelState.sizeBytes / (1024 * 1024)
                    val freeGb = modelState.freeSpaceBytes / (1024 * 1024 * 1024.0)

                    Text(
                        text = if (modelState.exists) {
                            "Status: Installed ($sizeMb MB)"
                        } else {
                            "Status: Not installed"
                        },
                        fontSize = 13.sp,
                        color = if (modelState.exists) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )

                    if (modelState.exists) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "You can delete the original file to save space.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = String.format(java.util.Locale.US, "Device Free Storage: %.1f GB", freeGb),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = modelUrlInput,
                        onValueChange = { viewModel.setModelUrl(it) },
                        label = { Text("Model link (direct .litertlm URL)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Remote size: $modelSizeLabel",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Smaller models (under 1 GB, such as Gemma 3 1B) use less space but answer less well. Download the file in your browser and use Import.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )

                    when (val dl = downloadState) {
                        is DownloadState.Downloading -> {
                            Spacer(modifier = Modifier.height(10.dp))
                            LinearProgressIndicator(
                                progress = { dl.progressPercent / 100f },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            val dlMb = dl.bytesDownloaded / (1024 * 1024)
                            val totalMb = dl.totalBytes / (1024 * 1024)
                            Text(
                                text = "Downloading / Copying: ${dl.progressPercent}% ($dlMb / $totalMb MB)",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        is DownloadState.Failed -> {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = dl.reason,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        is DownloadState.Completed -> {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Model file ready to load!",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        DownloadState.Idle -> {}
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                val action = { viewModel.startDownload() }
                                if (modelState.exists) {
                                    pendingAction = action
                                    showReplaceConfirmDialog = true
                                } else {
                                    action()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = downloadState !is DownloadState.Downloading,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Download model")
                        }

                        OutlinedButton(
                            onClick = {
                                val action = { filePickerLauncher.launch(arrayOf("*/*")) }
                                if (modelState.exists) {
                                    pendingAction = action
                                    showReplaceConfirmDialog = true
                                } else {
                                    action()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = downloadState !is DownloadState.Downloading
                        ) {
                            Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Import a .litertlm file")
                        }

                        if (modelState.exists) {
                            TextButton(
                                onClick = { showDeleteConfirmDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Delete model")
                            }
                        }
                    }
                }
            }

            // (c) Engine Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Engine Settings & Load",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Hardware Acceleration (GPU)", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                            Text("Uses OpenCL / Vulkan via LiteRT", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = useGpu,
                            onCheckedChange = { viewModel.setUseGpu(it) }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Speculative Decoding (MTP)", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                            Text("Draft tokens for faster generation", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = enableMtp,
                            onCheckedChange = { viewModel.setEnableMtp(it) }
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    when (val ls = loadState) {
                        LoadState.Unloaded -> {
                            Button(
                                onClick = { viewModel.loadModel() },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = modelState.exists
                            ) {
                                Text("Load Model into Memory")
                            }
                        }
                        LoadState.Loading -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Loading model...", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        is LoadState.Loaded -> {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "Loaded successfully! Backend: ${ls.backendName} (${ls.loadMillis} ms)",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontWeight = FontWeight.Bold
                                )
                                OutlinedButton(
                                    onClick = { viewModel.deleteModel() },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Unload / Clear")
                                }
                            }
                        }
                        is LoadState.Error -> {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "Load error: ${ls.message}",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Button(
                                    onClick = { viewModel.loadModel() },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Retry Loading")
                                }
                            }
                        }
                    }
                }
            }

            // (d) Chat Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Quick Chat Test",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    if (chatTranscript.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 240.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            chatTranscript.forEach { msg ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (msg.isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = if (msg.isUser) "You" else "Gemma",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (msg.isUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = msg.text,
                                            fontSize = 13.sp,
                                            color = if (msg.isUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                        )
                                        if (!msg.stats.isNullOrBlank()) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = msg.stats,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = chatInputText,
                            onValueChange = { chatInputText = it },
                            placeholder = { Text("Ask something...") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            enabled = loadState is LoadState.Loaded && !isGenerating
                        )

                        if (isGenerating) {
                            IconButton(
                                onClick = { viewModel.stopGeneration() },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = "Stop", tint = MaterialTheme.colorScheme.error)
                            }
                        } else {
                            Button(
                                onClick = {
                                    if (chatInputText.isNotBlank()) {
                                        viewModel.sendChat(chatInputText)
                                        chatInputText = ""
                                    }
                                },
                                enabled = loadState is LoadState.Loaded && chatInputText.isNotBlank(),
                                modifier = Modifier.height(48.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Send")
                            }
                        }
                    }
                }
            }

            // (e) Benchmark Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Benchmark Test Sheet",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (benchmarkResults.isNotEmpty()) {
                            IconButton(onClick = { viewModel.copyReport() }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy Report")
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Runs 10 standard prompts to measure speed, memory, and thermal stability.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { viewModel.runBenchmark() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = loadState is LoadState.Loaded && benchmarkIndex == null
                    ) {
                        Text(if (benchmarkIndex != null) "Running test $benchmarkIndex / 10..." else "Run 10-Prompt Test Sheet")
                    }

                    if (benchmarkResults.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            benchmarkResults.forEachIndexed { i, res ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${i + 1}. ${res.title}",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = String.format(Locale.US, "1st: %.1fs · %.1f tok/s", res.firstTokenMs / 1000.0, res.approxTokensPerSecond),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(88.dp))
        }
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete Model File?") },
            text = { Text("This will remove the downloaded model file from phone storage.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        viewModel.deleteModel()
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showReplaceConfirmDialog) {
        AlertDialog(
            onDismissRequest = {
                showReplaceConfirmDialog = false
                pendingAction = null
            },
            title = { Text("Replace the current model?") },
            text = { Text("An existing model file is already installed. Replacing it will remove the current model.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showReplaceConfirmDialog = false
                        pendingAction?.invoke()
                        pendingAction = null
                    }
                ) {
                    Text("Replace", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showReplaceConfirmDialog = false
                        pendingAction = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}
