package com.dmytrosamoilov.offhand.core.data.domain

// NOTE_STYLES covers the built-in Meeting, Visit and Legal styles, the
// default style and restyling a note; FOLDERS the third folder onwards;
// BACKUP_AUDIO the recordings inside a backup file.
enum class ProFeature {
    GENERAL,
    CUSTOM_STYLES,
    NOTE_STYLES,
    DOCUMENT_EXPORT,
    SMART_SUGGESTIONS,
    AUDIO_IMPORT,
    FOLDER_STYLES,
    FOLDERS,
    BACKUP_AUDIO,
}

// Summary is the free style; every other choice meets the paywall.
fun NoteStyleRef.requiredProFeature(): ProFeature? = when (this) {
    is NoteStyleRef.Custom -> ProFeature.CUSTOM_STYLES
    is NoteStyleRef.BuiltIn -> if (preset == NotePreset.SUMMARY) null else ProFeature.NOTE_STYLES
}
