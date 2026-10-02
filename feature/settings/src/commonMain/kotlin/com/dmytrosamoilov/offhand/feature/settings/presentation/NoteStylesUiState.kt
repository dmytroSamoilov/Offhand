package com.dmytrosamoilov.offhand.feature.settings.presentation

import com.dmytrosamoilov.offhand.core.data.domain.NoteStyleRef

data class NoteStylesUiState(
    val customStyles: List<CustomStyleOptionUi> = emptyList(),
    val isUnlocked: Boolean = false,
    val selected: NoteStyleRef = NoteStyleRef.DEFAULT,
)
