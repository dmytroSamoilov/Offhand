package com.dmytrosamoilov.offhand.feature.recording.domain

import com.dmytrosamoilov.offhand.core.data.domain.NoteStyleLimits

internal object NoteStyleDraftPrompt {

    fun build(): String = listOf(
        INTRO,
        DESIGN_RULES,
        THINKING_RULE,
        OUTPUT_INTRO,
        JSON_SHAPE,
        JSON_RULES,
    ).joinToString(LINE_BREAK)

    private const val LINE_BREAK = "\n"

    private val INTRO = """
        You will receive a short description, written by a user, of the kind of note they want written from their voice recordings.
        Design a note style for it: a name, what kind of note it is, and the section headings the note should be organised under, each with one sentence saying what belongs there.
    """.trimIndent()

    private val DESIGN_RULES = """
        Rules for the design:
        - Use between 1 and ${NoteStyleLimits.MAX_SECTIONS} sections, in the order they should appear in the note. Fewer clear sections beat many vague ones.
        - "heading": at most 4 words, a plain noun phrase, no numbering and no punctuation.
        - "guidance": one sentence that says what goes under the heading, written as an instruction that starts with a lowercase word, for example: each agreed task with who does it and by when.
        - "format": "bullets" when the section lists separate items such as tasks, facts, decisions or questions, "sentences" when it is narrative such as context, background or a summary, and "free" when the shape should follow the content.
        - "name": a short label for the style, at most 4 words.
        - "kind": completes the sentence The note is ... and starts with a or an, for example: a sales call debrief.
        - Write everything in the same language the description is written in.
        - Only use what the description asks for and what such a note obviously needs. Never invent requirements the user did not describe.
    """.trimIndent()

    private const val THINKING_RULE =
        "First think the design through inside one <thinking></thinking> block, in a few short " +
            "sentences: what the note is for, which sections it needs, which of them are lists. " +
            "Close the block before you answer."

    private const val OUTPUT_INTRO =
        "After the thinking block, output a single JSON object, exactly in this shape:"

    private const val JSON_SHAPE =
        """{"name": "...", "kind": "...", "sections": [{"heading": "...", "guidance": "...", "format": "bullets"}]}"""

    private val JSON_RULES = """
        Rules for the JSON output:
        - Output exactly one JSON object and no other text after it.
        - Never use double quotes inside the field values.
    """.trimIndent()
}
