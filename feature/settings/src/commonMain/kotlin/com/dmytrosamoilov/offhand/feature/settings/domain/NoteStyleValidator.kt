package com.dmytrosamoilov.offhand.feature.settings.domain

import com.dmytrosamoilov.offhand.core.data.domain.CustomNoteStyle
import com.dmytrosamoilov.offhand.core.data.domain.NoteStyleLimits
import com.dmytrosamoilov.offhand.core.data.domain.NoteStyleSection

enum class NoteStyleFieldError {
    BLANK,
    TOO_LONG,
    DUPLICATE,
}

enum class NoteStyleSectionsError {
    NONE,
    TOO_MANY,
}

data class NoteStyleErrors(
    val name: NoteStyleFieldError? = null,
    val noteKind: NoteStyleFieldError? = null,
    val sections: NoteStyleSectionsError? = null,
    val headings: Map<Int, NoteStyleFieldError> = emptyMap(),
    val guidance: Map<Int, NoteStyleFieldError> = emptyMap(),
) {
    val isEmpty: Boolean
        get() = name == null && noteKind == null && sections == null && headings.isEmpty() && guidance.isEmpty()
}

sealed interface NoteStyleValidation {
    data class Valid(val style: CustomNoteStyle) : NoteStyleValidation
    data class Invalid(val errors: NoteStyleErrors) : NoteStyleValidation
}

object NoteStyleValidator {

    const val MAX_NAME_LENGTH = NoteStyleLimits.MAX_NAME_LENGTH
    const val MAX_KIND_LENGTH = NoteStyleLimits.MAX_KIND_LENGTH
    const val MAX_HEADING_LENGTH = NoteStyleLimits.MAX_HEADING_LENGTH
    const val MAX_GUIDANCE_LENGTH = NoteStyleLimits.MAX_GUIDANCE_LENGTH
    const val MIN_SECTIONS = NoteStyleLimits.MIN_SECTIONS
    const val MAX_SECTIONS = NoteStyleLimits.MAX_SECTIONS

    fun validate(style: CustomNoteStyle, existing: List<CustomNoteStyle>): NoteStyleValidation {
        val normalized = style.normalized()
        val errors = NoteStyleErrors(
            name = nameError(normalized, existing),
            noteKind = NoteStyleFieldError.TOO_LONG.takeIf { normalized.noteKind.length > MAX_KIND_LENGTH },
            sections = sectionsError(normalized.sections),
            headings = headingErrors(normalized.sections),
            guidance = guidanceErrors(normalized.sections),
        )
        return if (errors.isEmpty) NoteStyleValidation.Valid(normalized) else NoteStyleValidation.Invalid(errors)
    }

    private fun CustomNoteStyle.normalized(): CustomNoteStyle = copy(
        name = name.collapseWhitespace(),
        noteKind = noteKind.collapseWhitespace(),
        sections = sections.map { section ->
            section.copy(
                heading = section.heading.collapseWhitespace().trimStart('#', ' '),
                guidance = section.guidance.collapseWhitespace(),
            )
        },
    )

    private fun nameError(style: CustomNoteStyle, existing: List<CustomNoteStyle>): NoteStyleFieldError? = when {
        style.name.isEmpty() -> NoteStyleFieldError.BLANK
        style.name.length > MAX_NAME_LENGTH -> NoteStyleFieldError.TOO_LONG
        existing.any { it.id != style.id && it.name.equals(style.name, ignoreCase = true) } ->
            NoteStyleFieldError.DUPLICATE
        else -> null
    }

    private fun sectionsError(sections: List<NoteStyleSection>): NoteStyleSectionsError? = when {
        sections.size < MIN_SECTIONS -> NoteStyleSectionsError.NONE
        sections.size > MAX_SECTIONS -> NoteStyleSectionsError.TOO_MANY
        else -> null
    }

    private fun headingErrors(sections: List<NoteStyleSection>): Map<Int, NoteStyleFieldError> =
        sections.withIndex().mapNotNull { (index, section) ->
            headingError(index, section.heading, sections)?.let { index to it }
        }.toMap()

    private fun headingError(index: Int, heading: String, sections: List<NoteStyleSection>): NoteStyleFieldError? = when {
        heading.isEmpty() -> NoteStyleFieldError.BLANK
        heading.length > MAX_HEADING_LENGTH -> NoteStyleFieldError.TOO_LONG
        sections.withIndex().any { (other, section) -> other != index && section.heading.equals(heading, ignoreCase = true) } ->
            NoteStyleFieldError.DUPLICATE
        else -> null
    }

    private fun guidanceErrors(sections: List<NoteStyleSection>): Map<Int, NoteStyleFieldError> =
        sections.withIndex()
            .filter { (_, section) -> section.guidance.length > MAX_GUIDANCE_LENGTH }
            .associate { (index, _) -> index to NoteStyleFieldError.TOO_LONG }

    private fun String.collapseWhitespace(): String = trim().replace(WHITESPACE_RUNS, " ")

    private val WHITESPACE_RUNS = Regex("\\s+")
}
