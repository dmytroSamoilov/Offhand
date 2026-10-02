package com.dmytrosamoilov.offhand.core.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
internal interface FolderDao {

    @Query("SELECT * FROM folders ORDER BY position ASC, name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<FolderEntity>>

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM folders")
    suspend fun nextPosition(): Int

    @Insert
    suspend fun insert(folder: FolderEntity): Long

    @Query("UPDATE folders SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    @Query("UPDATE folders SET styleKey = :styleKey WHERE id = :id")
    suspend fun setStyle(id: Long, styleKey: String?)

    @Query("UPDATE folders SET styleKey = NULL WHERE styleKey = :styleKey")
    suspend fun clearStyle(styleKey: String)

    @Query("UPDATE folders SET position = :position WHERE id = :id")
    suspend fun setPosition(id: Long, position: Int)

    @Query("DELETE FROM folders WHERE id = :id")
    suspend fun deleteById(id: Long)
}
