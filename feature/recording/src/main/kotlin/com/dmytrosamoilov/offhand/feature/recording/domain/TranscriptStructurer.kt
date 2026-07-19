package com.dmytrosamoilov.offhand.feature.recording.domain

import com.dmytrosamoilov.offhand.core.ai.api.AiBackend
import com.dmytrosamoilov.offhand.core.ai.api.HardwareBackend
import com.dmytrosamoilov.offhand.core.ai.api.TokenEstimator
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

data class StructuredNote(
    val title: String,
    val overview: String,
    val transcript: String,
    val structuringTimeMs: Long,
    val hardwareBackend: HardwareBackend,
)

@Singleton
class TranscriptStructurer @Inject constructor(
    private val aiBackend: AiBackend,
) {

    suspend fun structure(
        chunkTranscripts: List<String>,
        onProgress: (Float) -> Unit = {},
    ): StructuredNote {
        // Chunks arrive already proofread and are stored as-is — only title
        // and overview round-trip through the model, so output stays short.
        val transcript = chunkTranscripts.joinToString(PARAGRAPH_SEPARATOR)
        val quoted = chunkTranscripts.joinToString(SEGMENT_SEPARATOR) { chunk ->
            "\"${chunk.replace('"', '\'')}\""
        }
        var totalTimeMs = 0L
        var backend = HardwareBackend.CPU
        val segments = splitIntoSegments(quoted)
        val parts = segments.mapIndexed { index, segment ->
            val result = aiBackend.processText(RecordingPrompts.STRUCTURE_NOTE_JSON, segment)
            totalTimeMs += result.processingTimeMs
            backend = result.hardwareBackend
            onProgress((index + 1) / segments.size.toFloat())
            parseNoteJson(result.text)
        }
        val overview = combinedOverview(parts, transcript)
        return StructuredNote(
            title = combinedTitle(parts, overview),
            overview = overview,
            transcript = transcript,
            structuringTimeMs = totalTimeMs,
            hardwareBackend = backend,
        )
    }

    private fun combinedTitle(parts: List<ParsedNote>, overview: String): String =
        parts.firstNotNullOfOrNull { it.title.ifBlank { null } } ?: fallbackTitle(overview)

    private fun combinedOverview(parts: List<ParsedNote>, transcript: String): String =
        parts.mapNotNull { it.overview.ifBlank { null } }
            .joinToString(PARAGRAPH_SEPARATOR)
            .ifBlank { transcript }

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
        val overview = OVERVIEW_FIELD.find(json)?.groupValues?.get(1)
        if (title == null && overview == null) return null
        return ParsedNote(
            title = sanitizeTitle(unescapeJsonValue(title.orEmpty())),
            overview = unescapeJsonValue(overview.orEmpty()).trim(),
        )
    }

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

    internal fun splitIntoSegments(transcript: String): List<String> {
        if (TokenEstimator.approxText(transcript) <= SEGMENT_TOKEN_BUDGET) return listOf(transcript)

        val segments = mutableListOf<String>()
        val current = StringBuilder()
        var currentTokens = 0
        transcript.split(PARAGRAPH_SEPARATOR)
            .flatMap(::splitOversizedParagraph)
            .forEach { paragraph ->
                val paragraphTokens = TokenEstimator.approxText(paragraph)
                if (current.isNotEmpty() && currentTokens + paragraphTokens > SEGMENT_TOKEN_BUDGET) {
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

    private fun splitOversizedParagraph(paragraph: String): List<String> {
        if (TokenEstimator.approxText(paragraph) <= SEGMENT_TOKEN_BUDGET) return listOf(paragraph)
        return paragraph.chunked(OVERSIZED_PARAGRAPH_CHUNK_CHARS)
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
        // Sized so a segment + prompt + thinking output fits the smallest
        // catalog context window (4096) and keeps calls short enough that
        // small models don't drift into repetition loops.
        const val SEGMENT_TOKEN_BUDGET = 2_500
        const val OVERSIZED_PARAGRAPH_CHUNK_CHARS = SEGMENT_TOKEN_BUDGET * 2
        const val MAX_TITLE_CHARS = 80
        const val TITLE_MAX_WORDS = 8
        const val DEFAULT_TITLE = "Voice note"
        const val PARAGRAPH_SEPARATOR = "\n\n"
        const val SEGMENT_SEPARATOR = ",\n\n"
        val MARKDOWN_CHARS = Regex("[#*>`_\\-]")
        val WHITESPACE = Regex("\\s+")
        val TITLE_FIELD = Regex("\"title\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"")
        val OVERVIEW_FIELD = Regex("\"overview\"\\s*:\\s*\"(.*)\"", RegexOption.DOT_MATCHES_ALL)
        val JSON_SCAFFOLDING = Regex("```(?:json)?|\"(?:title|overview)\"\\s*:|[{}]")
        val lenientJson = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }
    }
}
