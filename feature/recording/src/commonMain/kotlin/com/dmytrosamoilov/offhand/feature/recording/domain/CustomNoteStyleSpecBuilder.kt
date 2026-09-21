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
        val effective = sections.ifEmpty { listOf(SUMMARY_SECTION) }
        val headings = effective.map { it.promptHeading() }
        return NoteStyleSpec(
            ref = ref,
            kind = NoteStylePrompt.markdownKind(kind),
            sections = headings,
            overviewRule = overviewRule(kind, headings, effective),
            language = language,
            userInstructions = userInstructions(effective, headings),
        )
    }

    // A free section with guidance is the user's own slot: the model is told
    // to follow it over every form, tone and language rule of ours.
    private fun userInstructions(sections: List<NoteStyleSection>, headings: List<String>): List<SectionInstruction> =
        sections.zip(headings)
            .filter { (section, _) -> section.format == SectionFormat.FREE }
            .mapNotNull { (section, heading) ->
                section.guidance.toPromptText().takeIf { it.isNotBlank() }?.let { SectionInstruction(heading, it) }
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
            append(" ${sectionRule(section, heading)}.")
        }
    }

    private fun sectionRule(section: NoteStyleSection, heading: String): String {
        val guidance = section.guidance.toPromptText()
        val rule = formatRule(section.format, hasGuidance = guidance.isNotBlank())
        return "Under \"$heading\" $rule" + if (guidance.isBlank()) "" else ": $guidance"
    }

    private fun formatRule(format: SectionFormat, hasGuidance: Boolean): String = when (format) {
        SectionFormat.SENTENCES -> SENTENCES_RULE
        SectionFormat.BULLETS -> BULLETS_RULE
        SectionFormat.FREE -> if (hasGuidance) INSTRUCTIONS_RULE else FREE_RULE
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
    // A style without sections still yields one heading, so the note keeps
    // the same JSON shape and the same polish rules.
    private val SUMMARY_SECTION = NoteStyleSection(heading = "Summary", guidance = "", format = SectionFormat.FREE)
    private const val SENTENCES_RULE = "write short plain sentences"
    private const val BULLETS_RULE = "write one \"- \" line per point"
    private const val FREE_RULE = "write it in whatever form fits the content best"
    private const val INSTRUCTIONS_RULE =
        "follow these instructions exactly, even where they differ from every other rule about form, tone or language"
    private val UNSAFE_CHARS = Regex("[\"{}\\\\`<>]")
    private val WHITESPACE = Regex("\\s+")
}
