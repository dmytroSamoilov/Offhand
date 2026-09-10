package com.dmytrosamoilov.offhand.core.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "note_suggestions")
internal data class NoteSuggestionsEntity(
    @PrimaryKey val noteId: Long,
    val eventsJson: String,
)
