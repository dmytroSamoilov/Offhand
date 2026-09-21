package com.dmytrosamoilov.offhand.core.data.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
internal interface TranscriptionCheckpointDao {

    @Query("SELECT * FROM transcription_checkpoints WHERE noteId = :noteId")
    suspend fun getByNoteId(noteId: Long): TranscriptionCheckpointEntity?

    @Upsert
    suspend fun upsert(checkpoint: TranscriptionCheckpointEntity)

    @Query("DELETE FROM transcription_checkpoints WHERE noteId = :noteId")
    suspend fun deleteByNoteId(noteId: Long)
}
