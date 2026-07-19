package com.dmytrosamoilov.offhand.core.ai.local

import android.content.Context
import com.dmytrosamoilov.offhand.core.ai.api.AiBackendException
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@Singleton
class WhisperSpeechToText @Inject constructor(
    @ApplicationContext private val context: Context,
    private val downloader: ModelDownloader,
) : SpeechToText {

    private val mutex = Mutex()

    @Volatile
    private var recognizer: OfflineRecognizer? = null

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
        model.files.forEach { file ->
            val target = File(directory, file.fileName)
            if (target.length() == file.sizeInBytes) return@forEach
            GenAiLog.logModelOutput("whisper-download", file.fileName)
            val outcome = downloader.download(model.downloadUrl(file), target).last()
            if (outcome is DownloadProgress.Failed) {
                throw AiBackendException("Whisper download failed: ${outcome.message}")
            }
        }
    }

    private companion object {
        const val SAMPLE_RATE = 16_000
        const val FEATURE_DIM = 80
        const val MAX_THREADS = 4
        const val AUTO_DETECT_LANGUAGE = ""
    }
}
