package com.dmytrosamoilov.offhand.feature.recording.domain.usecase

import com.dmytrosamoilov.offhand.core.ai.api.ModelManager
import com.dmytrosamoilov.offhand.core.ai.api.SpeechModelState
import com.dmytrosamoilov.offhand.core.ai.api.SpeechToText

class IsAiCoreDownloadedUseCase(
    private val modelManager: ModelManager,
    private val speechToText: SpeechToText,
) {
    suspend operator fun invoke(): Boolean =
        speechToText.downloadState.value is SpeechModelState.Downloaded &&
            modelManager.isModelDownloaded()
}
