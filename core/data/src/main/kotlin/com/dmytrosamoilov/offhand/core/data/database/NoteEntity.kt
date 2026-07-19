package com.dmytrosamoilov.offhand.core.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
internal data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val body: String,
    val transcript: String,
    val createdAtEpochMs: Long,
    val transcriptionTimeMs: Long?,
    val structuringTimeMs: Long?,
    val hardwareBackend: String?,
    val audioFileName: String?,
    val durationMs: Long?,
    val status: String,
)
