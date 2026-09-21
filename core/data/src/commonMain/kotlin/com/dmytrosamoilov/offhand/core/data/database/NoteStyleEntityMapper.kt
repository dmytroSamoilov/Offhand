package com.dmytrosamoilov.offhand.core.data.database

import com.dmytrosamoilov.offhand.core.data.domain.CustomNoteStyle
import com.dmytrosamoilov.offhand.core.data.domain.NoteStyleLanguage
import com.dmytrosamoilov.offhand.core.data.domain.NoteStyleSection
import com.dmytrosamoilov.offhand.core.data.domain.SectionFormat
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class SectionJson(
    val heading: String,
    val guidance: String,
    val format: String,
)

private val json = Json { ignoreUnknownKeys = true }

internal fun NoteStyleEntity.toDomain(): CustomNoteStyle = CustomNoteStyle(
    id = id,
    name = name,
    noteKind = noteKind,
    language = NoteStyleLanguage.fromName(language),
    sections = decodeSections(sectionsJson),
    createdAtEpochMs = createdAtEpochMs,
)

internal fun CustomNoteStyle.toEntity(): NoteStyleEntity = NoteStyleEntity(
    id = id,
    name = name,
    noteKind = noteKind,
    language = language.name,
    sectionsJson = encodeSections(sections),
    createdAtEpochMs = createdAtEpochMs,
)

private fun decodeSections(raw: String): List<NoteStyleSection> =
    runCatching { json.decodeFromString<List<SectionJson>>(raw) }
        .getOrDefault(emptyList())
        .map { NoteStyleSection(it.heading, it.guidance, SectionFormat.fromName(it.format)) }

private fun encodeSections(sections: List<NoteStyleSection>): String =
    json.encodeToString(sections.map { SectionJson(it.heading, it.guidance, it.format.name) })
