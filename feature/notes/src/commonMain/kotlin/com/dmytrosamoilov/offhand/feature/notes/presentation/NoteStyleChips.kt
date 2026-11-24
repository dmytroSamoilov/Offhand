package com.dmytrosamoilov.offhand.feature.notes.presentation

import com.dmytrosamoilov.offhand.core.data.domain.NotePreset
import com.dmytrosamoilov.offhand.core.data.domain.NoteStyleRef

data class StyleChipUi(
    val style: NoteStyleRef,
    val customName: String?,
    val isCurrent: Boolean,
)

internal fun NotesUiState.styleChips(): List<StyleChipUi> {
    val note = selected ?: return emptyList()
    if (!isSummaryAvailable || note.status != NoteStatusUi.READY || note.transcript.isBlank()) return emptyList()
    val options = customStyles.map { StyleChipUi(NoteStyleRef.Custom(it.id), it.name, isCurrent = false) } +
        NotePreset.entries.map { StyleChipUi(NoteStyleRef.BuiltIn(it), customName = null, isCurrent = false) }
    val current = options.firstOrNull { it.style == note.style } ?: return options
    return listOf(current.copy(isCurrent = true)) + options.filter { it.style != note.style }
}

internal fun NotesUiState.playingPartIndex(): Int? {
    if (!playback.isPlaying) return null
    val parts = selected?.transcriptParts.orEmpty()
    return parts.indexOfLast { it.startMs <= playback.positionMs }.takeIf { it >= 0 }
}
