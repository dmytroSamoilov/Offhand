package com.dmytrosamoilov.offhand.core.data.domain

// A folder's own summary style overrides the default for notes recorded into
// it or moved into it; null means the folder follows the default in Settings.
data class Folder(
    val id: Long,
    val name: String,
    val createdAtEpochMs: Long,
    val style: NoteStyleRef? = null,
    val position: Int = 0,
)
