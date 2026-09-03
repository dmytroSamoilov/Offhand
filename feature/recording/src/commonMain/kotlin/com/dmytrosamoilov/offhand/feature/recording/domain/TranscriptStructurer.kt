package com.dmytrosamoilov.offhand.feature.recording.domain

import co.touchlab.kermit.Logger
import com.dmytrosamoilov.offhand.core.ai.api.AiBackend
import com.dmytrosamoilov.offhand.core.ai.api.AiBackendException
import com.dmytrosamoilov.offhand.core.ai.api.AiResult
import com.dmytrosamoilov.offhand.core.ai.api.HardwareBackend
import com.dmytrosamoilov.offhand.core.ai.api.ModelManager
import com.dmytrosamoilov.offhand.core.ai.api.TokenEstimator
import com.dmytrosamoilov.offhand.core.data.domain.NotePreset
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.IsThinkingEnabledUseCase
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

data class StructuredNote(
    val title: String,
    val overview: String,
    val transcript: String,
    val structuringTimeMs: Long,
    val hardwareBackend: HardwareBackend,
)

class TranscriptStructurer(
    private val aiBackend: AiBackend,
    private val modelManager: ModelManager,
    private val isThinkingEnabled: IsThinkingEnabledUseCase,
    private val defaultNoteTitleProvider: DefaultNoteTitleProvider,
) {

    suspend fun structure(
        chunkTranscripts: List<String>,
        preset: NotePreset,
        onProgress: (Float) -> Unit = {},
    ): StructuredNote {
        // Raw Whisper chunks are stored verbatim — only title and overview
        // round-trip through the model.
        val transcript = joinChunks(chunkTranscripts)
        val quoted = chunkTranscripts.joinToString(SEGMENT_SEPARATOR) { chunk ->
            "\"${chunk.replace('"', '\'')}\""
        }
        val promptSet = ModelPromptSet.forFamily(modelManager.model.family)
        val prompt = promptSet.structureNote(preset)
        val segments = splitIntoSegments(quoted, segmentTokenBudget(prompt))
        val pass = structureSegments(segments, prompt, onProgress)
        val merged = mergedOverview(pass.notes, preset)
        val polished = polish(merged, preset, promptSet)
        onProgress(1f)
        val overview = (polished?.overview ?: merged).ifBlank { transcript }
        return StructuredNote(
            title = noteTitle(polished, pass.notes, overview),
            overview = overview,
            transcript = transcript,
            structuringTimeMs = pass.timeMs + (polished?.processingTimeMs ?: 0),
            hardwareBackend = polished?.hardwareBackend ?: pass.backend,
        )
    }

    fun joinChunks(chunkTranscripts: List<String>): String =
        chunkTranscripts.joinToString(PARAGRAPH_SEPARATOR)

    fun transcriptOnly(chunkTranscripts: List<String>): StructuredNote {
        val transcript = joinChunks(chunkTranscripts)
        return StructuredNote(
            title = fallbackTitle(transcript),
            overview = transcript,
            transcript = transcript,
            structuringTimeMs = 0,
            hardwareBackend = HardwareBackend.CPU,
        )
    }

    fun splitStoredTranscript(transcript: String): List<String> =
        transcript.split(PARAGRAPH_SEPARATOR).filter { it.isNotBlank() }

    // The model's context window holds prompt, transcript segment and the
    // generated JSON together, so a segment may only use what those leave over.
    internal fun segmentTokenBudget(prompt: String): Int =
        modelManager.model.maxTokens - TokenEstimator.approxText(prompt) - OUTPUT_TOKEN_RESERVE

    // The polish pass counts as one more step, so per-segment progress only
    // approaches the end instead of reaching it.
    private suspend fun structureSegments(
        segments: List<String>,
        prompt: String,
        onProgress: (Float) -> Unit,
    ): SegmentPass {
        var timeMs = 0L
        var backend = HardwareBackend.CPU
        val totalSteps = segments.size + 1
        val notes = segments.mapIndexed { index, segment ->
            val result = aiBackend.processText(prompt, segment)
            timeMs += result.processingTimeMs
            backend = result.hardwareBackend
            onProgress((index + 1) / totalSteps.toFloat())
            parseNoteJson(result.text)
        }
        return SegmentPass(notes, timeMs, backend)
    }

    // A failed polish keeps the merged overview — a note assembled from valid
    // segment passes must never degrade because the extra pass misbehaved.
    private suspend fun polish(
        merged: String,
        preset: NotePreset,
        promptSet: ModelPromptSet,
    ): PolishedNote? {
        val thinkingEnabled = isThinkingEnabled()
        val prompt = promptSet.polishNote(preset, thinkingEnabled)
        if (merged.isBlank() ||
            TokenEstimator.approxText(merged) > polishTokenBudget(prompt, thinkingEnabled)
        ) {
            return null
        }
        val result = polishSafely(prompt, merged.replace('"', '\'')) ?: return null
        val note = parseNoteJson(result.text)
        val overview = normalizeOverview(listOf(note.overview), preset)
        if (overview.length < merged.length * MIN_POLISH_RETAIN) return null
        return PolishedNote(note.title, overview, result.processingTimeMs, result.hardwareBackend)
    }

    private suspend fun polishSafely(prompt: String, draft: String): AiResult? = try {
        aiBackend.processText(prompt, draft)
    } catch (backendFailure: AiBackendException) {
        Logger.withTag(LOG_TAG).w(backendFailure) { "Polish pass failed, keeping the merged note" }
        null
    }

    // Polishing regenerates the whole note, so the context window must hold
    // the prompt plus the note twice — once as input and once as output —
    // and, when thinking is enabled, the reasoning the model writes first.
    internal fun polishTokenBudget(prompt: String, thinkingEnabled: Boolean): Int {
        val thinkingReserve = if (thinkingEnabled) POLISH_THINKING_RESERVE else 0
        val reserved = TokenEstimator.approxText(prompt) + POLISH_TOKEN_SLACK + thinkingReserve
        return (modelManager.model.maxTokens - reserved) / 2
    }

    private fun noteTitle(
        polished: PolishedNote?,
        parts: List<ParsedNote>,
        overview: String,
    ): String = polished?.title?.ifBlank { null }
        ?: parts.firstNotNullOfOrNull { it.title.ifBlank { null } }
        ?: fallbackTitle(overview)

    private fun mergedOverview(parts: List<ParsedNote>, preset: NotePreset): String =
        normalizeOverview(parts.mapNotNull { it.overview.ifBlank { null } }, preset)

    private fun normalizeOverview(overviews: List<String>, preset: NotePreset): String {
        val sections = NotePresetPrompt.sections(preset)
        return if (sections.isEmpty()) {
            NoteProseFormatter.format(overviews)
        } else {
            NoteSectionMerger.merge(overviews, sections)
        }
    }

    private fun parseNoteJson(raw: String): ParsedNote {
        val cleaned = ModelResponseCleaner.stripThinking(raw)
        val json = extractJsonObject(cleaned)
            ?: return ParsedNote(title = "", overview = scrubJsonArtifacts(cleaned))
        decodeNoteJson(json)?.let { return it }
        extractFieldsByRegex(json)?.let { return it }
        return ParsedNote(title = "", overview = scrubJsonArtifacts(cleaned))
    }

    private fun decodeNoteJson(json: String): ParsedNote? = try {
        val note = lenientJson.decodeFromString<NoteJson>(escapeNewlinesInStrings(json))
        ParsedNote(title = sanitizeTitle(note.title), overview = note.overview.trim())
    } catch (serialization: Exception) {
        null
    }

    private fun extractFieldsByRegex(json: String): ParsedNote? {
        val title = TITLE_FIELD.find(json)?.groupValues?.get(1)
        val overview = OVERVIEW_FIELD.find(json)?.groupValues?.get(1)?.let(::trimJsonTail)
        if (title == null && overview == null) return null
        return ParsedNote(
            title = sanitizeTitle(unescapeJsonValue(title.orEmpty())),
            overview = unescapeJsonValue(overview.orEmpty()).trim(),
        )
    }

    // Models regularly drop the closing quote of the last value, so the overview
    // is read to the end of the object and its JSON tail removed here. Without
    // this the whole overview is lost and the note falls back to the transcript.
    private fun trimJsonTail(value: String): String = value.replace(JSON_VALUE_TAIL, "")

    // Models often emit raw line breaks inside JSON string values, which is
    // invalid JSON — escape them so decoding still succeeds.
    private fun escapeNewlinesInStrings(json: String): String {
        val result = StringBuilder(json.length)
        var inString = false
        var index = 0
        while (index < json.length) {
            val char = json[index]
            when {
                char == '\\' && index + 1 < json.length -> {
                    result.append(char).append(json[index + 1])
                    index++
                }
                char == '"' -> {
                    inString = !inString
                    result.append(char)
                }
                inString && char == '\n' -> result.append("\\n")
                inString && char == '\r' -> Unit
                else -> result.append(char)
            }
            index++
        }
        return result.toString()
    }

    private fun unescapeJsonValue(value: String): String = value
        .replace("\\n", "\n")
        .replace("\\\"", "\"")
        .replace("\\\\", "\\")

    private fun scrubJsonArtifacts(text: String): String = text
        .replace(JSON_SCAFFOLDING, "")
        .trim()

    private fun extractJsonObject(text: String): String? {
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        if (start < 0 || end <= start) return null
        return text.substring(start, end + 1)
    }

    internal fun splitIntoSegments(transcript: String, tokenBudget: Int): List<String> {
        if (TokenEstimator.approxText(transcript) <= tokenBudget) return listOf(transcript)

        val segments = mutableListOf<String>()
        val current = StringBuilder()
        var currentTokens = 0
        transcript.split(PARAGRAPH_SEPARATOR)
            .flatMap { paragraph -> splitOversizedParagraph(paragraph, tokenBudget) }
            .forEach { paragraph ->
                val paragraphTokens = TokenEstimator.approxText(paragraph)
                if (current.isNotEmpty() && currentTokens + paragraphTokens > tokenBudget) {
                    segments += current.toString()
                    current.clear()
                    currentTokens = 0
                }
                if (current.isNotEmpty()) current.append(PARAGRAPH_SEPARATOR)
                current.append(paragraph)
                currentTokens += paragraphTokens
            }
        if (current.isNotEmpty()) segments += current.toString()
        return segments
    }

    private fun splitOversizedParagraph(paragraph: String, tokenBudget: Int): List<String> {
        if (TokenEstimator.approxText(paragraph) <= tokenBudget) return listOf(paragraph)
        return paragraph.chunked(tokenBudget * OVERSIZED_PARAGRAPH_CHARS_PER_TOKEN)
    }

    private fun sanitizeTitle(raw: String): String = raw
        .replace(JSON_SCAFFOLDING, "")
        .trim()
        .trimStart('#', ' ')
        .trim('"', '\'', '*', '{', '}', ':', ',')
        .trim()
        .take(MAX_TITLE_CHARS)

    private fun fallbackTitle(body: String): String {
        val untitled = defaultNoteTitleProvider.untitledTitle()
        val firstLine = body.lineSequence().firstOrNull { it.isNotBlank() } ?: return untitled
        val words = firstLine
            .replace(MARKDOWN_CHARS, " ")
            .split(WHITESPACE)
            .filter { it.isNotBlank() }
            .take(TITLE_MAX_WORDS)
        return words.joinToString(" ").ifBlank { untitled }
    }

    private data class ParsedNote(
        val title: String,
        val overview: String,
    )

    private data class SegmentPass(
        val notes: List<ParsedNote>,
        val timeMs: Long,
        val backend: HardwareBackend,
    )

    private data class PolishedNote(
        val title: String,
        val overview: String,
        val processingTimeMs: Long,
        val hardwareBackend: HardwareBackend,
    )

    @Serializable
    private data class NoteJson(
        val title: String = "",
        val overview: String = "",
    )

    private companion object {
        const val LOG_TAG = "TranscriptStructurer"
        // Keeps ~1000-1500 tokens of the context window free for the title
        // and overview the model generates, with slack for estimator error.
        const val OUTPUT_TOKEN_RESERVE = 1_250
        // Slack for the JSON scaffolding, the title and estimator error on
        // top of the symmetric input/output split of the polish budget.
        const val POLISH_TOKEN_SLACK = 250
        // Thinking is enabled for the polish pass, so the window must also
        // hold the reasoning the model emits before the JSON.
        const val POLISH_THINKING_RESERVE = 500
        // A polished note far shorter than the draft means the model
        // truncated or summarised it away instead of deduplicating it.
        const val MIN_POLISH_RETAIN = 0.3f
        const val OVERSIZED_PARAGRAPH_CHARS_PER_TOKEN = 2
        const val MAX_TITLE_CHARS = 80
        const val TITLE_MAX_WORDS = 8
        const val PARAGRAPH_SEPARATOR = "\n\n"
        const val SEGMENT_SEPARATOR = ",\n\n"
        val MARKDOWN_CHARS = Regex("[#*>`_\\-]")
        val WHITESPACE = Regex("\\s+")
        val TITLE_FIELD = Regex("\"title\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"")
        val OVERVIEW_FIELD = Regex("\"overview\"\\s*:\\s*\"(.*)", RegexOption.DOT_MATCHES_ALL)
        val JSON_VALUE_TAIL = Regex("[\"},\\s]+$")
        val JSON_SCAFFOLDING = Regex("```(?:json)?|\"(?:title|overview)\"\\s*:|[{}]")
        val lenientJson = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }
    }
}
