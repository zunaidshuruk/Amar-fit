package com.example.util.ai

import android.content.Context
import android.util.Log
import com.google.ai.edge.litertlm.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

data class LoadResult(
    val ok: Boolean,
    val backendName: String,
    val loadMillis: Long,
    val error: String? = null
)

class OnDeviceLlm(private val context: Context) {

    private val tag = "OnDeviceLlm"
    private var engine: Engine? = null
    private var activeBackendName: String = "None"

    private val generationMutex = Mutex()
    private val idleScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var idleJob: Job? = null

    companion object {
        @Volatile
        private var instance: OnDeviceLlm? = null

        fun shared(context: Context): OnDeviceLlm = instance ?: synchronized(this) {
            instance ?: OnDeviceLlm(context.applicationContext).also { instance = it }
        }
    }

    @Synchronized
    fun isLoaded(): Boolean = engine?.isInitialized() == true

    private fun scheduleIdleUnload() {
        idleJob?.cancel()
        idleJob = idleScope.launch {
            delay(5 * 60 * 1000L) // 5 minutes
            close()
        }
    }

    suspend fun ensureLoaded(): LoadResult = withContext(Dispatchers.IO) {
        if (isLoaded()) {
            return@withContext LoadResult(ok = true, backendName = activeBackendName, loadMillis = 0)
        }
        val modelFile = OnDeviceModelStore.findModelFile(context)
        if (modelFile == null || !modelFile.exists()) {
            return@withContext LoadResult(ok = false, backendName = "None", loadMillis = 0, error = "Model file not found")
        }
        val prefs = context.getSharedPreferences("ShasthoPrefs", Context.MODE_PRIVATE)
        val useGpu = prefs.getBoolean("ai_use_gpu", true)
        val enableMtp = prefs.getBoolean("ai_use_mtp", true)
        load(modelFile.absolutePath, useGpu = useGpu, enableMtp = enableMtp)
    }

    suspend fun load(modelPath: String, useGpu: Boolean, enableMtp: Boolean): LoadResult =
        withContext(Dispatchers.Default) {
            close()

            val startTime = System.currentTimeMillis()

            if (useGpu) {
                try {
                    tryLoadEngine(modelPath, Backend.GPU(), "GPU", enableMtp)
                    val elapsed = System.currentTimeMillis() - startTime
                    activeBackendName = "GPU"
                    scheduleIdleUnload()
                    return@withContext LoadResult(
                        ok = true,
                        backendName = "GPU",
                        loadMillis = elapsed
                    )
                } catch (t: Throwable) {
                    Log.w(tag, "GPU initialization failed, falling back to CPU", t)
                }
            }

            // CPU attempt (either requested or fallback)
            try {
                tryLoadEngine(modelPath, Backend.CPU(), "CPU", enableMtp = false)
                val elapsed = System.currentTimeMillis() - startTime
                activeBackendName = "CPU"
                scheduleIdleUnload()
                LoadResult(
                    ok = true,
                    backendName = "CPU",
                    loadMillis = elapsed
                )
            } catch (t: Throwable) {
                Log.e(tag, "CPU initialization failed", t)
                close()
                val elapsed = System.currentTimeMillis() - startTime
                LoadResult(
                    ok = false,
                    backendName = "None",
                    loadMillis = elapsed,
                    error = "${t.javaClass.simpleName}: ${t.message ?: "Unknown error"}"
                )
            }
        }

    @OptIn(ExperimentalApi::class)
    private fun tryLoadEngine(
        modelPath: String,
        backend: Backend,
        name: String,
        enableMtp: Boolean
    ) {
        if (enableMtp && backend is Backend.GPU) {
            try {
                ExperimentalFlags.enableSpeculativeDecoding = true
            } catch (t: Throwable) {
                Log.w(tag, "Failed to enable speculative decoding", t)
            }
        } else {
            try {
                ExperimentalFlags.enableSpeculativeDecoding = false
            } catch (t: Throwable) {
                // Ignore
            }
        }

        val cachePath = context.cacheDir.path
        val config = EngineConfig(
            modelPath = modelPath,
            backend = backend,
            cacheDir = cachePath
        )
        val eng = Engine(config)
        eng.initialize()
        engine = eng
    }

    fun generate(
        prompt: String,
        systemInstruction: String? = null,
        history: List<Pair<Boolean, String>> = emptyList(),
        maxOutputTokens: Int? = null
    ): Flow<String> = flow {
        generationMutex.withLock {
            val eng = engine ?: throw IllegalStateException("Model engine is not loaded")
            if (!eng.isInitialized()) {
                throw IllegalStateException("Model engine is not initialized")
            }

            scheduleIdleUnload()

            val initialMessages = history.map { (isUser, text) ->
                if (isUser) Message.user(text)
                else Message.model(text)
            }

            val config = ConversationConfig(
                systemInstruction = if (!systemInstruction.isNullOrBlank()) Contents.of(systemInstruction) else null,
                initialMessages = initialMessages,
                maxOutputToken = maxOutputTokens,
                samplerConfig = SamplerConfig(topK = 40, topP = 0.95, temperature = 0.7)
            )

            val conversation = eng.createConversation(config)
            try {
                conversation.sendMessageAsync(prompt).collect { msg ->
                    val chunk = msg.toString()
                    emit(chunk)
                }
            } finally {
                try {
                    conversation.close()
                } catch (t: Throwable) {
                    Log.w(tag, "Error closing conversation", t)
                }
                scheduleIdleUnload()
            }
        }
    }

    @Synchronized
    fun close() {
        idleJob?.cancel()
        idleJob = null
        val eng = engine
        engine = null
        activeBackendName = "None"
        if (eng != null) {
            try {
                eng.close()
            } catch (t: Throwable) {
                Log.w(tag, "Error closing engine", t)
            }
        }
    }
}
