package com.dmytrosamoilov.offhand.feature.recording.domain.usecase

import com.dmytrosamoilov.offhand.core.ai.api.ModelManager
import com.dmytrosamoilov.offhand.core.ai.api.SpeechModelState
import com.dmytrosamoilov.offhand.core.ai.api.SpeechToText
import javax.inject.Inject

class IsAiCoreDownloadedUseCase @Inject constructor(
    private val modelManager: ModelManager,
    private val speechToText: SpeechToText,
) {
    suspend operator fun invoke(): Boolean =
        speechToText.downloadState.value is SpeechModelState.Downloaded &&
            modelManager.isModelDownloaded()
}
