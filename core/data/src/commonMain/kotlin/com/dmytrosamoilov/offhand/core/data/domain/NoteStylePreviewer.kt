package com.dmytrosamoilov.offhand.core.data.domain

data class NoteStylePreview(
    val title: String,
    val overview: String,
)

interface NoteStylePreviewer {

    suspend fun preview(style: CustomNoteStyle, sampleTranscript: String): NoteStylePreview?
}
