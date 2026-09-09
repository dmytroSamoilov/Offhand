package com.dmytrosamoilov.offhand.core.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "folders")
internal data class FolderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAtEpochMs: Long,
)
