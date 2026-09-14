@file:OptIn(ExperimentalTime::class)

package com.dmytrosamoilov.offhand.feature.recording.domain

import com.dmytrosamoilov.offhand.core.ai.api.AiBackend
import com.dmytrosamoilov.offhand.core.ai.api.ModelManager
import com.dmytrosamoilov.offhand.core.ai.api.TokenEstimator
import com.dmytrosamoilov.offhand.core.data.domain.CalendarEventSuggestion
import com.dmytrosamoilov.offhand.core.data.domain.Note
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

// Runs the calendar prompt on a finished note; callers hold the session's
// processing lock so the model is never shared with structuring.
class CalendarEventExtractor(
    private val aiBackend: AiBackend,
    private val modelManager: ModelManager,
) {

    suspend fun extract(note: Note): List<CalendarEventSuggestion> {
        val timeZone = TimeZone.currentSystemDefault()
        val recordedAt = Instant.fromEpochMilliseconds(note.createdAtEpochMs).toLocalDateTime(timeZone)
        val prompt = CalendarSuggestionPrompt.build(recordedAt)
        val input = inputFor(note, prompt)
        if (input.isBlank()) return emptyList()
        return CalendarSuggestionParser.parse(aiBackend.processText(prompt, input).text, timeZone)
    }

    // The transcript carries the dates verbatim; the overview is the fallback
    // when the transcript no longer fits the context window beside the prompt.
    private fun inputFor(note: Note, prompt: String): String {
        val budget = modelManager.model.maxTokens - TokenEstimator.approxText(prompt) - OUTPUT_TOKEN_RESERVE
        val transcript = note.transcript.replace('"', '\'')
        if (transcript.isNotBlank() && TokenEstimator.approxText(transcript) <= budget) return transcript
        return note.body.replace('"', '\'').take(budget * CHARS_PER_TOKEN)
    }

    private companion object {
        const val OUTPUT_TOKEN_RESERVE = 1_000
        const val CHARS_PER_TOKEN = 3
    }
}
