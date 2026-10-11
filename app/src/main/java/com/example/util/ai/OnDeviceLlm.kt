package com.example.util.ai

import android.content.Context
import android.util.Log
import com.google.ai.edge.litertlm.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
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

    @Synchronized
    fun isLoaded(): Boolean = engine?.isInitialized() == true

    suspend fun load(modelPath: String, useGpu: Boolean, enableMtp: Boolean): LoadResult =
        withContext(Dispatchers.Default) {
            close()

            val startTime = System.currentTimeMillis()

            if (useGpu) {
                try {
                    val res = tryLoadEngine(modelPath, Backend.GPU(), "GPU", enableMtp)
                    val elapsed = System.currentTimeMillis() - startTime
                    activeBackendName = "GPU"
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

    fun generate(prompt: String, systemInstruction: String? = null): Flow<String> = flow {
        val eng = engine ?: throw IllegalStateException("Model engine is not loaded")
        if (!eng.isInitialized()) {
            throw IllegalStateException("Model engine is not initialized")
        }

        val config = if (!systemInstruction.isNullOrBlank()) {
            ConversationConfig(
                systemInstruction = Contents.of(systemInstruction),
                samplerConfig = SamplerConfig(topK = 40, topP = 0.95, temperature = 0.7)
            )
        } else {
            ConversationConfig(
                samplerConfig = SamplerConfig(topK = 40, topP = 0.95, temperature = 0.7)
            )
        }

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
        }
    }

    @Synchronized
    fun close() {
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
