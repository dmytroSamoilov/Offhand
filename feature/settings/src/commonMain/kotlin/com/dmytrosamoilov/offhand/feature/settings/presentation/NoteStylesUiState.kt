package com.dmytrosamoilov.offhand.feature.settings.presentation

data class NoteStylesUiState(
    val customStyles: List<CustomStyleOptionUi> = emptyList(),
    val isUnlocked: Boolean = false,
    val pendingDeleteId: Long? = null,
)
