package com.dmytrosamoilov.offhand.core.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
internal interface FolderDao {

    @Query("SELECT * FROM folders ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<FolderEntity>>

    @Insert
    suspend fun insert(folder: FolderEntity): Long

    @Query("UPDATE folders SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    @Query("DELETE FROM folders WHERE id = :id")
    suspend fun deleteById(id: Long)
}
