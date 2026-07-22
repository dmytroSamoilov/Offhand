package com.dmytrosamoilov.offhand.core.ai.local

import android.content.Context
import com.dmytrosamoilov.offhand.core.ai.api.AiBackendException
import com.dmytrosamoilov.offhand.core.ai.api.SpeechModelState
import com.dmytrosamoilov.offhand.core.ai.api.SpeechToText
import com.dmytrosamoilov.offhand.core.ai.api.TranscriptionResult
import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import com.k2fsa.sherpa.onnx.OfflineWhisperModelConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@Singleton
class WhisperSpeechToText @Inject constructor(
    @ApplicationContext private val context: Context,
    private val downloader: ModelDownloader,
) : SpeechToText {

    private val mutex = Mutex()

    private val mutableDownloadState =
        MutableStateFlow<SpeechModelState>(SpeechModelState.NotDownloaded)
    override val downloadState: StateFlow<SpeechModelState> = mutableDownloadState.asStateFlow()

    @Volatile
    private var recognizer: OfflineRecognizer? = null

    init {
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            // compareAndSet so a download that started before the disk check
            // finishes never gets overwritten.
            mutableDownloadState.compareAndSet(SpeechModelState.NotDownloaded, diskState())
        }
    }

    override suspend fun prepare() {
        ensureRecognizer()
    }

    override suspend fun transcribe(audioWav: ByteArray): TranscriptionResult {
        val active = ensureRecognizer()
        return withContext(Dispatchers.Default) {
            val samples = WavPcm.toFloatSamples(audioWav)
            val started = System.currentTimeMillis()
            val stream = active.createStream()
            try {
                stream.acceptWaveform(samples, SAMPLE_RATE)
                active.decode(stream)
                val text = active.getResult(stream).text.trim()
                val elapsed = System.currentTimeMillis() - started
                GenAiLog.logModelOutput("whisper-output", text)
                GenAiLog.logModelOutput(
                    "whisper-done",
                    "samples=${samples.size} · ms=$elapsed · chars=${text.length}",
                )
                TranscriptionResult(text = text, processingTimeMs = elapsed)
            } finally {
                stream.release()
            }
        }
    }

    override fun release() {
        recognizer?.let { active ->
            recognizer = null
            active.release()
            GenAiLog.logModelOutput("whisper-released", WhisperModels.SMALL.id)
        }
    }

    private suspend fun ensureRecognizer(): OfflineRecognizer = mutex.withLock {
        recognizer ?: createRecognizer().also { recognizer = it }
    }

    private suspend fun createRecognizer(): OfflineRecognizer {
        val model = WhisperModels.SMALL
        val directory = File(context.filesDir, WhisperModels.DIRECTORY)
        downloadMissingFiles(model, directory)
        GenAiLog.logModelOutput("whisper-load", model.id)
        return withContext(Dispatchers.IO) {
            OfflineRecognizer(
                config = OfflineRecognizerConfig(
                    featConfig = FeatureConfig(sampleRate = SAMPLE_RATE, featureDim = FEATURE_DIM),
                    modelConfig = OfflineModelConfig(
                        whisper = OfflineWhisperModelConfig(
                            encoder = File(directory, model.encoder.fileName).absolutePath,
                            decoder = File(directory, model.decoder.fileName).absolutePath,
                            language = AUTO_DETECT_LANGUAGE,
                            task = "transcribe",
                        ),
                        tokens = File(directory, model.tokens.fileName).absolutePath,
                        modelType = "whisper",
                        numThreads = Runtime.getRuntime().availableProcessors().coerceAtMost(MAX_THREADS),
                    ),
                ),
            )
        }
    }

    private suspend fun downloadMissingFiles(model: WhisperModel, directory: File) {
        directory.mkdirs()
        val bytesTotal = model.files.sumOf { it.sizeInBytes }
        var bytesCompleted = model.files
            .filter { File(directory, it.fileName).length() == it.sizeInBytes }
            .sumOf { it.sizeInBytes }
        model.files.forEach { file ->
            val target = File(directory, file.fileName)
            if (target.length() == file.sizeInBytes) return@forEach
            GenAiLog.logModelOutput("whisper-download", file.fileName)
            downloader.download(model.downloadUrl(file), target).collect { progress ->
                when (progress) {
                    is DownloadProgress.InProgress -> {
                        mutableDownloadState.value = SpeechModelState.Downloading(
                            bytesDownloaded = bytesCompleted + progress.bytesDownloaded,
                            bytesTotal = bytesTotal,
                        )
                    }
                    is DownloadProgress.Completed -> bytesCompleted += file.sizeInBytes
                    is DownloadProgress.Failed -> {
                        mutableDownloadState.value = SpeechModelState.NotDownloaded
                        throw AiBackendException("Whisper download failed: ${progress.message}")
                    }
                }
            }
        }
        mutableDownloadState.value = SpeechModelState.Downloaded
    }

    private fun diskState(): SpeechModelState {
        val directory = File(context.filesDir, WhisperModels.DIRECTORY)
        val isComplete = WhisperModels.SMALL.files.all { file ->
            File(directory, file.fileName).length() == file.sizeInBytes
        }
        return if (isComplete) SpeechModelState.Downloaded else SpeechModelState.NotDownloaded
    }

    private companion object {
        const val SAMPLE_RATE = 16_000
        const val FEATURE_DIM = 80
        const val MAX_THREADS = 4
        const val AUTO_DETECT_LANGUAGE = ""
    }
}
