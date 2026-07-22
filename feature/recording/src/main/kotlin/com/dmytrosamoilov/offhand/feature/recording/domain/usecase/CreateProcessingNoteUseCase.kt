package com.dmytrosamoilov.offhand.feature.recording.domain.usecase

import android.content.Context
import com.dmytrosamoilov.offhand.core.data.domain.Note
import com.dmytrosamoilov.offhand.core.data.domain.NoteStatus
import com.dmytrosamoilov.offhand.core.data.domain.NotesRepository
import com.dmytrosamoilov.offhand.feature.recording.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class CreateProcessingNoteUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val notesRepository: NotesRepository,
) {
    suspend operator fun invoke(audioFileName: String?, durationMs: Long?): Long =
        notesRepository.createNote(
            Note(
                id = 0,
                title = context.getString(
                    R.string.recording_default_note_title,
                    notesRepository.countNotes() + 1,
                ),
                body = "",
                transcript = "",
                createdAtEpochMs = System.currentTimeMillis(),
                transcriptionTimeMs = null,
                structuringTimeMs = null,
                hardwareBackend = null,
                audioFileName = audioFileName,
                durationMs = durationMs,
                status = NoteStatus.PROCESSING,
            ),
        )
}
