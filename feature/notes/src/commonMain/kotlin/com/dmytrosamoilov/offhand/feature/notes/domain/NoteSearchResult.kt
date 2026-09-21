package com.dmytrosamoilov.offhand.feature.notes.domain

import com.dmytrosamoilov.offhand.core.data.domain.Note

data class TextMatch(
    val start: Int,
    val end: Int,
)

data class NoteSearchResult(
    val note: Note,
    val titleMatches: List<TextMatch> = emptyList(),
    val snippet: String? = null,
    val snippetMatches: List<TextMatch> = emptyList(),
)
