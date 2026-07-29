package com.dmytrosamoilov.offhand.feature.settings.presentation

import com.dmytrosamoilov.offhand.core.ui.component.NotePresetOption

data class SettingsUiState(
    val notePreset: NotePresetOption = NotePresetOption.SUMMARY,
    val isDynamicColorEnabled: Boolean = false,
)
