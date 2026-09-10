package com.dmytrosamoilov.offhand.feature.notes.presentation

import com.dmytrosamoilov.offhand.core.data.domain.CalendarEventSuggestion
import com.dmytrosamoilov.offhand.core.data.domain.NoteStyleRef

data class NotesUiState(
    val sections: List<NotesSectionUi> = emptyList(),
    val selected: NoteDetailUi? = null,
    val editor: NoteEditorUi? = null,
    val playback: AudioPlaybackUi = AudioPlaybackUi(),
    val pendingDeleteNoteId: Long? = null,
    val isRetranscribeConfirmationVisible: Boolean = false,
    val isShareDialogVisible: Boolean = false,
    val isPresetSheetVisible: Boolean = false,
    val pendingShare: NoteShareUi? = null,
    val isDeveloperMode: Boolean = false,
    val noteProgress: Map<Long, Int> = emptyMap(),
    val modelPreparation: ModelPreparationUi? = null,
    val searchQuery: String = "",
    val folders: List<FolderUi> = emptyList(),
    val selectedFolderId: Long? = null,
    val folderEditor: FolderEditorUi? = null,
    val pendingDeleteFolderId: Long? = null,
    val moveToFolder: MoveToFolderUi? = null,
    val customStyles: List<NoteStyleOptionUi> = emptyList(),
    val isAudioImportUnlocked: Boolean = false,
    val importMessage: ImportMessageUi? = null,
    val smartSuggestions: SmartSuggestionsUi? = null,
    val pendingCalendarEvent: CalendarEventSuggestion? = null,
)

sealed interface SmartSuggestionsUi {
    data object Loading : SmartSuggestionsUi
    data object NotRun : SmartSuggestionsUi
    data object Empty : SmartSuggestionsUi
    data class Ready(val events: List<CalendarEventUi>) : SmartSuggestionsUi
}

data class CalendarEventUi(
    val index: Int,
    val title: String,
    val whenText: String,
    val isAllDay: Boolean,
    val location: String,
    val details: String,
    val isAdded: Boolean,
)

enum class ImportMessageUi {
    LOCKED,
    UNSUPPORTED,
    TOO_LONG,
    UNREADABLE,
}

data class NoteStyleOptionUi(
    val id: Long,
    val name: String,
    val description: String,
)

data class MoveToFolderUi(
    val noteId: Long,
    val currentFolderId: Long?,
)

data class FolderUi(
    val id: Long,
    val name: String,
    val noteCount: Int,
)

data class FolderEditorUi(
    val folderId: Long?,
    val name: String,
    val error: FolderNameErrorUi? = null,
)

enum class FolderNameErrorUi {
    BLANK,
    TOO_LONG,
    DUPLICATE,
}

data class NoteShareUi(
    val filePaths: List<String>,
    val mimeType: String,
)

data class ModelPreparationUi(
    val progressPercent: Int,
)

data class NotesSectionUi(
    val dayLabel: NoteDayLabelUi,
    val notes: List<NoteCardUi>,
)

sealed interface NoteDayLabelUi {
    data object Today : NoteDayLabelUi
    data object Yesterday : NoteDayLabelUi
    data class Date(val text: String) : NoteDayLabelUi
}

data class NoteCardUi(
    val id: Long,
    val title: String,
    val dayLabel: NoteDayLabelUi,
    val time: String,
    val preview: String,
    val durationText: String?,
    val status: NoteStatusUi,
    val titleHighlights: List<TextRangeUi> = emptyList(),
    val previewHighlights: List<TextRangeUi> = emptyList(),
    val folderName: String? = null,
)

data class TextRangeUi(
    val start: Int,
    val end: Int,
)

data class NoteDetailUi(
    val id: Long,
    val title: String,
    val body: String,
    val transcript: String,
    val createdAt: String,
    val wordCount: Int,
    val hasAudio: Boolean,
    val metrics: NoteMetricsUi?,
    val status: NoteStatusUi,
    val style: NoteStyleRef,
    val folderId: Long? = null,
    val folderName: String? = null,
)

enum class NoteStatusUi {
    PROCESSING,
    READY,
    FAILED,
}

data class NoteMetricsUi(
    val transcriptionTime: String,
    val structuringTime: String,
    val hardwareBackend: String,
)

data class AudioPlaybackUi(
    val isAvailable: Boolean = false,
    val isPlaying: Boolean = false,
    val progress: Float = 0f,
    val positionText: String = "0:00",
    val durationText: String = "0:00",
)

data class NoteEditorUi(
    val title: String,
    val transcript: String,
)
