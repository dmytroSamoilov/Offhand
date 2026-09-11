package com.dmytrosamoilov.offhand.feature.recording.domain

import com.dmytrosamoilov.offhand.core.data.domain.NoteStyleLanguage

internal object NoteStylePrompt {

    fun fieldRules(spec: NoteStyleSpec): String = listOf(
        FIELD_RULES_HEADER,
        TITLE_RULE,
        "$OVERVIEW_RULE_PREFIX${spec.overviewRule}",
        EMPTY_SECTION_RULE,
    ).joinToString(LINE_BREAK)

    fun polishStructureRule(spec: NoteStyleSpec): String = listOf(
        sectionPolishRule(spec.sections),
        userInstructionsPolishRule(spec.userInstructions),
    ).filter { it.isNotBlank() }.joinToString(LINE_BREAK)

    fun polishFieldRules(): String = listOf(
        FIELD_RULES_HEADER,
        POLISH_TITLE_RULE,
        "$OVERVIEW_RULE_PREFIX$POLISHED_OVERVIEW_RULE",
    ).joinToString(LINE_BREAK)

    fun structureLanguageRule(spec: NoteStyleSpec): String = when (spec.language) {
        NoteStyleLanguage.RECORDING -> RECORDING_LANGUAGE_RULE
        NoteStyleLanguage.ENGLISH -> ENGLISH_LANGUAGE_RULE
    } + languageException(spec)

    fun polishLanguageRule(spec: NoteStyleSpec): String = when (spec.language) {
        NoteStyleLanguage.RECORDING -> DRAFT_LANGUAGE_RULE
        NoteStyleLanguage.ENGLISH -> ENGLISH_LANGUAGE_RULE
    } + languageException(spec)

    private fun languageException(spec: NoteStyleSpec): String =
        if (spec.userInstructions.isEmpty()) "" else " $USER_LANGUAGE_EXCEPTION"

    private fun userInstructionsPolishRule(instructions: List<SectionInstruction>): String =
        instructions.joinToString(LINE_BREAK) { instruction ->
            "- Under \"${instruction.heading}\" the draft follows these instructions; keep to them, " +
                "even where they differ from the other rules: ${instruction.text}."
        }

    fun markdownKind(kind: String): String = "$kind in Markdown, organised under section headings"

    fun quoteList(sections: List<String>): String =
        sections.joinToString(SECTION_LIST_SEPARATOR) { "\"$it\"" }

    private fun sectionPolishRule(sections: List<String>): String = listOf(
        "- Keep only these section headings: ${quoteList(sections)}. Never use any other heading.",
        "- Move a point that sits under the wrong heading to the heading where it belongs. " +
            "When a point clearly belongs under one of the allowed headings that the draft " +
            "does not have yet, add that heading and move the point under it.",
        "- Keep each point as one \"- \" line in sections that use \"- \" lines, and remove " +
            "a heading that has nothing under it as well as lines that only say none or not mentioned.",
    ).joinToString(LINE_BREAK)

    private const val LINE_BREAK = "\n"
    private const val SECTION_LIST_SEPARATOR = ", "
    private const val FIELD_RULES_HEADER = "Rules for the fields:"
    private const val TITLE_RULE =
        "- \"title\": a short title for the recording, at most 8 words."
    private const val POLISH_TITLE_RULE =
        "- \"title\": a short title for the note, at most 8 words."
    private const val OVERVIEW_RULE_PREFIX = "- \"overview\": "
    private const val POLISHED_OVERVIEW_RULE = "the full polished note and nothing else."
    private const val EMPTY_SECTION_RULE =
        "- Write a heading only when the recording really contains that kind of content. " +
            "Never write a heading with nothing under it, and never write none, not mentioned or N/A."
    private const val RECORDING_LANGUAGE_RULE =
        "Write the title and the overview in the same language the recording is spoken in."
    private const val DRAFT_LANGUAGE_RULE =
        "Write the title and the overview in the same language the draft is written in."
    private const val ENGLISH_LANGUAGE_RULE =
        "Write the title and the overview in English, whatever language the recording is spoken in."
    private const val USER_LANGUAGE_EXCEPTION =
        "Where a section's own instructions ask for another language, tone or form, that section follows its instructions instead."
}
