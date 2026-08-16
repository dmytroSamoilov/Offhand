package com.dmytrosamoilov.offhand.feature.recording.domain

import com.dmytrosamoilov.offhand.core.ai.api.AiBackend
import com.dmytrosamoilov.offhand.core.ai.api.HardwareBackend
import com.dmytrosamoilov.offhand.core.ai.api.ModelManager
import com.dmytrosamoilov.offhand.core.ai.api.TokenEstimator
import com.dmytrosamoilov.offhand.core.data.domain.NotePreset
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
        var totalTimeMs = 0L
        var backend = HardwareBackend.CPU
        val prompt = ModelPromptSet.forFamily(modelManager.model.family).structureNote(preset)
        val segments = splitIntoSegments(quoted, segmentTokenBudget(prompt))
        val parts = segments.mapIndexed { index, segment ->
            val result = aiBackend.processText(prompt, segment)
            totalTimeMs += result.processingTimeMs
            backend = result.hardwareBackend
            onProgress((index + 1) / segments.size.toFloat())
            parseNoteJson(result.text)
        }
        val overview = combinedOverview(parts, transcript, preset)
        return StructuredNote(
            title = combinedTitle(parts, overview),
            overview = overview,
            transcript = transcript,
            structuringTimeMs = totalTimeMs,
            hardwareBackend = backend,
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

    private fun combinedTitle(parts: List<ParsedNote>, overview: String): String =
        parts.firstNotNullOfOrNull { it.title.ifBlank { null } } ?: fallbackTitle(overview)

    private fun combinedOverview(
        parts: List<ParsedNote>,
        transcript: String,
        preset: NotePreset,
    ): String {
        val overviews = parts.mapNotNull { it.overview.ifBlank { null } }
        val sections = NotePresetPrompt.sections(preset)
        val combined = if (sections.isEmpty()) {
            NoteProseFormatter.format(overviews)
        } else {
            NoteSectionMerger.merge(overviews, sections)
        }
        return combined.ifBlank { transcript }
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
        val firstLine = body.lineSequence().firstOrNull { it.isNotBlank() } ?: return DEFAULT_TITLE
        val words = firstLine
            .replace(MARKDOWN_CHARS, " ")
            .split(WHITESPACE)
            .filter { it.isNotBlank() }
            .take(TITLE_MAX_WORDS)
        return words.joinToString(" ").ifBlank { DEFAULT_TITLE }
    }

    private data class ParsedNote(
        val title: String,
        val overview: String,
    )

    @Serializable
    private data class NoteJson(
        val title: String = "",
        val overview: String = "",
    )

    private companion object {
        // Keeps ~1000-1500 tokens of the context window free for the title
        // and overview the model generates, with slack for estimator error.
        const val OUTPUT_TOKEN_RESERVE = 1_250
        const val OVERSIZED_PARAGRAPH_CHARS_PER_TOKEN = 2
        const val MAX_TITLE_CHARS = 80
        const val TITLE_MAX_WORDS = 8
        const val DEFAULT_TITLE = "Voice note"
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
