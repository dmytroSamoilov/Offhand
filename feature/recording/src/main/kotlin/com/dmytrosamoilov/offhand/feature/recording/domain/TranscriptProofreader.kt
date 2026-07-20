package com.dmytrosamoilov.offhand.feature.recording.domain

import com.dmytrosamoilov.offhand.core.ai.api.AiBackend
import com.dmytrosamoilov.offhand.core.ai.api.AiBackendException
import com.dmytrosamoilov.offhand.core.ai.api.ModelManager
import com.dmytrosamoilov.offhand.core.ai.api.TokenEstimator
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

data class ProofreadTranscript(
    val chunks: List<String>,
    val processingTimeMs: Long,
)

@Singleton
class TranscriptProofreader @Inject constructor(
    private val aiBackend: AiBackend,
    private val modelManager: ModelManager,
) {

    suspend fun proofread(
        chunkTranscripts: List<String>,
        onProgress: (Float) -> Unit = {},
    ): ProofreadTranscript {
        var totalTimeMs = 0L
        val chunks = chunkTranscripts.mapIndexed { index, chunk ->
            val cleaned = proofreadChunk(chunk)
            totalTimeMs += cleaned.timeMs
            onProgress((index + 1) / chunkTranscripts.size.toFloat())
            cleaned.text
        }
        return ProofreadTranscript(chunks = chunks, processingTimeMs = totalTimeMs)
    }

    private suspend fun proofreadChunk(chunk: String): CleanedChunk {
        if (TokenEstimator.approxText(chunk) > CHUNK_TOKEN_BUDGET) return CleanedChunk(chunk, 0)
        val prompt = ModelPromptSet.forFamily(modelManager.model.family).proofreadTranscript
        val result = try {
            aiBackend.processText(prompt, chunk)
        } catch (backendFailure: AiBackendException) {
            Timber.tag(LOG_TAG).w(backendFailure, "Proofreading call failed, keeping raw chunk")
            return CleanedChunk(chunk, 0)
        }
        val cleaned = ModelResponseCleaner.stripThinking(result.text)
        return CleanedChunk(
            text = if (isFaithful(chunk, cleaned)) cleaned else keepRawChunk(chunk),
            timeMs = result.processingTimeMs,
        )
    }

    private fun keepRawChunk(chunk: String): String {
        Timber.tag(LOG_TAG).i("Proofread output drifted from the raw chunk, keeping raw")
        return chunk
    }

    internal fun isFaithful(original: String, cleaned: String): Boolean {
        if (cleaned.isBlank()) return false
        val originalWords = normalizedWords(original)
        val cleanedWords = normalizedWords(cleaned)
        if (originalWords.isEmpty()) return false
        val changedWords = wordEditDistance(originalWords, cleanedWords)
        return changedWords.toFloat() / originalWords.size <= MAX_CHANGED_WORD_RATIO
    }

    private fun normalizedWords(text: String): List<String> =
        text.lowercase().split(NON_WORD_CHARS).filter { it.isNotBlank() }

    private fun wordEditDistance(source: List<String>, target: List<String>): Int {
        var previous = IntArray(target.size + 1) { it }
        var current = IntArray(target.size + 1)
        source.forEachIndexed { sourceIndex, sourceWord ->
            current[0] = sourceIndex + 1
            target.forEachIndexed { targetIndex, targetWord ->
                val substitution = previous[targetIndex] + if (sourceWord == targetWord) 0 else 1
                current[targetIndex + 1] = minOf(
                    substitution,
                    previous[targetIndex + 1] + 1,
                    current[targetIndex] + 1,
                )
            }
            val finished = previous
            previous = current
            current = finished
        }
        return previous[target.size]
    }

    private data class CleanedChunk(
        val text: String,
        val timeMs: Long,
    )

    private companion object {
        const val LOG_TAG = "TranscriptProofreader"
        // Output length matches input, so a chunk plus its rewrite plus the
        // prompt must fit the smallest catalog context window (4096).
        const val CHUNK_TOKEN_BUDGET = 1_500
        const val MAX_CHANGED_WORD_RATIO = 0.15f
        val NON_WORD_CHARS = Regex("[^\\p{L}\\p{N}]+")
    }
}
