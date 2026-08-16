package com.dmytrosamoilov.offhand.core.data.domain

enum class NotePreset {
    SUMMARY,
    MEETING,
    VISIT,
    LEGAL;

    companion object {
        val DEFAULT = SUMMARY

        fun fromName(name: String?): NotePreset = entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}
