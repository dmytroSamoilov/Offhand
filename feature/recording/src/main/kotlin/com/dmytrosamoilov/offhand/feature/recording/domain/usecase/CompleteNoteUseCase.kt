package com.dmytrosamoilov.offhand.feature.recording.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.NoteStatus
import com.dmytrosamoilov.offhand.core.data.domain.NotesRepository
import javax.inject.Inject

class CompleteNoteUseCase @Inject constructor(
    private val notesRepository: NotesRepository,
) {
    suspend operator fun invoke(
        noteId: Long,
        title: String,
        body: String,
        transcript: String,
        transcriptionTimeMs: Long,
        structuringTimeMs: Long,
        hardwareBackend: String,
    ): Boolean {
        val note = notesRepository.getNote(noteId) ?: return false
        notesRepository.updateNote(
            note.copy(
                title = title,
                body = body,
                transcript = transcript,
                transcriptionTimeMs = transcriptionTimeMs,
                structuringTimeMs = structuringTimeMs,
                hardwareBackend = hardwareBackend,
                status = NoteStatus.READY,
            ),
        )
        return true
    }
}
