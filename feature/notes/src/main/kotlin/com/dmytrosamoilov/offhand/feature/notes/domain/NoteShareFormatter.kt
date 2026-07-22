package com.dmytrosamoilov.offhand.feature.notes.domain

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class NoteShareLabels(
    val title: String,
    val date: String,
    val overview: String,
    val transcript: String,
)

object NoteShareFormatter {

    private val FILE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH-mm", Locale.US)
    private val CONTENT_DATE_FORMATTER =
        DateTimeFormatter.ofPattern("MMM d, yyyy · HH:mm", Locale.US)
    private val ILLEGAL_FILENAME_CHARS = Regex("[\\\\/:*?\"<>|\\p{Cntrl}]")
    private val WHITESPACE_RUNS = Regex("\\s+")
    private const val MAX_TITLE_CHARS = 60

    fun fileBaseName(
        title: String,
        fallbackTitle: String,
        createdAtEpochMs: Long,
        zone: ZoneId,
    ): String {
        val sanitizedTitle = title
            .replace(ILLEGAL_FILENAME_CHARS, " ")
            .replace(WHITESPACE_RUNS, " ")
            .trim()
            .take(MAX_TITLE_CHARS)
            .trim()
            .ifEmpty { fallbackTitle }
        val timestamp = FILE_DATE_FORMATTER.format(atZone(createdAtEpochMs, zone))
        return "$sanitizedTitle $timestamp"
    }

    fun textContent(
        labels: NoteShareLabels,
        title: String,
        createdAtEpochMs: Long,
        overview: String,
        transcript: String,
        zone: ZoneId,
    ): String = buildString {
        appendLine("${labels.title}: $title")
        appendLine("${labels.date}: ${CONTENT_DATE_FORMATTER.format(atZone(createdAtEpochMs, zone))}")
        appendLine()
        appendLine("${labels.overview}:")
        appendLine(overview.trim())
        appendLine()
        appendLine("${labels.transcript}:")
        appendLine(transcript.trim())
    }

    private fun atZone(epochMs: Long, zone: ZoneId) = Instant.ofEpochMilli(epochMs).atZone(zone)
}
