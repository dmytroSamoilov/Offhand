package com.dmytrosamoilov.offhand.feature.recording.domain

import com.dmytrosamoilov.offhand.core.data.domain.NoteStyleLanguage
import com.dmytrosamoilov.offhand.core.data.domain.NoteStyleRef

internal data class NoteStyleSpec(
    val ref: NoteStyleRef,
    val kind: String,
    val sections: List<String>,
    val overviewRule: String,
    val language: NoteStyleLanguage,
    val userInstructions: List<SectionInstruction> = emptyList(),
    val listSections: List<String> = emptyList(),
    val proseSections: List<String> = emptyList(),
    val paragraphSections: List<String> = emptyList(),
    val polishRules: List<String> = emptyList(),
    val headingTranslations: Map<String, List<String>> = emptyMap(),
    val isCondensedByPolish: Boolean = false,
)

internal data class SectionInstruction(
    val heading: String,
    val text: String,
)
