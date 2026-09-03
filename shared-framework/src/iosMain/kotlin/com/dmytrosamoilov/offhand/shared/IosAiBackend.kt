package com.dmytrosamoilov.offhand.shared

import com.dmytrosamoilov.offhand.core.ai.api.AiBackend
import com.dmytrosamoilov.offhand.core.ai.api.AiBackendException
import com.dmytrosamoilov.offhand.core.ai.api.AiResult
import com.dmytrosamoilov.offhand.core.ai.api.TokenEstimator
import kotlin.time.TimeSource

class IosAiBackend(
    private val modelManager: IosModelManager,
    private val gemmaEngine: IosGemmaEngine,
) : AiBackend {

    override suspend fun prewarm() {
        modelManager.awaitReadyEngine()
    }

    override suspend fun processText(systemPrompt: String, userText: String): AiResult {
        modelManager.awaitReadyEngine()
        val start = TimeSource.Monotonic.markNow()
        val response = try {
            gemmaEngine.generate(systemPrompt, userText)
        } catch (t: Throwable) {
            throw AiBackendException(t.message ?: "Generation failed", t)
        }
        return AiResult(
            text = response,
            processingTimeMs = start.elapsedNow().inWholeMilliseconds,
            inputTokens = TokenEstimator.approxText(systemPrompt) + TokenEstimator.approxText(userText),
            outputTokens = TokenEstimator.approxText(response),
            hardwareBackend = modelManager.activeBackend.value,
        )
    }
}
