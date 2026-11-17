package com.dmytrosamoilov.offhand.core.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "note_waveforms")
internal data class NoteWaveformEntity(
    @PrimaryKey val noteId: Long,
    val levels: String,
)
