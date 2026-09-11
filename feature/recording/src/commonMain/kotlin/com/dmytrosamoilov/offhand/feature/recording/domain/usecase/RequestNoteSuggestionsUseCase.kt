package com.dmytrosamoilov.offhand.feature.recording.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.ProStatusRepository
import com.dmytrosamoilov.offhand.core.data.domain.RecordingProcessController
import com.dmytrosamoilov.offhand.feature.recording.domain.RecordingSessionManager
import kotlinx.coroutines.flow.first

class RequestNoteSuggestionsUseCase(
    private val entitlements: ProStatusRepository,
    private val recordingProcessController: RecordingProcessController,
    private val sessionManager: RecordingSessionManager,
) {
    suspend operator fun invoke(noteId: Long): Boolean {
        if (!entitlements.observeStatus().first().isPro) return false
        if (!recordingProcessController.suggestEvents(noteId)) sessionManager.suggestEvents(noteId)
        return true
    }
}
