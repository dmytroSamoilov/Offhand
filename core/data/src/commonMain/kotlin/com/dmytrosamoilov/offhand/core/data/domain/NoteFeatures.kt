package com.dmytrosamoilov.offhand.core.data.domain

// What the app does with a recording besides keeping it. A summary is
// written from the transcript, so it needs transcription as well.
enum class NoteFeature {
    TRANSCRIPTION,
    SUMMARY,
}

data class NoteFeatures(
    val isTranscriptionEnabled: Boolean = true,
    val isSummaryEnabled: Boolean = true,
) {
    val writesSummary: Boolean get() = isTranscriptionEnabled && isSummaryEnabled
}

val UserPreferences.noteFeatures: NoteFeatures
    get() = NoteFeatures(isTranscriptionEnabled = transcriptionEnabled, isSummaryEnabled = summaryEnabled)
