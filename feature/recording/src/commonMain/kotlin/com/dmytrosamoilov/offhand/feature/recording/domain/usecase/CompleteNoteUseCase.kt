package com.dmytrosamoilov.offhand.feature.recording.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.NotePreset
import com.dmytrosamoilov.offhand.core.data.domain.NoteStatus
import com.dmytrosamoilov.offhand.core.data.domain.NotesRepository

class CompleteNoteUseCase(
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
        preset: NotePreset,
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
                preset = preset,
            ),
        )
        return true
    }
}
