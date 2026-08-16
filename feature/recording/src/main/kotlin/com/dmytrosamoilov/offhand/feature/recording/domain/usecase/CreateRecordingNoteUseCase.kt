package com.dmytrosamoilov.offhand.feature.recording.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.Note
import com.dmytrosamoilov.offhand.core.data.domain.NotePreset
import com.dmytrosamoilov.offhand.core.data.domain.NoteStatus
import com.dmytrosamoilov.offhand.core.data.domain.NotesRepository
import com.dmytrosamoilov.offhand.feature.recording.domain.DefaultNoteTitleProvider

class CreateRecordingNoteUseCase(
    private val defaultNoteTitleProvider: DefaultNoteTitleProvider,
    private val notesRepository: NotesRepository,
) {
    suspend operator fun invoke(
        audioFileName: String?,
        preset: NotePreset,
    ): Long =
        notesRepository.createNote(
            Note(
                id = 0,
                title = defaultNoteTitleProvider.titleFor(notesRepository.countNotes() + 1),
                body = "",
                transcript = "",
                createdAtEpochMs = System.currentTimeMillis(),
                transcriptionTimeMs = null,
                structuringTimeMs = null,
                hardwareBackend = null,
                audioFileName = audioFileName,
                durationMs = null,
                status = NoteStatus.RECORDING,
                preset = preset,
            ),
        )
}
