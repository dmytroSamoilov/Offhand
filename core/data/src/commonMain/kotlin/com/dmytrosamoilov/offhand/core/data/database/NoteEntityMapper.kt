package com.dmytrosamoilov.offhand.core.data.database

import com.dmytrosamoilov.offhand.core.data.domain.Folder
import com.dmytrosamoilov.offhand.core.data.domain.Note
import com.dmytrosamoilov.offhand.core.data.domain.NotePreset
import com.dmytrosamoilov.offhand.core.data.domain.NoteStatus
import com.dmytrosamoilov.offhand.core.data.domain.NoteStyleRef

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
    style = customStyleId?.let(NoteStyleRef::Custom) ?: NoteStyleRef.BuiltIn(NotePreset.fromName(preset)),
    folderId = folderId,
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
    preset = (style as? NoteStyleRef.BuiltIn)?.preset?.name ?: NotePreset.DEFAULT.name,
    folderId = folderId,
    customStyleId = (style as? NoteStyleRef.Custom)?.id,
)

internal fun FolderEntity.toDomain(): Folder = Folder(
    id = id,
    name = name,
    createdAtEpochMs = createdAtEpochMs,
)
