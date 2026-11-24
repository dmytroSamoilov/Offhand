package com.dmytrosamoilov.offhand.feature.recording.domain.usecase

import com.dmytrosamoilov.offhand.core.ai.api.SpeechModelState
import com.dmytrosamoilov.offhand.core.ai.api.SpeechToText

class IsSpeechModelDownloadedUseCase(
    private val speechToText: SpeechToText,
) {
    operator fun invoke(): Boolean = speechToText.downloadState.value is SpeechModelState.Downloaded
}
