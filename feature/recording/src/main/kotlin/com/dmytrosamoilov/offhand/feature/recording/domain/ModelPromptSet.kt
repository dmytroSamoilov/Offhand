package com.dmytrosamoilov.offhand.feature.recording.domain

import com.dmytrosamoilov.offhand.core.ai.api.ModelFamily

internal sealed class ModelPromptSet {

    abstract val structureNote: String
    abstract val proofreadTranscript: String

    data object Qwen3 : ModelPromptSet() {

        override val structureNote = """
            You will receive a voice-to-text transcript of one audio recording.
            It was transcribed in separate segments. Each segment is wrapped in double quotes and segments are separated by commas, in the order they were spoken.
            First, think briefly inside <thinking></thinking> tags: read the segments in order, combine them into one continuous recording in your mind, and work out what it is about.
            After the thinking block, output a single JSON object and nothing else, exactly in this shape:
            $NOTE_JSON_SHAPE
            $NOTE_FIELD_RULES
            Rules for the JSON output:
            - Output exactly one JSON object and no other text after the thinking block.
            $NOTE_JSON_RULES
            $NOTE_FACTUALITY_RULES
        """.trimIndent()

        override val proofreadTranscript = PROOFREAD_TRANSCRIPT
    }

    data object Gemma4 : ModelPromptSet() {

        override val structureNote = """
            You will receive a voice-to-text transcript of one audio recording.
            It was transcribed in separate segments. Each segment is wrapped in double quotes and segments are separated by commas, in the order they were spoken.
            Read the segments in order, combine them into one continuous recording, and work out what it is about.
            Output a single JSON object and nothing else, exactly in this shape:
            $NOTE_JSON_SHAPE
            $NOTE_FIELD_RULES
            Rules for the JSON output:
            - Output exactly one JSON object and no other text before or after it.
            $NOTE_JSON_RULES
            $NOTE_FACTUALITY_RULES
        """.trimIndent()

        override val proofreadTranscript = PROOFREAD_TRANSCRIPT
    }

    companion object {

        fun forFamily(family: ModelFamily): ModelPromptSet = when (family) {
            ModelFamily.QWEN3 -> Qwen3
            ModelFamily.GEMMA4 -> Gemma4
        }
    }
}

private const val NOTE_JSON_SHAPE = """{"title": "...", "overview": "..."}"""

private val NOTE_FIELD_RULES = """
    Rules for the fields:
    - "title": a short title for the recording, at most 8 words.
    - "overview": a structured Markdown overview of the recording, organized around its main topics. Start a section heading line with "## " for each topic that is actually spoken about. Under a heading, write plain sentences that keep the important facts, names, dates, numbers and amounts. Use list item lines starting with "- " only for real lists that are spoken: tasks, action items, decisions, steps or items. When tasks or action items are mentioned, put each one on its own "- " line.
""".trimIndent()

private val NOTE_JSON_RULES = """
    - Never use double quotes inside the field values.
    - Use \n instead of real line breaks inside the field values.
""".trimIndent()

private const val NOTE_FACTUALITY_RULES =
    "Very important: only mention facts and numbers that are explicitly said in the recording — never invent or guess anything. Never add dates, years, or times that are not explicitly spoken. Write the title and the overview in the same language the recording is spoken in."

private val PROOFREAD_TRANSCRIPT = """
    You will receive one raw segment of a voice-to-text transcript.
    Rewrite the segment with correct punctuation and capitalization.
    Keep every spoken word in its original order, including filler words. Do not shorten, rephrase or summarize anything.
    Only change a word when it is clearly a transcription mistake and the surrounding sentence makes the intended word obvious.
    Never invent names, dates or numbers, and never translate the text into another language.
    Output only the corrected transcript text, with no explanations and nothing else.
""".trimIndent()
