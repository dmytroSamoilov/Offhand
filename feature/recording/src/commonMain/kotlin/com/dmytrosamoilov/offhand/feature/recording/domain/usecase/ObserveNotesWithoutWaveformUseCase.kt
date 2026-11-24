package com.dmytrosamoilov.offhand.feature.recording.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.Note
import com.dmytrosamoilov.offhand.core.data.domain.NoteStatus
import com.dmytrosamoilov.offhand.core.data.domain.NoteWaveformRepository
import com.dmytrosamoilov.offhand.core.data.domain.NotesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

// Notes whose recording is complete and has not been measured yet. A note is
// still being written while it records, and an import's placeholder is
// PROCESSING with no duration until its audio has been decoded.
class ObserveNotesWithoutWaveformUseCase(
    private val notesRepository: NotesRepository,
    private val waveformRepository: NoteWaveformRepository,
) {
    operator fun invoke(): Flow<List<Note>> = combine(
        notesRepository.observeNotes(),
        waveformRepository.observeWaveforms(),
    ) { notes, waveforms ->
        val measured = waveforms.mapTo(HashSet()) { it.noteId }
        notes.filter { note -> note.id !in measured && note.hasCompleteAudio() }
    }

    private fun Note.hasCompleteAudio(): Boolean = when {
        audioFileName == null -> false
        status == NoteStatus.RECORDING -> false
        status == NoteStatus.PROCESSING -> durationMs != null
        else -> true
    }
}
