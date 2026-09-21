package com.dmytrosamoilov.offhand.testing.fakes

import com.dmytrosamoilov.offhand.core.ai.api.AiBackend
import com.dmytrosamoilov.offhand.core.ai.api.AiResult
import com.dmytrosamoilov.offhand.core.ai.api.HardwareBackend
import kotlinx.coroutines.delay

class FakeAiBackend : AiBackend {

    override suspend fun prewarm() = Unit

    override suspend fun processText(systemPrompt: String, userText: String): AiResult {
        delay(PROCESSING_MS)
        return AiResult(
            text = cannedAnswer(systemPrompt) ?: noteJson(userText),
            processingTimeMs = PROCESSING_MS,
            inputTokens = userText.length / CHARS_PER_TOKEN,
            outputTokens = OUTPUT_TOKENS,
            hardwareBackend = HardwareBackend.CPU,
        )
    }

    private fun cannedAnswer(systemPrompt: String): String? = when {
        systemPrompt.startsWith(STYLE_DRAFT_PROMPT_START) -> STYLE_JSON
        systemPrompt.startsWith(CALENDAR_PROMPT_START) -> CALENDAR_JSON
        else -> null
    }

    private fun noteJson(userText: String): String {
        val sentences = userText
            .split('"')
            .map(String::trim)
            .filter { it.endsWith('.') }
            .distinct()
        val overview = sentences.joinToString("\\n") { "- $it" }
        return """{"title": "$TITLE", "overview": "## Summary\n$overview"}"""
    }

    companion object {
        const val TITLE = "Smoke test note"
        const val STYLE_DRAFT_PROMPT_START = "You will receive a short description, written by a user"
        const val CALENDAR_PROMPT_START = "You will receive a note written from a voice recording. The recording was made on"
        const val CALENDAR_JSON = "<thinking>Two dated items.</thinking>\n" +
            """{"events": [{"title": "Budget review with Anna", "date": "2030-01-15", "time": "10:00", "durationMinutes": 45, "location": "Room 4", "details": "Anna presents the iPad screenshots."}, """ +
            """{"title": "Send quarterly report", "date": "2030-01-20", "time": "", "durationMinutes": 60, "location": "", "details": "The report is due on the 20th."}]}"""
        const val STYLE_JSON = "<thinking>A debrief needs the customer and the follow-ups.</thinking>\n" +
            """{"name": "Sales debrief", "kind": "a sales call debrief", "sections": [""" +
            """{"heading": "Customer", "guidance": "who the customer is and their role", "format": "sentences"}, """ +
            """{"heading": "Next steps", "guidance": "each agreed task with who does it and by when", "format": "bullets"}]}"""
        const val PROCESSING_MS = 300L
        const val CHARS_PER_TOKEN = 4
        const val OUTPUT_TOKENS = 64
    }
}
