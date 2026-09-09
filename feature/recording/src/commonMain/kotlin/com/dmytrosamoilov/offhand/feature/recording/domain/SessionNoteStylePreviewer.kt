package com.dmytrosamoilov.offhand.feature.recording.domain

import com.dmytrosamoilov.offhand.core.data.domain.CustomNoteStyle
import com.dmytrosamoilov.offhand.core.data.domain.NoteStylePreview
import com.dmytrosamoilov.offhand.core.data.domain.NoteStylePreviewer
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.IsAiCoreDownloadedUseCase

class SessionNoteStylePreviewer(
    private val sessionManager: RecordingSessionManager,
    private val isAiCoreDownloaded: IsAiCoreDownloadedUseCase,
) : NoteStylePreviewer {

    override suspend fun preview(style: CustomNoteStyle, sampleTranscript: String): NoteStylePreview? {
        if (!isAiCoreDownloaded()) return null
        return sessionManager.previewNoteStyle(CustomNoteStyleSpecBuilder.build(style), sampleTranscript)
    }
}
