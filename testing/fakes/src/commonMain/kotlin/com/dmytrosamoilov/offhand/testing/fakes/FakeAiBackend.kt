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
            text = noteJson(userText),
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
        const val PROCESSING_MS = 300L
        const val CHARS_PER_TOKEN = 4
        const val OUTPUT_TOKENS = 64
    }
}
