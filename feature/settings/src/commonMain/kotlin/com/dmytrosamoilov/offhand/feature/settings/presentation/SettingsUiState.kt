package com.dmytrosamoilov.offhand.feature.settings.presentation

import com.dmytrosamoilov.offhand.core.data.domain.NoteStyleRef
import com.dmytrosamoilov.offhand.core.data.domain.ProOverride

data class SettingsUiState(
    val noteStyle: NoteStyleRef = NoteStyleRef.DEFAULT,
    val customStyles: List<CustomStyleOptionUi> = emptyList(),
    val isDynamicColorEnabled: Boolean = false,
    val isAppLockEnabled: Boolean = false,
    val isTelemetryEnabled: Boolean = false,
    val isDeviceSecure: Boolean = false,
    val isAudioImportUnlocked: Boolean = false,
    val freeImportsLeft: Int? = null,
    val allowsMultipleImports: Boolean = false,
    val isSmartSuggestionsEnabled: Boolean = false,
    val isTranscriptionEnabled: Boolean = true,
    val isSummaryEnabled: Boolean = true,
    val isSmartSuggestionsUnlocked: Boolean = false,
    val pro: ProStatusUi = ProStatusUi.Free,
    val proOverride: ProOverride? = null,
    val isImportPickerRequested: Boolean = false,
    val isRedeemFallbackRequested: Boolean = false,
    val importNotice: ImportNoticeUi? = null,
)

sealed interface ProStatusUi {
    data object Free : ProStatusUi
    data class Trial(val endsAtMs: Long?) : ProStatusUi
    // The fourteen free days: elapsed is the share of the trial already used.
    data class AppTrial(val daysLeft: Int, val elapsed: Float) : ProStatusUi
    data class Yearly(val renewsAtMs: Long?) : ProStatusUi
    data class Monthly(val renewsAtMs: Long?) : ProStatusUi
    data object Lifetime : ProStatusUi
    data object Included : ProStatusUi
}

sealed interface ImportNoticeUi {
    data object Unreadable : ImportNoticeUi
    data object OneVideoAtATime : ImportNoticeUi
    data class Started(val fileCount: Int) : ImportNoticeUi
}

data class CustomStyleOptionUi(
    val id: Long,
    val name: String,
    val description: String,
)
