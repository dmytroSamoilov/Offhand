package com.dmytrosamoilov.offhand.feature.settings.presentation

import com.dmytrosamoilov.offhand.core.data.domain.NoteStyleRef
import com.dmytrosamoilov.offhand.core.data.domain.ProOverride

data class SettingsUiState(
    val noteStyle: NoteStyleRef = NoteStyleRef.DEFAULT,
    val customStyles: List<CustomStyleOptionUi> = emptyList(),
    val isCustomStylesUnlocked: Boolean = false,
    val isDynamicColorEnabled: Boolean = false,
    val isAppLockEnabled: Boolean = false,
    val isDeviceSecure: Boolean = false,
    val isAudioImportUnlocked: Boolean = false,
    val isSmartSuggestionsEnabled: Boolean = false,
    val isSmartSuggestionsUnlocked: Boolean = false,
    val pro: ProStatusUi = ProStatusUi.Free,
    val proOverride: ProOverride? = null,
    val isImportPickerRequested: Boolean = false,
    val importNotice: ImportNoticeUi? = null,
)

sealed interface ProStatusUi {
    data object Free : ProStatusUi
    data class Trial(val endsAtMs: Long?) : ProStatusUi
    data class Yearly(val renewsAtMs: Long?) : ProStatusUi
    data object Lifetime : ProStatusUi
}

sealed interface ImportNoticeUi {
    data object Unreadable : ImportNoticeUi
    data class Started(val fileCount: Int) : ImportNoticeUi
}

data class CustomStyleOptionUi(
    val id: Long,
    val name: String,
    val description: String,
)
