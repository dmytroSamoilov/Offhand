package com.dmytrosamoilov.offhand.core.data.domain

data class CustomNoteStyle(
    val id: Long,
    val name: String,
    val noteKind: String,
    val language: NoteStyleLanguage,
    val sections: List<NoteStyleSection>,
    val createdAtEpochMs: Long,
)

data class NoteStyleSection(
    val heading: String,
    val guidance: String,
    val format: SectionFormat,
)

enum class SectionFormat {
    SENTENCES,
    BULLETS;

    companion object {
        fun fromName(name: String?): SectionFormat = entries.firstOrNull { it.name == name } ?: SENTENCES
    }
}

enum class NoteStyleLanguage {
    RECORDING,
    ENGLISH;

    companion object {
        fun fromName(name: String?): NoteStyleLanguage = entries.firstOrNull { it.name == name } ?: RECORDING
    }
}
