package com.dmytrosamoilov.offhand.core.data.domain

// How far a note's stored audio has been transcribed; the finished windows
// live in the note's transcript. Present only while transcription is unfinished.
data class TranscriptionCheckpoint(
    val noteId: Long,
    val transcribedBytes: Long,
    val transcriptionTimeMs: Long,
)

interface TranscriptionCheckpointRepository {

    suspend fun getCheckpoint(noteId: Long): TranscriptionCheckpoint?

    suspend fun saveCheckpoint(checkpoint: TranscriptionCheckpoint)

    suspend fun clearCheckpoint(noteId: Long)
}
