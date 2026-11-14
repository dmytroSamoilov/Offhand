package com.dmytrosamoilov.offhand.core.data.domain

enum class AudioImportKind {
    AUDIO,
    VIDEO,
}

// A video is imported for its audio track; the kind only decides the rules
// around it (one video at a time).
data class AudioImportSource(
    val handle: String,
    val displayName: String,
    val kind: AudioImportKind = AudioImportKind.AUDIO,
)
