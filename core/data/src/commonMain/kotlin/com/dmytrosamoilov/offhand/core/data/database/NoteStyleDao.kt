package com.dmytrosamoilov.offhand.core.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
internal interface NoteStyleDao {

    @Query("SELECT * FROM note_styles ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<NoteStyleEntity>>

    @Query("SELECT * FROM note_styles WHERE id = :id")
    suspend fun getById(id: Long): NoteStyleEntity?

    @Insert
    suspend fun insert(style: NoteStyleEntity): Long

    @Update
    suspend fun update(style: NoteStyleEntity)

    @Query("DELETE FROM note_styles WHERE id = :id")
    suspend fun deleteById(id: Long)
}
