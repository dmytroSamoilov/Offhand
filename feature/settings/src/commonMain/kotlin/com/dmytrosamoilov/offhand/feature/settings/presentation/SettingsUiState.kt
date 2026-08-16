package com.dmytrosamoilov.offhand.feature.settings.presentation

import com.dmytrosamoilov.offhand.core.data.domain.NotePreset

data class SettingsUiState(
    val notePreset: NotePreset = NotePreset.DEFAULT,
    val isDynamicColorEnabled: Boolean = false,
)
