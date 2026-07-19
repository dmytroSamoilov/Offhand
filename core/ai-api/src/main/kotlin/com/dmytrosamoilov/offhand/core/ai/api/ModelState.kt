package com.dmytrosamoilov.offhand.core.ai.api

sealed interface ModelState {
    data object NotDownloaded : ModelState
    data object Downloaded : ModelState
    data class Downloading(
        val progress: Float,
        val bytesDownloaded: Long,
        val bytesTotal: Long,
    ) : ModelState

    data object Loading : ModelState
    data object Ready : ModelState
    data class Error(val message: String) : ModelState
}
