package com.dmytrosamoilov.offhand.feature.settings.presentation

import com.dmytrosamoilov.offhand.core.data.domain.NoteStyleRef

data class SettingsUiState(
    val noteStyle: NoteStyleRef = NoteStyleRef.DEFAULT,
    val customStyles: List<CustomStyleOptionUi> = emptyList(),
    val isCustomStylesUnlocked: Boolean = false,
    val isDynamicColorEnabled: Boolean = false,
    val isAppLockEnabled: Boolean = false,
    val isDeviceSecure: Boolean = false,
    val isAudioImportUnlocked: Boolean = false,
    val importNotice: ImportNoticeUi? = null,
)

sealed interface ImportNoticeUi {
    data object Locked : ImportNoticeUi
    data object Unreadable : ImportNoticeUi
    data class Started(val fileCount: Int) : ImportNoticeUi
}

data class CustomStyleOptionUi(
    val id: Long,
    val name: String,
    val description: String,
)
