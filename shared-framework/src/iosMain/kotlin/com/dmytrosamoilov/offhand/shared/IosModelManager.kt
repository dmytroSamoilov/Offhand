@file:OptIn(ExperimentalForeignApi::class)

package com.dmytrosamoilov.offhand.shared

import com.dmytrosamoilov.offhand.core.data.domain.AppForegroundState
import co.touchlab.kermit.Logger
import com.dmytrosamoilov.offhand.core.ai.api.AvailableModel
import com.dmytrosamoilov.offhand.core.ai.api.HardwareBackend
import com.dmytrosamoilov.offhand.core.ai.api.ModelManager
import com.dmytrosamoilov.offhand.core.ai.api.ModelState
import com.dmytrosamoilov.offhand.core.security.excludeFromBackup
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

class IosModelManager(
    private val gemmaEngine: IosGemmaEngine,
    private val downloader: IosFileDownloader,
    private val appForegroundState: AppForegroundState,
) : ModelManager {

    override val model: AvailableModel = gemmaE2bModel

    override val availableModels: List<AvailableModel> = listOf(gemmaE2bModel)

    override val speechModelSizeInBytes: Long = whisperFiles.sumOf { it.sizeInBytes }

    override val modelOverrideId: StateFlow<String?> = MutableStateFlow(null).asStateFlow()

    private val mutableModelState = MutableStateFlow<ModelState>(idleDiskState())
    override val modelState: StateFlow<ModelState> = mutableModelState.asStateFlow()

    private val mutableActiveBackend = MutableStateFlow(HardwareBackend.CPU)
    override val activeBackend: StateFlow<HardwareBackend> = mutableActiveBackend.asStateFlow()

    private val engineMutex = Mutex()
    private var engineLoaded = false
    private var engineUsesGpu = false

    // What the loaded engine really runs on, which is the CPU whenever the
    // app is away, whatever backend is selected.
    internal val engineBackend: HardwareBackend
        get() = if (engineUsesGpu) HardwareBackend.GPU else HardwareBackend.CPU

    override suspend fun isModelDownloaded(): Boolean = modelFileSize() == model.sizeInBytes

    override suspend fun ensureModelAvailable() {
        engineMutex.withLock {
            if (engineLoaded) return
            if (!isModelDownloaded()) downloadModel()
            loadEngine()
        }
    }

    override suspend fun setHardwareBackend(backend: HardwareBackend) {
        if (backend == HardwareBackend.NPU) return
        engineMutex.withLock {
            mutableActiveBackend.value = backend
            if (engineLoaded) {
                gemmaEngine.unload()
                engineLoaded = false
                mutableModelState.value = idleDiskState()
            }
        }
    }

    override suspend fun setModelOverride(modelId: String?) = Unit

    override suspend fun deleteModel() {
        engineMutex.withLock {
            if (engineLoaded) {
                gemmaEngine.unload()
                engineLoaded = false
            }
            NSFileManager.defaultManager.removeItemAtPath(modelPath(), error = null)
            mutableModelState.value = ModelState.NotDownloaded
        }
    }

    override fun release() {
        if (!engineLoaded) return
        gemmaEngine.unload()
        engineLoaded = false
        mutableModelState.value = idleDiskState()
    }

    // iOS refuses GPU work to an app that is not in front, on every iPhone, so
    // an engine loaded for the GPU is swapped for a CPU one before it is used
    // in the background, and back again once the app is in front.
    internal suspend fun awaitReadyEngine() {
        engineMutex.withLock {
            if (engineLoaded && engineUsesGpu != shouldUseGpu()) unloadEngine()
        }
        ensureModelAvailable()
    }

    // A generation that was cut off by the app leaving the foreground is
    // worth one more try on the CPU; anything else is a real failure.
    internal suspend fun reloadForBackground(): Boolean = engineMutex.withLock {
        if (!engineLoaded || !engineUsesGpu || shouldUseGpu()) return@withLock false
        unloadEngine()
        true
    }

    private fun shouldUseGpu(): Boolean =
        mutableActiveBackend.value == HardwareBackend.GPU && appForegroundState.isInForeground.value

    private fun unloadEngine() {
        gemmaEngine.unload()
        engineLoaded = false
        mutableModelState.value = idleDiskState()
    }

    private suspend fun downloadModel() {
        mutableModelState.value = ModelState.Downloading(0f, 0, model.sizeInBytes)
        val downloaded = downloader.download(model.downloadUrl, modelPath()) { done, total ->
            val safeTotal = if (total > 0) total else model.sizeInBytes
            mutableModelState.value = ModelState.Downloading(
                progress = done.toFloat() / safeTotal,
                bytesDownloaded = done,
                bytesTotal = safeTotal,
            )
        }
        if (!downloaded || !isModelDownloaded()) {
            mutableModelState.value = ModelState.Error("Model download failed")
            throw IosFileDownloader.DownloadFailedException(model.modelFile)
        }
        mutableModelState.value = ModelState.Downloaded
    }

    private suspend fun loadEngine() {
        mutableModelState.value = ModelState.Loading
        val useGpu = shouldUseGpu()
        try {
            gemmaEngine.load(
                modelPath = modelPath(),
                useGpu = useGpu,
                maxTokens = model.maxTokens,
                temperature = model.temperature,
                topK = model.topK,
                topP = model.topP,
            )
            engineLoaded = true
            engineUsesGpu = useGpu
            mutableModelState.value = ModelState.Ready
        } catch (t: Throwable) {
            Logger.withTag(LOG_TAG).e(t) { "Engine load failed" }
            mutableModelState.value = ModelState.Error(t.message ?: "Engine load failed")
            throw t
        }
    }

    private fun idleDiskState(): ModelState =
        if (modelFileSize() == model.sizeInBytes) ModelState.Downloaded else ModelState.NotDownloaded

    private fun modelFileSize(): Long {
        val attributes = NSFileManager.defaultManager.attributesOfItemAtPath(modelPath(), error = null)
            ?: return 0L
        return (attributes[FILE_SIZE_KEY] as? Long) ?: 0L
    }

    private fun modelPath(): String = "${modelsDirectory()}/${model.modelFile}"

    internal fun modelsDirectory(): String {
        val documents =
            NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true)
                .first() as String
        val directory = "$documents/models"
        if (!NSFileManager.defaultManager.fileExistsAtPath(directory)) {
            NSFileManager.defaultManager.createDirectoryAtPath(
                directory,
                withIntermediateDirectories = true,
                attributes = null,
                error = null,
            )
        }
        excludeFromBackup(directory)
        return directory
    }

    private companion object {
        const val LOG_TAG = "IosModelManager"
        const val FILE_SIZE_KEY = "NSFileSize"
    }
}
