package com.dmytrosamoilov.offhand.core.data.database

import com.dmytrosamoilov.offhand.core.data.domain.Note
import com.dmytrosamoilov.offhand.core.data.domain.NoteStatus

internal fun NoteEntity.toDomain(): Note = Note(
    id = id,
    title = title,
    body = body,
    transcript = transcript,
    createdAtEpochMs = createdAtEpochMs,
    transcriptionTimeMs = transcriptionTimeMs,
    structuringTimeMs = structuringTimeMs,
    hardwareBackend = hardwareBackend,
    audioFileName = audioFileName,
    durationMs = durationMs,
    status = NoteStatus.entries.firstOrNull { it.name == status } ?: NoteStatus.READY,
)

internal fun Note.toEntity(): NoteEntity = NoteEntity(
    id = id,
    title = title,
    body = body,
    transcript = transcript,
    createdAtEpochMs = createdAtEpochMs,
    transcriptionTimeMs = transcriptionTimeMs,
    structuringTimeMs = structuringTimeMs,
    hardwareBackend = hardwareBackend,
    audioFileName = audioFileName,
    durationMs = durationMs,
    status = status.name,
)
