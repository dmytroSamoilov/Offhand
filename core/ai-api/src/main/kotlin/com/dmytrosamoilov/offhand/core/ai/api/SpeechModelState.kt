package com.dmytrosamoilov.offhand.core.ai.api

sealed interface SpeechModelState {
    data object NotDownloaded : SpeechModelState
    data class Downloading(
        val bytesDownloaded: Long,
        val bytesTotal: Long,
    ) : SpeechModelState

    data object Downloaded : SpeechModelState
}
