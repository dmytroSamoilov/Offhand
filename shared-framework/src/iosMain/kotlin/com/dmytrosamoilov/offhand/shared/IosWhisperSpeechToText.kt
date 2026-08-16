@file:OptIn(ExperimentalForeignApi::class)

package com.dmytrosamoilov.offhand.shared

import com.dmytrosamoilov.offhand.core.ai.api.SpeechModelState
import com.dmytrosamoilov.offhand.core.ai.api.SpeechToText
import com.dmytrosamoilov.offhand.core.ai.api.TranscriptionResult
import kotlin.time.TimeSource
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import platform.Foundation.NSFileManager

class IosWhisperSpeechToText(
    private val whisperEngine: IosWhisperEngine,
    private val modelManager: IosModelManager,
    private val downloader: IosFileDownloader,
) : SpeechToText {

    private val mutableDownloadState = MutableStateFlow(idleDiskState())
    override val downloadState: StateFlow<SpeechModelState> = mutableDownloadState.asStateFlow()

    private val prepareMutex = Mutex()
    private var prepared = false

    override suspend fun prepare() {
        prepareMutex.withLock {
            if (prepared) return
            downloadMissingFiles()
            whisperEngine.prepare(
                encoderPath = pathFor(whisperFiles[0]),
                decoderPath = pathFor(whisperFiles[1]),
                tokensPath = pathFor(whisperFiles[2]),
            )
            prepared = true
        }
    }

    override suspend fun transcribe(audioWav: ByteArray): TranscriptionResult {
        prepare()
        val start = TimeSource.Monotonic.markNow()
        val text = whisperEngine.transcribe(audioWav)
        return TranscriptionResult(
            text = text.trim(),
            processingTimeMs = start.elapsedNow().inWholeMilliseconds,
        )
    }

    override fun release() {
        if (!prepared) return
        whisperEngine.releaseEngine()
        prepared = false
    }

    private suspend fun downloadMissingFiles() {
        val missing = whisperFiles.filterNot { isComplete(it) }
        if (missing.isEmpty()) {
            mutableDownloadState.value = SpeechModelState.Downloaded
            return
        }
        val totalBytes = whisperFiles.sumOf { it.sizeInBytes }
        var completedBytes = whisperFiles.filter(::isComplete).sumOf { it.sizeInBytes }
        missing.forEach { file ->
            val downloaded = downloader.download(file.downloadUrl, pathFor(file)) { done, _ ->
                mutableDownloadState.value =
                    SpeechModelState.Downloading(completedBytes + done, totalBytes)
            }
            if (!downloaded || !isComplete(file)) {
                mutableDownloadState.value = idleDiskState()
                throw IosFileDownloader.DownloadFailedException(file.fileName)
            }
            completedBytes += file.sizeInBytes
        }
        mutableDownloadState.value = SpeechModelState.Downloaded
    }

    private fun idleDiskState(): SpeechModelState =
        if (whisperFiles.all(::isComplete)) SpeechModelState.Downloaded else SpeechModelState.NotDownloaded

    private fun isComplete(file: WhisperFile): Boolean {
        val attributes = NSFileManager.defaultManager
            .attributesOfItemAtPath(pathFor(file), error = null) ?: return false
        return (attributes[FILE_SIZE_KEY] as? Long) == file.sizeInBytes
    }

    private fun pathFor(file: WhisperFile): String =
        "${modelManager.modelsDirectory()}/${file.fileName}"

    private companion object {
        const val FILE_SIZE_KEY = "NSFileSize"
    }
}
