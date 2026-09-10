package com.dmytrosamoilov.offhand.feature.recording.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.EntitlementsRepository
import com.dmytrosamoilov.offhand.core.data.domain.RecordingProcessController
import com.dmytrosamoilov.offhand.feature.recording.domain.RecordingSessionManager
import kotlinx.coroutines.flow.first

class RequestNoteSuggestionsUseCase(
    private val entitlements: EntitlementsRepository,
    private val recordingProcessController: RecordingProcessController,
    private val sessionManager: RecordingSessionManager,
) {
    suspend operator fun invoke(noteId: Long): Boolean {
        if (!entitlements.observeEntitlements().first().calendarSuggestionsUnlocked) return false
        if (!recordingProcessController.suggestEvents(noteId)) sessionManager.suggestEvents(noteId)
        return true
    }
}
