package com.dmytrosamoilov.offhand.core.ai.api

import com.dmytrosamoilov.offhand.core.common.ModelDownloadPolicy
import com.dmytrosamoilov.offhand.core.device.DeviceTierProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

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
    private val deviceTier: DeviceTierProvider,
) {

    val state: Flow<AiCoreDownloadState> =
        combine(modelManager.modelState, speechToText.downloadState, downloadPolicy.canStart) { modelState, speechState, canStart ->
            toDownloadState(modelState, speechState, canStart)
        }.distinctUntilChanged()

    // True while a part of the on-device AI is not on the phone yet, whether a
    // download is running, waiting or has not started: notes cannot be worked
    // on until it is false.
    val isMissing: Flow<Boolean> =
        combine(modelManager.modelState, speechToText.downloadState) { modelState, speechState ->
            (deviceTier.writesSummaries() && modelState.isNotOnDisk()) || speechState !is SpeechModelState.Downloaded
        }.distinctUntilChanged()

    // The speech part is the small one and is downloaded first: once it is on
    // the phone recordings are transcribed, whatever the rest is doing.
    val isSpeechMissing: Flow<Boolean> =
        speechToText.downloadState.map { speechState -> speechState !is SpeechModelState.Downloaded }
            .distinctUntilChanged()

    private fun ModelState.isNotOnDisk(): Boolean =
        this is ModelState.NotDownloaded || this is ModelState.Downloading

    private fun toDownloadState(
        modelState: ModelState,
        speechState: SpeechModelState,
        canStart: Boolean,
    ): AiCoreDownloadState {
        val includesModel = deviceTier.writesSummaries()
        val isDownloading =
            (includesModel && modelState is ModelState.Downloading) || speechState is SpeechModelState.Downloading
        val modelBytesTotal = if (includesModel) modelManager.model.sizeInBytes else 0L
        val speechBytesTotal = modelManager.speechModelSizeInBytes
        val bytesTotal = modelBytesTotal + speechBytesTotal
        if (!isDownloading) {
            val isMissing = (includesModel && modelState is ModelState.NotDownloaded) ||
                speechState is SpeechModelState.NotDownloaded
            return if (isMissing && !canStart) AiCoreDownloadState.WaitingForMobileData(bytesTotal) else AiCoreDownloadState.Idle
        }
        if (bytesTotal <= 0) return AiCoreDownloadState.Downloading(progressPercent = 0)

        val bytesDownloaded = (if (includesModel) modelState.downloadedBytes(modelBytesTotal) else 0L) +
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
