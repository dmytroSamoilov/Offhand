package com.dmytrosamoilov.offhand.core.ai.local

import com.dmytrosamoilov.offhand.core.ai.api.AiBackend
import com.dmytrosamoilov.offhand.core.ai.api.AiBackendException
import com.dmytrosamoilov.offhand.core.ai.api.AiResult
import com.dmytrosamoilov.offhand.core.ai.api.ModelState
import com.dmytrosamoilov.offhand.core.ai.api.TokenEstimator
import com.google.ai.edge.litertlm.Content
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

@Singleton
class LocalAiBackend @Inject constructor(
    private val manager: LiteRtLmManager,
) : AiBackend {

    override suspend fun prewarm() {
        try {
            manager.ensureModelAvailable()
        } catch (t: Throwable) {
            GenAiLog.warn("prewarm-error", t)
        }
    }

    override suspend fun processText(systemPrompt: String, userText: String): AiResult {
        awaitModel()
        val prompt = buildString {
            appendLine(systemPrompt)
            appendLine()
            append(userText)
        }
        GenAiLog.logModelOutput("local-text-prompt", "SYSTEM:\n$systemPrompt\n\nUSER:\n$userText")
        return run(
            tag = "local-text",
            content = listOf(Content.Text(prompt)),
            inputTokens = TokenEstimator.approxText(prompt),
        )
    }

    private suspend fun run(
        tag: String,
        content: List<Content>,
        inputTokens: Int,
    ): AiResult = try {
        val started = System.currentTimeMillis()
        val text = manager.generate(content)
        val elapsed = System.currentTimeMillis() - started
        val outputTokens = TokenEstimator.approxText(text)
        GenAiLog.logModelOutput("$tag-output", text)
        GenAiLog.logModelOutput(
            "$tag-done",
            "tokens: in=~$inputTokens out=~$outputTokens · ms=$elapsed · chars=${text.length}",
        )
        AiResult(
            text = text,
            processingTimeMs = elapsed,
            inputTokens = inputTokens,
            outputTokens = outputTokens,
            hardwareBackend = manager.activeBackend.value,
        )
    } catch (t: Throwable) {
        throw AiBackendException(t.message ?: "On-device inference failed", t)
    }

    private suspend fun awaitModel() {
        if (manager.modelState.value is ModelState.Ready) return
        manager.ensureModelAvailable()
        val terminal = manager.modelState.first { it is ModelState.Ready || it is ModelState.Error }
        if (terminal is ModelState.Error) {
            throw AiBackendException("Model not ready: ${terminal.message}")
        }
    }
}
