package com.dmytrosamoilov.offhand.core.ai.api

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

sealed interface AiCoreDownloadState {
    data object Idle : AiCoreDownloadState
    data class Downloading(val progressPercent: Int) : AiCoreDownloadState
}

@Singleton
class AiCoreDownloadStatus @Inject constructor(
    private val modelManager: ModelManager,
    speechToText: SpeechToText,
) {

    val state: Flow<AiCoreDownloadState> =
        combine(modelManager.modelState, speechToText.downloadState) { modelState, speechState ->
            toDownloadState(modelState, speechState)
        }.distinctUntilChanged()

    private fun toDownloadState(
        modelState: ModelState,
        speechState: SpeechModelState,
    ): AiCoreDownloadState {
        val isDownloading =
            modelState is ModelState.Downloading || speechState is SpeechModelState.Downloading
        if (!isDownloading) return AiCoreDownloadState.Idle

        val modelBytesTotal = modelManager.model.sizeInBytes
        val speechBytesTotal = modelManager.speechModelSizeInBytes
        val bytesTotal = modelBytesTotal + speechBytesTotal
        if (bytesTotal <= 0) return AiCoreDownloadState.Downloading(progressPercent = 0)

        val bytesDownloaded = modelState.downloadedBytes(modelBytesTotal) +
            speechState.downloadedBytes(speechBytesTotal)
        return AiCoreDownloadState.Downloading(
            progressPercent = (bytesDownloaded * 100 / bytesTotal).toInt().coerceIn(0, 100),
        )
    }

    private fun ModelState.downloadedBytes(bytesTotal: Long): Long = when (this) {
        is ModelState.Downloading -> bytesDownloaded
        is ModelState.Downloaded, is ModelState.Loading, is ModelState.Ready -> bytesTotal
        is ModelState.NotDownloaded, is ModelState.Error -> 0
    }

    private fun SpeechModelState.downloadedBytes(bytesTotal: Long): Long = when (this) {
        is SpeechModelState.Downloading -> bytesDownloaded
        is SpeechModelState.Downloaded -> bytesTotal
        is SpeechModelState.NotDownloaded -> 0
    }
}
