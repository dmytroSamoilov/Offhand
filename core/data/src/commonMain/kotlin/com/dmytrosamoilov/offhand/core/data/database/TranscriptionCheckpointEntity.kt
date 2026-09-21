package com.dmytrosamoilov.offhand.core.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transcription_checkpoints")
internal data class TranscriptionCheckpointEntity(
    @PrimaryKey val noteId: Long,
    val transcribedBytes: Long,
    val transcriptionTimeMs: Long,
)
