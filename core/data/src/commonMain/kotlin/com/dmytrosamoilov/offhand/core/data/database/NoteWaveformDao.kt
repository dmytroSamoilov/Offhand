package com.dmytrosamoilov.offhand.core.data.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
internal interface NoteWaveformDao {

    @Query("SELECT * FROM note_waveforms")
    fun observeAll(): Flow<List<NoteWaveformEntity>>

    @Upsert
    suspend fun upsert(waveform: NoteWaveformEntity)

    @Query("DELETE FROM note_waveforms WHERE noteId = :noteId")
    suspend fun deleteByNoteId(noteId: Long)
}
