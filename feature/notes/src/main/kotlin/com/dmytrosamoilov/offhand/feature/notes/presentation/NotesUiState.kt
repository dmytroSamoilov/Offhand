package com.dmytrosamoilov.offhand.feature.notes.presentation

data class NotesUiState(
    val sections: List<NotesSectionUi> = emptyList(),
    val selected: NoteDetailUi? = null,
    val editor: NoteEditorUi? = null,
    val playback: AudioPlaybackUi = AudioPlaybackUi(),
    val isDeleteConfirmationVisible: Boolean = false,
    val isRetranscribeConfirmationVisible: Boolean = false,
    val isDeveloperMode: Boolean = false,
    val noteProgress: Map<Long, Int> = emptyMap(),
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
