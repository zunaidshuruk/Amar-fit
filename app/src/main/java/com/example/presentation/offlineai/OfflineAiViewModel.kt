package com.example.presentation.offlineai

import android.app.ActivityManager
import android.app.Application
import android.app.DownloadManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.os.StatFs
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.util.ai.BenchmarkResult
import com.example.util.ai.LoadResult
import com.example.util.ai.OnDeviceBenchmark
import com.example.util.ai.OnDeviceLlm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

data class ModelFileState(
    val path: String = "",
    val exists: Boolean = false,
    val sizeBytes: Long = 0L,
    val freeSpaceBytes: Long = 0L
)

sealed class DownloadState {
    object Idle : DownloadState()
    data class Downloading(val progressPercent: Int, val bytesDownloaded: Long, val totalBytes: Long) : DownloadState()
    data class Failed(val reason: String) : DownloadState()
    object Completed : DownloadState()
}

sealed class LoadState {
    object Unloaded : LoadState()
    object Loading : LoadState()
    data class Loaded(val backendName: String, val loadMillis: Long) : LoadState()
    data class Error(val message: String) : LoadState()
}

data class ChatMessage(
    val isUser: Boolean,
    val text: String,
    val stats: String? = null
)

class OfflineAiViewModel(application: Application) : AndroidViewModel(application) {

    private val tag = "OfflineAiViewModel"
    private val llm = OnDeviceLlm(application)

    val defaultModelFileName = "gemma-4-E2B-it.litertlm"
    private val modelUrl = "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it.litertlm"

    private val _modelFileState = MutableStateFlow(ModelFileState())
    val modelFileState: StateFlow<ModelFileState> = _modelFileState.asStateFlow()

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    private val _loadState = MutableStateFlow<LoadState>(LoadState.Unloaded)
    val loadState: StateFlow<LoadState> = _loadState.asStateFlow()

    private val _useGpu = MutableStateFlow(true)
    val useGpu: StateFlow<Boolean> = _useGpu.asStateFlow()

    private val _enableMtp = MutableStateFlow(true)
    val enableMtp: StateFlow<Boolean> = _enableMtp.asStateFlow()

    private val _chatTranscript = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatTranscript: StateFlow<List<ChatMessage>> = _chatTranscript.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _benchmarkIndex = MutableStateFlow<Int?>(null) // null if not running, 1..10
    val benchmarkIndex: StateFlow<Int?> = _benchmarkIndex.asStateFlow()

    private val _benchmarkResults = MutableStateFlow<List<BenchmarkResult>>(emptyList())
    val benchmarkResults: StateFlow<List<BenchmarkResult>> = _benchmarkResults.asStateFlow()

    private var activeGenerationJob: Job? = null
    private var downloadPollingJob: Job? = null
    private var activeDownloadId: Long? = null
    private var lastLoadResult: LoadResult? = null

    init {
        refreshModelFileState()
    }

    fun setUseGpu(enabled: Boolean) {
        _useGpu.value = enabled
    }

    fun setEnableMtp(enabled: Boolean) {
        _enableMtp.value = enabled
    }

    private fun getModelsDir(): File {
        val dir = File(getApplication<Application>().filesDir, "models")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getModelFile(): File = File(getModelsDir(), defaultModelFileName)

    fun refreshModelFileState() {
        val file = getModelFile()
        val exists = file.exists() && file.length() > 0L
        val size = if (exists) file.length() else 0L

        val freeSpace = try {
            val stat = StatFs(getApplication<Application>().filesDir.absolutePath)
            stat.availableBlocksLong * stat.blockSizeLong
        } catch (e: Exception) {
            0L
        }

        _modelFileState.value = ModelFileState(
            path = file.absolutePath,
            exists = exists,
            sizeBytes = size,
            freeSpaceBytes = freeSpace
        )
    }

    fun startDownload() {
        val context = getApplication<Application>()
        refreshModelFileState()

        val requiredBytes = 4L * 1024L * 1024L * 1024L // 4 GB
        if (_modelFileState.value.freeSpaceBytes < requiredBytes) {
            _downloadState.value = DownloadState.Failed("Not enough free space (needs about 4 GB)")
            return
        }

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
        if (downloadManager == null) {
            _downloadState.value = DownloadState.Failed("DownloadManager not available on this device")
            return
        }

        try {
            val destDir = context.getExternalFilesDir("models")
            if (destDir != null && !destDir.exists()) destDir.mkdirs()
            val destFile = File(destDir, defaultModelFileName)
            if (destFile.exists()) destFile.delete()

            val request = DownloadManager.Request(Uri.parse(modelUrl))
                .setTitle("Gemma 4 E2B")
                .setDescription("Downloading LiteRT-LM model over Wi-Fi")
                .setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationUri(Uri.fromFile(destFile))

            val downloadId = downloadManager.enqueue(request)
            activeDownloadId = downloadId
            _downloadState.value = DownloadState.Downloading(0, 0, 0)

            downloadPollingJob?.cancel()
            downloadPollingJob = viewModelScope.launch(Dispatchers.IO) {
                pollDownloadProgress(downloadManager, downloadId, destFile)
            }
        } catch (t: Throwable) {
            Log.e(tag, "Failed to start download", t)
            _downloadState.value = DownloadState.Failed("Could not start download: ${t.message}. Use Import instead.")
        }
    }

    private suspend fun pollDownloadProgress(
        downloadManager: DownloadManager,
        downloadId: Long,
        tempDestFile: File
    ) {
        var completed = false
        while (!completed && viewModelScope.isActive) {
            delay(1000)
            val query = DownloadManager.Query().setFilterById(downloadId)
            val cursor = downloadManager.query(query)
            if (cursor != null && cursor.moveToFirst()) {
                val statusIdx = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                val reasonIdx = cursor.getColumnIndex(DownloadManager.COLUMN_REASON)
                val downloadedIdx = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                val totalIdx = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)

                val status = if (statusIdx >= 0) cursor.getInt(statusIdx) else -1
                val reason = if (reasonIdx >= 0) cursor.getInt(reasonIdx) else -1
                val downloaded = if (downloadedIdx >= 0) cursor.getLong(downloadedIdx) else 0L
                val total = if (totalIdx >= 0) cursor.getLong(totalIdx) else 0L

                cursor.close()

                when (status) {
                    DownloadManager.STATUS_RUNNING, DownloadManager.STATUS_PAUSED -> {
                        val percent = if (total > 0) ((downloaded * 100) / total).toInt() else 0
                        _downloadState.value = DownloadState.Downloading(percent, downloaded, total)
                    }
                    DownloadManager.STATUS_SUCCESSFUL -> {
                        completed = true
                        onDownloadFinished(tempDestFile)
                    }
                    DownloadManager.STATUS_FAILED -> {
                        completed = true
                        val reasonText = mapDownloadManagerReason(reason)
                        _downloadState.value = DownloadState.Failed(
                            "Download failed: $reasonText. Model might require Hugging Face login or Wi-Fi. Please use Import."
                        )
                    }
                }
            } else {
                cursor?.close()
            }
        }
    }

    private suspend fun onDownloadFinished(tempDestFile: File) {
        withContext(Dispatchers.IO) {
            try {
                val targetFile = getModelFile()
                if (tempDestFile.exists()) {
                    if (targetFile.exists()) targetFile.delete()
                    val copied = tempDestFile.renameTo(targetFile)
                    if (!copied) {
                        tempDestFile.copyTo(targetFile, overwrite = true)
                        tempDestFile.delete()
                    }
                }
                refreshModelFileState()
                _downloadState.value = DownloadState.Completed
            } catch (t: Throwable) {
                Log.e(tag, "Failed to move downloaded model", t)
                _downloadState.value = DownloadState.Failed("Error copying downloaded file: ${t.message}")
            }
        }
    }

    private fun mapDownloadManagerReason(reason: Int): String {
        return when (reason) {
            DownloadManager.ERROR_CANNOT_RESUME -> "Cannot resume"
            DownloadManager.ERROR_DEVICE_NOT_FOUND -> "Storage not found"
            DownloadManager.ERROR_FILE_ALREADY_EXISTS -> "File already exists"
            DownloadManager.ERROR_FILE_ERROR -> "File error"
            DownloadManager.ERROR_HTTP_DATA_ERROR -> "HTTP data error"
            DownloadManager.ERROR_INSUFFICIENT_SPACE -> "Insufficient storage space"
            DownloadManager.ERROR_TOO_MANY_REDIRECTS -> "Too many redirects"
            DownloadManager.ERROR_UNHANDLED_HTTP_CODE -> "HTTP status error (e.g. 401/403 auth required)"
            DownloadManager.ERROR_UNKNOWN -> "Network or server error"
            DownloadManager.PAUSED_QUEUED_FOR_WIFI -> "Waiting for Wi-Fi"
            DownloadManager.PAUSED_WAITING_FOR_NETWORK -> "Waiting for network"
            DownloadManager.PAUSED_WAITING_TO_RETRY -> "Waiting to retry"
            else -> "Error code $reason"
        }
    }

    fun importModel(uri: Uri) {
        val context = getApplication<Application>()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val contentResolver = context.contentResolver
                val targetFile = getModelFile()
                if (targetFile.exists()) targetFile.delete()

                val inputStream = contentResolver.openInputStream(uri)
                    ?: throw IllegalStateException("Could not open input stream from file picker")

                _downloadState.value = DownloadState.Downloading(0, 0, 0)

                val outputStream = FileOutputStream(targetFile)
                val buffer = ByteArray(64 * 1024)
                var bytesRead: Int
                var totalRead = 0L

                val fileSize = try {
                    contentResolver.openFileDescriptor(uri, "r")?.use { it.statSize } ?: 0L
                } catch (e: Exception) {
                    0L
                }

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    totalRead += bytesRead
                    if (fileSize > 0) {
                        val pct = ((totalRead * 100) / fileSize).toInt()
                        _downloadState.value = DownloadState.Downloading(pct, totalRead, fileSize)
                    }
                }

                outputStream.flush()
                outputStream.close()
                inputStream.close()

                refreshModelFileState()
                _downloadState.value = DownloadState.Completed
            } catch (t: Throwable) {
                Log.e(tag, "Failed to import model", t)
                _downloadState.value = DownloadState.Failed("Import failed: ${t.message}")
            }
        }
    }

    fun deleteModel() {
        viewModelScope.launch(Dispatchers.IO) {
            llm.close()
            _loadState.value = LoadState.Unloaded
            val file = getModelFile()
            if (file.exists()) {
                file.delete()
            }
            refreshModelFileState()
            _downloadState.value = DownloadState.Idle
        }
    }

    fun loadModel() {
        val file = getModelFile()
        if (!file.exists() || file.length() == 0L) {
            _loadState.value = LoadState.Error("Model file not found. Download or import it first.")
            return
        }

        viewModelScope.launch {
            _loadState.value = LoadState.Loading
            val result = llm.load(
                modelPath = file.absolutePath,
                useGpu = _useGpu.value,
                enableMtp = _enableMtp.value
            )
            lastLoadResult = result
            if (result.ok) {
                _loadState.value = LoadState.Loaded(result.backendName, result.loadMillis)
            } else {
                _loadState.value = LoadState.Error(result.error ?: "Failed to load model")
            }
        }
    }

    fun sendChat(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || _isGenerating.value) return

        if (!llm.isLoaded()) {
            Toast.makeText(getApplication(), "Please load the model first", Toast.LENGTH_SHORT).show()
            return
        }

        val updated = _chatTranscript.value.toMutableList()
        updated.add(ChatMessage(isUser = true, text = trimmed))
        val assistantIndex = updated.size
        updated.add(ChatMessage(isUser = false, text = "...", stats = null))
        _chatTranscript.value = updated
        _isGenerating.value = true

        activeGenerationJob?.cancel()
        activeGenerationJob = viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            var firstTokenTime = 0L
            val answerBuilder = StringBuilder()

            try {
                llm.generate(trimmed).collect { chunk ->
                    if (firstTokenTime == 0L) {
                        firstTokenTime = System.currentTimeMillis()
                    }
                    answerBuilder.append(chunk)

                    val currentList = _chatTranscript.value.toMutableList()
                    if (assistantIndex < currentList.size) {
                        currentList[assistantIndex] = ChatMessage(
                            isUser = false,
                            text = answerBuilder.toString()
                        )
                        _chatTranscript.value = currentList
                    }
                }

                val totalTime = System.currentTimeMillis() - startTime
                val firstTokenMs = if (firstTokenTime > 0L) (firstTokenTime - startTime) else totalTime
                val tps = OnDeviceBenchmark.calculateTokensPerSecond(answerBuilder.length, firstTokenMs, totalTime)

                val statsLine = String.format(
                    java.util.Locale.US,
                    "first word %.1f s · about %.0f words/s",
                    firstTokenMs / 1000.0,
                    tps
                )

                val finalTranscript = _chatTranscript.value.toMutableList()
                if (assistantIndex < finalTranscript.size) {
                    finalTranscript[assistantIndex] = ChatMessage(
                        isUser = false,
                        text = answerBuilder.toString(),
                        stats = statsLine
                    )
                    _chatTranscript.value = finalTranscript
                }
            } catch (t: Throwable) {
                Log.e(tag, "Chat generation error", t)
                val finalTranscript = _chatTranscript.value.toMutableList()
                if (assistantIndex < finalTranscript.size) {
                    finalTranscript[assistantIndex] = ChatMessage(
                        isUser = false,
                        text = "Error: ${t.message}",
                        stats = null
                    )
                    _chatTranscript.value = finalTranscript
                }
            } finally {
                _isGenerating.value = false
            }
        }
    }

    fun stopGeneration() {
        activeGenerationJob?.cancel()
        _isGenerating.value = false
    }

    fun runBenchmark() {
        if (!llm.isLoaded()) {
            Toast.makeText(getApplication(), "Please load the model first", Toast.LENGTH_SHORT).show()
            return
        }

        if (_benchmarkIndex.value != null) return

        viewModelScope.launch {
            _benchmarkResults.value = emptyList()
            val context = getApplication<Application>()

            val initialMemory = getProcessMemoryPss(context)
            val initialBattery = getBatteryCapacity(context)
            val initialThermal = getThermalStatus(context)

            val results = mutableListOf<BenchmarkResult>()

            for ((index, promptPair) in OnDeviceBenchmark.BENCHMARK_PROMPTS.withIndex()) {
                _benchmarkIndex.value = index + 1
                val (title, prompt) = promptPair

                val startTime = System.currentTimeMillis()
                var firstTokenTime = 0L
                val out = StringBuilder()

                try {
                    llm.generate(prompt).collect { chunk ->
                        if (firstTokenTime == 0L) {
                            firstTokenTime = System.currentTimeMillis()
                        }
                        out.append(chunk)
                    }
                } catch (t: Throwable) {
                    out.append("\n[ERROR: ${t.message}]")
                }

                val totalTime = System.currentTimeMillis() - startTime
                val firstTokenMs = if (firstTokenTime > 0L) (firstTokenTime - startTime) else totalTime
                val tps = OnDeviceBenchmark.calculateTokensPerSecond(out.length, firstTokenMs, totalTime)

                val bench = BenchmarkResult(
                    title = title,
                    firstTokenMs = firstTokenMs,
                    totalMs = totalTime,
                    outputChars = out.length,
                    approxTokensPerSecond = tps,
                    output = out.toString().trim()
                )
                results.add(bench)
                _benchmarkResults.value = results.toList()
            }

            _benchmarkIndex.value = null

            val finalMemory = getProcessMemoryPss(context)
            val finalBattery = getBatteryCapacity(context)
            val finalThermal = getThermalStatus(context)

            val memoryNote = "Initial: $initialMemory MB | Final: $finalMemory MB"
            val batteryNote = "Battery: $initialBattery% -> $finalBattery% | Thermal: $initialThermal -> $finalThermal"

            val report = OnDeviceBenchmark.buildReport(
                deviceInfo = "${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE}, API ${Build.VERSION.SDK_INT})",
                settings = "GPU: ${_useGpu.value}, SpeculativeDecoding(MTP): ${_enableMtp.value}",
                loadResult = lastLoadResult,
                results = results,
                memoryNote = memoryNote,
                batteryNote = batteryNote
            )

            // Save report to clipboard automatically or on button tap
            copyTextToClipboard(context, report)
            Toast.makeText(context, "Benchmark finished! Report copied to clipboard", Toast.LENGTH_LONG).show()
        }
    }

    fun copyReport() {
        val context = getApplication<Application>()
        val results = _benchmarkResults.value
        if (results.isEmpty()) {
            Toast.makeText(context, "Run test sheet first to generate a report", Toast.LENGTH_SHORT).show()
            return
        }

        val mem = getProcessMemoryPss(context)
        val bat = getBatteryCapacity(context)
        val thermal = getThermalStatus(context)

        val report = OnDeviceBenchmark.buildReport(
            deviceInfo = "${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE}, API ${Build.VERSION.SDK_INT})",
            settings = "GPU: ${_useGpu.value}, SpeculativeDecoding(MTP): ${_enableMtp.value}",
            loadResult = lastLoadResult,
            results = results,
            memoryNote = "Current process: $mem MB",
            batteryNote = "Battery: $bat% | Thermal: $thermal"
        )
        copyTextToClipboard(context, report)
        Toast.makeText(context, "Report copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    private fun copyTextToClipboard(context: Context, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = ClipData.newPlainText("KardIQ Offline AI Benchmark", text)
        clipboard?.setPrimaryClip(clip)
    }

    private fun getProcessMemoryPss(context: Context): Int {
        return try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val memInfo = am?.getProcessMemoryInfo(intArrayOf(android.os.Process.myPid()))
            if (memInfo != null && memInfo.isNotEmpty()) {
                memInfo[0].totalPss / 1024
            } else 0
        } catch (e: Exception) {
            0
        }
    }

    private fun getBatteryCapacity(context: Context): Int {
        return try {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
        } catch (e: Exception) {
            -1
        }
    }

    private fun getThermalStatus(context: Context): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return try {
                val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                when (pm?.currentThermalStatus) {
                    PowerManager.THERMAL_STATUS_NONE -> "NONE"
                    PowerManager.THERMAL_STATUS_LIGHT -> "LIGHT"
                    PowerManager.THERMAL_STATUS_MODERATE -> "MODERATE"
                    PowerManager.THERMAL_STATUS_SEVERE -> "SEVERE"
                    PowerManager.THERMAL_STATUS_CRITICAL -> "CRITICAL"
                    PowerManager.THERMAL_STATUS_EMERGENCY -> "EMERGENCY"
                    PowerManager.THERMAL_STATUS_SHUTDOWN -> "SHUTDOWN"
                    else -> "UNKNOWN"
                }
            } catch (e: Exception) {
                "N/A"
            }
        }
        return "N/A (<API29)"
    }

    override fun onCleared() {
        super.onCleared()
        activeGenerationJob?.cancel()
        downloadPollingJob?.cancel()
        llm.close()
    }
}
