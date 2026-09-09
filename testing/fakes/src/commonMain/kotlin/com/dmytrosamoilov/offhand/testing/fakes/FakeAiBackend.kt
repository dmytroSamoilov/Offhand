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
            text = if (systemPrompt.startsWith(STYLE_DRAFT_PROMPT_START)) STYLE_JSON else noteJson(userText),
            processingTimeMs = PROCESSING_MS,
            inputTokens = userText.length / CHARS_PER_TOKEN,
            outputTokens = OUTPUT_TOKENS,
            hardwareBackend = HardwareBackend.CPU,
        )
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
        const val STYLE_JSON = "<thinking>A debrief needs the customer and the follow-ups.</thinking>\n" +
            """{"name": "Sales debrief", "kind": "a sales call debrief", "sections": [""" +
            """{"heading": "Customer", "guidance": "who the customer is and their role", "format": "sentences"}, """ +
            """{"heading": "Next steps", "guidance": "each agreed task with who does it and by when", "format": "bullets"}]}"""
        const val PROCESSING_MS = 300L
        const val CHARS_PER_TOKEN = 4
        const val OUTPUT_TOKENS = 64
    }
}
