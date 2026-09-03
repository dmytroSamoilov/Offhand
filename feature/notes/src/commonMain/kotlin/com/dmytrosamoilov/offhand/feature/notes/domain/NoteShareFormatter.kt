@file:OptIn(ExperimentalTime::class)

package com.dmytrosamoilov.offhand.feature.notes.domain

import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime

data class NoteShareLabels(
    val title: String,
    val date: String,
    val overview: String,
    val transcript: String,
)

object NoteShareFormatter {

    private val ILLEGAL_FILENAME_CHARS = Regex("[\\\\/:*?\"<>|\\u0000-\\u001f\\u007f]")
    private val WHITESPACE_RUNS = Regex("\\s+")
    private const val MAX_TITLE_CHARS = 60

    fun fileBaseName(
        title: String,
        fallbackTitle: String,
        createdAtEpochMs: Long,
        zone: TimeZone,
    ): String {
        val sanitizedTitle = title
            .replace(ILLEGAL_FILENAME_CHARS, " ")
            .replace(WHITESPACE_RUNS, " ")
            .trim()
            .take(MAX_TITLE_CHARS)
            .trim()
            .ifEmpty { fallbackTitle }
        val timestamp = fileTimestamp(atZone(createdAtEpochMs, zone))
        return "$sanitizedTitle $timestamp"
    }

    fun textContent(
        labels: NoteShareLabels,
        title: String,
        formattedDate: String,
        overview: String,
        transcript: String,
    ): String = buildString {
        appendLine("${labels.title}: $title")
        appendLine("${labels.date}: $formattedDate")
        appendLine()
        appendLine("${labels.overview}:")
        appendLine(overview.trim())
        appendLine()
        appendLine("${labels.transcript}:")
        appendLine(transcript.trim())
    }

    private fun fileTimestamp(dateTime: LocalDateTime): String {
        val year = dateTime.year.toString().padStart(YEAR_DIGITS, '0')
        val month = dateTime.month.number.toString().padStart(TWO_DIGITS, '0')
        val day = dateTime.day.toString().padStart(TWO_DIGITS, '0')
        val hour = dateTime.hour.toString().padStart(TWO_DIGITS, '0')
        val minute = dateTime.minute.toString().padStart(TWO_DIGITS, '0')
        return "$year-$month-$day $hour-$minute"
    }

    private fun atZone(epochMs: Long, zone: TimeZone): LocalDateTime =
        Instant.fromEpochMilliseconds(epochMs).toLocalDateTime(zone)

    private const val TWO_DIGITS = 2
    private const val YEAR_DIGITS = 4
}
