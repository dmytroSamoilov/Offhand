package com.dmytrosamoilov.offhand.core.data.repository

import com.dmytrosamoilov.offhand.core.data.database.TranscriptionCheckpointDao
import com.dmytrosamoilov.offhand.core.data.database.TranscriptionCheckpointEntity
import com.dmytrosamoilov.offhand.core.data.domain.TranscriptionCheckpoint
import com.dmytrosamoilov.offhand.core.data.domain.TranscriptionCheckpointRepository

internal class RoomTranscriptionCheckpointRepository(
    private val transcriptionCheckpointDao: TranscriptionCheckpointDao,
) : TranscriptionCheckpointRepository {

    override suspend fun getCheckpoint(noteId: Long): TranscriptionCheckpoint? =
        transcriptionCheckpointDao.getByNoteId(noteId)?.let { entity ->
            TranscriptionCheckpoint(entity.noteId, entity.transcribedBytes, entity.transcriptionTimeMs)
        }

    override suspend fun saveCheckpoint(checkpoint: TranscriptionCheckpoint) = transcriptionCheckpointDao.upsert(
        TranscriptionCheckpointEntity(checkpoint.noteId, checkpoint.transcribedBytes, checkpoint.transcriptionTimeMs),
    )

    override suspend fun clearCheckpoint(noteId: Long) = transcriptionCheckpointDao.deleteByNoteId(noteId)
}
