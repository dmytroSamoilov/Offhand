package com.dmytrosamoilov.offhand.feature.recording.domain

import com.dmytrosamoilov.offhand.core.data.domain.CustomNoteStyle
import com.dmytrosamoilov.offhand.core.data.domain.NoteStyleLanguage
import com.dmytrosamoilov.offhand.core.data.domain.NoteStyleRef
import com.dmytrosamoilov.offhand.core.data.domain.NoteStyleSection
import com.dmytrosamoilov.offhand.core.data.domain.SectionFormat

internal object CustomNoteStyleSpecBuilder {

    fun build(style: CustomNoteStyle): NoteStyleSpec =
        build(NoteStyleRef.Custom(style.id), style.noteKind, style.sections, style.language)

    fun build(
        ref: NoteStyleRef,
        noteKind: String,
        sections: List<NoteStyleSection>,
        language: NoteStyleLanguage,
    ): NoteStyleSpec {
        val kind = noteKind.toPromptText().ifBlank { DEFAULT_KIND }
        val headings = sections.map { it.promptHeading() }
        return NoteStyleSpec(
            ref = ref,
            kind = NoteStylePrompt.markdownKind(kind),
            sections = headings,
            overviewRule = overviewRule(kind, headings, sections),
            language = language,
        )
    }

    private fun overviewRule(
        kind: String,
        headings: List<String>,
        sections: List<NoteStyleSection>,
    ): String = buildString {
        append("$kind in Markdown, built only from these section headings: ")
        append(NoteStylePrompt.quoteList(headings))
        append(". ")
        append(ONE_HEADING_RULE)
        sections.zip(headings).forEach { (section, heading) ->
            append(" Under \"$heading\" ${formatRule(section.format)}")
            section.guidance.toPromptText().takeIf { it.isNotBlank() }?.let { append(": $it") }
            append(".")
        }
    }

    private fun formatRule(format: SectionFormat): String = when (format) {
        SectionFormat.SENTENCES -> SENTENCES_RULE
        SectionFormat.BULLETS -> BULLETS_RULE
    }

    private fun NoteStyleSection.promptHeading(): String =
        "$HEADING_PREFIX${heading.toPromptText().trimStart('#', ' ')}"

    private fun String.toPromptText(): String = replace(UNSAFE_CHARS, " ")
        .replace(WHITESPACE, " ")
        .trim()
        .trimEnd('.')

    private const val DEFAULT_KIND = "a note"
    private const val HEADING_PREFIX = "## "
    private const val ONE_HEADING_RULE =
        "Every point goes under exactly one heading and is never repeated under another one."
    private const val SENTENCES_RULE = "write short plain sentences"
    private const val BULLETS_RULE = "write one \"- \" line per point"
    private val UNSAFE_CHARS = Regex("[\"{}\\\\`<>]")
    private val WHITESPACE = Regex("\\s+")
}
