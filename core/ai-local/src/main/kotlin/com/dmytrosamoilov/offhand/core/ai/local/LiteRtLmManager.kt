package com.dmytrosamoilov.offhand.core.ai.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.dmytrosamoilov.offhand.core.ai.api.AvailableModel
import com.dmytrosamoilov.offhand.core.ai.api.HardwareBackend
import com.dmytrosamoilov.offhand.core.ai.api.ModelManager
import com.dmytrosamoilov.offhand.core.ai.api.ModelState
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.MessageCallback
import com.google.ai.edge.litertlm.SamplerConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

private val Context.aiSettingsDataStore by preferencesDataStore(name = "ai_settings")

private val KEY_HARDWARE_BACKEND = stringPreferencesKey("hardware_backend")

private val KEY_MODEL_OVERRIDE = stringPreferencesKey("model_override")

@Singleton
class LiteRtLmManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val catalog: ModelCatalog,
    private val downloader: ModelDownloader,
) : ModelManager {

    private val mutableModelOverrideId = MutableStateFlow<String?>(null)
    override val modelOverrideId: StateFlow<String?> = mutableModelOverrideId.asStateFlow()

    override val model: AvailableModel
        get() = mutableModelOverrideId.value
            ?.let { overrideId -> availableModels.firstOrNull { it.id == overrideId } }
            ?: catalog.modelForDevice

    override val availableModels: List<AvailableModel>
        get() = catalog.all

    override val speechModelSizeInBytes: Long =
        WhisperModels.SMALL.files.sumOf { it.sizeInBytes }

    private val mutableModelState = MutableStateFlow<ModelState>(ModelState.NotDownloaded)
    override val modelState: StateFlow<ModelState> = mutableModelState.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val loadMutex = Mutex()

    init {
        scope.launch {
            loadMutex.withLock {
                mutableModelOverrideId.value =
                    context.aiSettingsDataStore.data.first()[KEY_MODEL_OVERRIDE]
                if (mutableModelState.value is ModelState.NotDownloaded) {
                    mutableModelState.value = idleDiskState()
                }
            }
        }
    }

    override val activeBackend: StateFlow<HardwareBackend> = context.aiSettingsDataStore.data
        .map { preferences ->
            preferences[KEY_HARDWARE_BACKEND]
                ?.let { stored -> runCatching { HardwareBackend.valueOf(stored) }.getOrNull() }
                ?: model.hardwareBackend
        }
        .stateIn(scope, SharingStarted.Eagerly, catalog.modelForDevice.hardwareBackend)

    @Volatile
    private var engine: Engine? = null

    override suspend fun setHardwareBackend(backend: HardwareBackend) {
        context.aiSettingsDataStore.edit { it[KEY_HARDWARE_BACKEND] = backend.name }
        loadMutex.withLock {
            if (engine != null) {
                runCatching { engine?.close() }
                engine = null
                mutableModelState.value = idleDiskState()
            }
        }
    }

    override suspend fun setModelOverride(modelId: String?) {
        context.aiSettingsDataStore.edit { preferences ->
            if (modelId == null) {
                preferences.remove(KEY_MODEL_OVERRIDE)
            } else {
                preferences[KEY_MODEL_OVERRIDE] = modelId
            }
        }
        loadMutex.withLock {
            mutableModelOverrideId.value = modelId
            runCatching { engine?.close() }
            engine = null
            mutableModelState.value = idleDiskState()
        }
    }

    override suspend fun deleteModel() = loadMutex.withLock {
        runCatching { engine?.close() }
        engine = null
        File(context.filesDir, model.modelFile).delete()
        mutableModelState.value = ModelState.NotDownloaded
    }

    override suspend fun ensureModelAvailable() = loadMutex.withLock {
        if (engine != null && mutableModelState.value is ModelState.Ready) return@withLock

        deleteStaleModelFiles()
        val file = File(context.filesDir, model.modelFile)
        if (!file.exists()) {
            downloadModel(file)
            if (mutableModelState.value is ModelState.Error) return@withLock
        }

        loadEngine(file)
    }

    // Only orphans (files no longer in the catalog) are removed — files of
    // other catalog models stay on disk so switching models is not a re-download.
    private fun deleteStaleModelFiles() {
        val knownFiles = catalog.all.map { it.modelFile }.toSet()
        context.filesDir.listFiles()
            ?.filter { it.name.endsWith(MODEL_FILE_EXTENSION) && it.name !in knownFiles }
            ?.forEach { orphan ->
                if (orphan.delete()) {
                    GenAiLog.logModelOutput("stale-model-deleted", orphan.name)
                }
            }
    }

    private suspend fun downloadModel(file: File) {
        downloader.download(model.downloadUrl, file).collect { progress ->
            mutableModelState.value = when (progress) {
                is DownloadProgress.InProgress -> ModelState.Downloading(
                    progress = progress.progress,
                    bytesDownloaded = progress.bytesDownloaded,
                    bytesTotal = progress.bytesTotal,
                )
                is DownloadProgress.Completed -> ModelState.Loading
                is DownloadProgress.Failed -> ModelState.Error(progress.message)
            }
        }
    }

    private suspend fun loadEngine(file: File) {
        mutableModelState.value = ModelState.Loading
        val backend = activeBackend.value
        GenAiLog.logModelOutput(
            "engine-load",
            "model=${model.id} backend=$backend maxTokens=${model.maxTokens}",
        )

        try {
            withContext(Dispatchers.IO) {
                val config = EngineConfig(
                    modelPath = file.absolutePath,
                    backend = resolveBackend(backend),
                    visionBackend = null,
                    // Whisper handles all STT — the LLM only ever sees text.
                    // Configuring an audio backend on a text-only bundle makes
                    // createConversation() throw AUDIO_ENCODER not found.
                    audioBackend = null,
                    maxNumTokens = model.maxTokens,
                    cacheDir = context.cacheDir.absolutePath,
                )
                engine = Engine(config).also { it.initialize() }
            }
            mutableModelState.value = ModelState.Ready
            GenAiLog.logModelOutput("engine-loaded", "backend=$backend")
        } catch (t: Throwable) {
            GenAiLog.error("engine-load-failed", t, "backend=$backend")
            runCatching { engine?.close() }
            engine = null
            mutableModelState.value = ModelState.Error(t.message ?: "Engine load failed")
        }
    }

    private fun resolveBackend(choice: HardwareBackend): Backend = when (choice) {
        HardwareBackend.CPU -> Backend.CPU(Runtime.getRuntime().availableProcessors())
        HardwareBackend.GPU -> Backend.GPU()
        HardwareBackend.NPU -> Backend.NPU(context.applicationInfo.nativeLibraryDir.orEmpty())
    }

    suspend fun generate(content: List<Content>): String {
        val current = engine ?: error("Model is not loaded. Call ensureModelAvailable() first.")
        return withContext(Dispatchers.Default) {
            runConversation(current, content)
        }
    }

    private suspend fun runConversation(engine: Engine, content: List<Content>): String {
        val conversation: Conversation = engine.createConversation(
            ConversationConfig(
                samplerConfig = SamplerConfig(
                    topK = model.topK,
                    topP = model.topP.toDouble(),
                    temperature = model.temperature.toDouble(),
                ),
                systemInstruction = null,
                tools = emptyList(),
            ),
        )
        return try {
            suspendCancellableCoroutine { continuation ->
                val buffer = StringBuilder()
                conversation.sendMessageAsync(
                    Contents.of(content),
                    object : MessageCallback {
                        override fun onMessage(message: Message) {
                            buffer.append(message.toString())
                            // Small models can degenerate into repetition loops
                            // that decode until the context window is full —
                            // bound the damage and return what we have.
                            if (buffer.length > MAX_RESPONSE_CHARS && continuation.isActive) {
                                GenAiLog.logModelOutput(
                                    "response-capped",
                                    "chars=${buffer.length} model=${model.id}",
                                )
                                runCatching { conversation.cancelProcess() }
                                continuation.resume(buffer.toString())
                            }
                        }

                        override fun onDone() {
                            if (continuation.isActive) continuation.resume(buffer.toString())
                        }

                        override fun onError(throwable: Throwable) {
                            if (continuation.isActive) continuation.resumeWithException(throwable)
                        }
                    },
                    emptyMap<String, String>(),
                )
                continuation.invokeOnCancellation { runCatching { conversation.cancelProcess() } }
            }
        } finally {
            runCatching { conversation.close() }
        }
    }

    override fun release() {
        scope.launch {
            loadMutex.withLock {
                runCatching { engine?.close() }
                engine = null
                mutableModelState.value = idleDiskState()
            }
        }
    }

    private fun idleDiskState(): ModelState =
        if (File(context.filesDir, model.modelFile).exists()) {
            ModelState.Downloaded
        } else {
            ModelState.NotDownloaded
        }

    private companion object {
        const val MODEL_FILE_EXTENSION = ".litertlm"
        const val MAX_RESPONSE_CHARS = 12_000
    }
}
