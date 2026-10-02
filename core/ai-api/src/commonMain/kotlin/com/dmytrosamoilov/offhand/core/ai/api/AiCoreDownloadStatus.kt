package com.dmytrosamoilov.offhand.core.ai.api

import com.dmytrosamoilov.offhand.core.common.ModelDownloadPolicy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

sealed interface AiCoreDownloadState {
    data object Idle : AiCoreDownloadState
    data class Downloading(val progressPercent: Int) : AiCoreDownloadState
    // Nothing is on disk and the policy holds the download back until Wi-Fi or
    // the user's say-so.
    data class WaitingForMobileData(val bytesTotal: Long) : AiCoreDownloadState
}

class AiCoreDownloadStatus(
    private val modelManager: ModelManager,
    speechToText: SpeechToText,
    downloadPolicy: ModelDownloadPolicy,
) {

    val state: Flow<AiCoreDownloadState> =
        combine(modelManager.modelState, speechToText.downloadState, downloadPolicy.canStart) { modelState, speechState, canStart ->
            toDownloadState(modelState, speechState, canStart)
        }.distinctUntilChanged()

    private fun toDownloadState(
        modelState: ModelState,
        speechState: SpeechModelState,
        canStart: Boolean,
    ): AiCoreDownloadState {
        val isDownloading =
            modelState is ModelState.Downloading || speechState is SpeechModelState.Downloading
        val modelBytesTotal = modelManager.model.sizeInBytes
        val speechBytesTotal = modelManager.speechModelSizeInBytes
        val bytesTotal = modelBytesTotal + speechBytesTotal
        if (!isDownloading) {
            val isMissing = modelState is ModelState.NotDownloaded || speechState is SpeechModelState.NotDownloaded
            return if (isMissing && !canStart) AiCoreDownloadState.WaitingForMobileData(bytesTotal) else AiCoreDownloadState.Idle
        }
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
