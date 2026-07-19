package com.dmytrosamoilov.offhand.core.data.domain

data class Note(
    val id: Long,
    val title: String,
    val body: String,
    val transcript: String,
    val createdAtEpochMs: Long,
    val transcriptionTimeMs: Long?,
    val structuringTimeMs: Long?,
    val hardwareBackend: String?,
    val audioFileName: String? = null,
    val durationMs: Long? = null,
    val status: NoteStatus = NoteStatus.READY,
)

enum class NoteStatus {
    PROCESSING,
    READY,
    FAILED,
}
