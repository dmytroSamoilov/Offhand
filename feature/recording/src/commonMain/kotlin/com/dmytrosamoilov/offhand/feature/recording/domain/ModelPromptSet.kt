package com.dmytrosamoilov.offhand.feature.recording.domain

import com.dmytrosamoilov.offhand.core.ai.api.ModelFamily
import com.dmytrosamoilov.offhand.core.data.domain.NotePreset

internal sealed class ModelPromptSet {

    abstract fun structureNote(preset: NotePreset): String

    abstract fun polishNote(preset: NotePreset): String

    data object Gemma4 : ModelPromptSet() {

        override fun structureNote(preset: NotePreset): String = listOf(
            TRANSCRIPT_INTRO,
            OUTPUT_SHAPE_INTRO,
            NOTE_JSON_SHAPE,
            NotePresetPrompt.fieldRules(preset),
            NOTE_JSON_RULES,
            NOTE_FACTUALITY_RULES,
        ).joinToString(LINE_BREAK)

        override fun polishNote(preset: NotePreset): String = listOf(
            "$POLISH_INTRO_PREFIX${NotePresetPrompt.noteKind(preset)}.",
            POLISH_CONTEXT,
            POLISH_TASKS_HEADER,
            POLISH_DEDUPE_RULE,
            POLISH_PROOFREAD_RULE,
            NotePresetPrompt.polishStructureRule(preset),
            OUTPUT_SHAPE_INTRO,
            NOTE_JSON_SHAPE,
            NotePresetPrompt.polishFieldRules(),
            NOTE_JSON_RULES,
            POLISH_FACTUALITY_RULES,
        ).joinToString(LINE_BREAK)
    }

    companion object {

        fun forFamily(family: ModelFamily): ModelPromptSet = when (family) {
            ModelFamily.GEMMA4 -> Gemma4
        }
    }
}

private const val LINE_BREAK = "\n"

private val TRANSCRIPT_INTRO = """
    You will receive a voice-to-text transcript of one audio recording.
    It was transcribed in separate segments. Each segment is wrapped in double quotes and segments are separated by commas, in the order they were spoken.
    Read the segments in order, combine them into one continuous recording, and work out what it is about.
""".trimIndent()

private const val OUTPUT_SHAPE_INTRO =
    "Output a single JSON object and nothing else, exactly in this shape:"

private const val NOTE_JSON_SHAPE = """{"title": "...", "overview": "..."}"""

private val NOTE_JSON_RULES = """
    Rules for the JSON output:
    - Output exactly one JSON object and no other text before or after it.
    - Never use double quotes inside the field values.
    - Use \n instead of real line breaks inside the field values.
""".trimIndent()

private const val NOTE_FACTUALITY_RULES =
    "Very important: only mention facts and numbers that are explicitly said in the recording — never invent or guess anything. Never add dates, years, or times that are not explicitly spoken. Write the title and the overview in the same language the recording is spoken in."

private const val POLISH_INTRO_PREFIX = "You will receive the draft of one note. The note is "

private val POLISH_CONTEXT = """
    The draft was written down from a voice-to-text transcript of one audio recording.
    Because of that it may repeat the same point in different words, and single words in it may have been transcribed wrongly.
""".trimIndent()

private const val POLISH_TASKS_HEADER = "Rewrite the draft into its final polished version:"

private const val POLISH_DEDUPE_RULE =
    "- Say each thing only once. When the same point appears more than once, keep the " +
        "clearest and most complete version and drop the rest."

private const val POLISH_PROOFREAD_RULE =
    "- When a word is clearly wrong for the meaning of its sentence, replace it with the " +
        "word that was most likely spoken, and fix broken grammar the same way. Leave every " +
        "word that already makes sense unchanged."

private const val POLISH_FACTUALITY_RULES =
    "Very important: keep every name, number, amount, date and decision exactly as written in the draft, and never add anything the draft does not say. Write the title and the overview in the same language the draft is written in."
