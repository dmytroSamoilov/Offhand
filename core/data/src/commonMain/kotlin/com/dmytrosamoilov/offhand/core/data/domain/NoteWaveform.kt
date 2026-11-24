package com.dmytrosamoilov.offhand.core.data.domain

import kotlinx.coroutines.flow.Flow

// The loudness of a note's recording in equal sections, 0..100 each, measured
// once from the stored audio. Kept apart from the note so a pipeline write of
// an older copy of the note can never drop it.
data class NoteWaveform(
    val noteId: Long,
    val levels: List<Int>,
)

interface NoteWaveformRepository {

    fun observeWaveforms(): Flow<List<NoteWaveform>>

    suspend fun saveWaveform(waveform: NoteWaveform)
}
