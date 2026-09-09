package com.dmytrosamoilov.offhand.feature.recording.domain

import com.dmytrosamoilov.offhand.core.data.domain.NoteStyleDraft
import com.dmytrosamoilov.offhand.core.data.domain.NoteStyleLimits
import com.dmytrosamoilov.offhand.core.data.domain.NoteStyleSection
import com.dmytrosamoilov.offhand.core.data.domain.SectionFormat
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

internal object NoteStyleDraftParser {

    fun parse(raw: String): NoteStyleDraft? {
        val json = extractJsonObject(ModelResponseCleaner.stripThinking(raw)) ?: return null
        val draft = runCatching { lenientJson.decodeFromString<DraftJson>(json) }.getOrNull() ?: return null
        val sections = draft.sections
            .map { it.toSection() }
            .filter { it.heading.isNotBlank() }
            .take(NoteStyleLimits.MAX_SECTIONS)
        if (sections.isEmpty()) return null
        return NoteStyleDraft(
            name = draft.name.clean(NoteStyleLimits.MAX_NAME_LENGTH),
            noteKind = draft.kind.clean(NoteStyleLimits.MAX_KIND_LENGTH),
            sections = sections,
        )
    }

    private fun SectionJson.toSection(): NoteStyleSection = NoteStyleSection(
        heading = heading.clean(NoteStyleLimits.MAX_HEADING_LENGTH).trimStart('#', ' '),
        guidance = guidance.clean(NoteStyleLimits.MAX_GUIDANCE_LENGTH),
        format = if (format.trim().lowercase().startsWith(BULLETS_PREFIX)) SectionFormat.BULLETS else SectionFormat.SENTENCES,
    )

    private fun String.clean(maxLength: Int): String =
        replace(WHITESPACE, " ").trim().trimEnd('.').take(maxLength).trim()

    private fun extractJsonObject(text: String): String? {
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        if (start < 0 || end <= start) return null
        return text.substring(start, end + 1)
    }

    @Serializable
    private data class DraftJson(
        val name: String = "",
        val kind: String = "",
        val sections: List<SectionJson> = emptyList(),
    )

    @Serializable
    private data class SectionJson(
        val heading: String = "",
        val guidance: String = "",
        val format: String = "",
    )

    private const val BULLETS_PREFIX = "bullet"
    private val WHITESPACE = Regex("\\s+")
    private val lenientJson = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
}
