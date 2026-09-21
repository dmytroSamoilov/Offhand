package com.dmytrosamoilov.offhand.core.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "note_styles")
internal data class NoteStyleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val noteKind: String,
    val language: String,
    val sectionsJson: String,
    val createdAtEpochMs: Long,
)
