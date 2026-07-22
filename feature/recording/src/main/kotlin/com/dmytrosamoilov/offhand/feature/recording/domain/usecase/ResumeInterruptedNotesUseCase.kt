package com.dmytrosamoilov.offhand.feature.recording.domain.usecase

import android.app.ForegroundServiceStartNotAllowedException
import android.content.Context
import com.dmytrosamoilov.offhand.core.data.domain.NoteStatus
import com.dmytrosamoilov.offhand.core.data.domain.NotesRepository
import com.dmytrosamoilov.offhand.feature.recording.domain.RecordingSessionManager
import com.dmytrosamoilov.offhand.feature.recording.service.RecordingService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import timber.log.Timber

@Singleton
class ResumeInterruptedNotesUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val notesRepository: NotesRepository,
    private val sessionManager: RecordingSessionManager,
    private val failNote: FailNoteUseCase,
    private val isAiCoreDownloaded: IsAiCoreDownloadedUseCase,
) {

    suspend operator fun invoke() {
        if (!isAiCoreDownloaded()) return
        val activeNoteIds = sessionManager.processingNoteIds.value
        notesRepository.observeNotes().first()
            .filter { it.status == NoteStatus.PROCESSING && it.id !in activeNoteIds }
            .sortedBy { it.createdAtEpochMs }
            .forEach { note ->
                val audioFileName = note.audioFileName
                if (audioFileName == null) {
                    failNote(note.id)
                } else {
                    retryViaService(note.id, audioFileName)
                }
            }
    }

    private fun retryViaService(noteId: Long, audioFileName: String) {
        try {
            RecordingService.retryNote(context, noteId, audioFileName)
        } catch (notAllowed: ForegroundServiceStartNotAllowedException) {
            Timber.tag(LOG_TAG).w(notAllowed, "FGS not allowed, processing note %d in-process", noteId)
            sessionManager.retryNote(noteId, audioFileName)
        }
    }

    private companion object {
        const val LOG_TAG = "RecordingSession"
    }
}
