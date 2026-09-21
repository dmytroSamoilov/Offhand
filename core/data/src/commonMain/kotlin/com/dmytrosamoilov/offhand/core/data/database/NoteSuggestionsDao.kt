package com.dmytrosamoilov.offhand.core.data.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
internal interface NoteSuggestionsDao {

    @Query("SELECT * FROM note_suggestions WHERE noteId = :noteId")
    fun observeByNoteId(noteId: Long): Flow<NoteSuggestionsEntity?>

    @Query("SELECT * FROM note_suggestions WHERE noteId = :noteId")
    suspend fun getByNoteId(noteId: Long): NoteSuggestionsEntity?

    @Upsert
    suspend fun upsert(suggestions: NoteSuggestionsEntity)

    @Query("DELETE FROM note_suggestions WHERE noteId = :noteId")
    suspend fun deleteByNoteId(noteId: Long)
}
