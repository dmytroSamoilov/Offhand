package com.dmytrosamoilov.offhand.feature.recording.domain

import com.dmytrosamoilov.offhand.core.ai.api.AiBackend
import com.dmytrosamoilov.offhand.core.data.domain.NoteStyleDraft
import com.dmytrosamoilov.offhand.core.data.domain.NoteStyleDraftException
import com.dmytrosamoilov.offhand.core.data.domain.NoteStyleDrafter
import com.dmytrosamoilov.offhand.core.data.domain.NoteStyleLimits
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.IsAiCoreDownloadedUseCase

class SessionNoteStyleDrafter(
    private val sessionManager: RecordingSessionManager,
    private val aiBackend: AiBackend,
    private val isAiCoreDownloaded: IsAiCoreDownloadedUseCase,
) : NoteStyleDrafter {

    override suspend fun draft(description: String): NoteStyleDraft? {
        if (!isAiCoreDownloaded()) return null
        val cleaned = description.replace('"', '\'').trim().take(NoteStyleLimits.MAX_DESCRIPTION_LENGTH)
        return sessionManager.withProcessingLock {
            NoteStyleDraftParser.parse(aiBackend.processText(NoteStyleDraftPrompt.build(), cleaned).text)
                ?: throw NoteStyleDraftException()
        }
    }
}
