package com.dmytrosamoilov.offhand.feature.recording.domain.usecase

import com.dmytrosamoilov.offhand.core.ai.api.ModelManager
import com.dmytrosamoilov.offhand.core.ai.api.SpeechModelState
import com.dmytrosamoilov.offhand.core.ai.api.SpeechToText
import com.dmytrosamoilov.offhand.core.device.DeviceTierProvider

class IsAiCoreDownloadedUseCase(
    private val modelManager: ModelManager,
    private val speechToText: SpeechToText,
    private val deviceTier: DeviceTierProvider,
) {
    suspend operator fun invoke(): Boolean =
        speechToText.downloadState.value is SpeechModelState.Downloaded &&
            (!deviceTier.writesSummaries() || modelManager.isModelDownloaded())
}
