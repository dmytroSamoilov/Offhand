@file:OptIn(ExperimentalTime::class)

package com.dmytrosamoilov.offhand.feature.recording.domain

import com.dmytrosamoilov.offhand.core.data.domain.CalendarEventSuggestion
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.time.ExperimentalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

internal object CalendarSuggestionParser {

    fun parse(raw: String, timeZone: TimeZone): List<CalendarEventSuggestion> {
        val cleaned = ModelResponseCleaner.stripThinking(raw)
        return decodeEvents(cleaned)
            .mapNotNull { it.toSuggestion(timeZone) }
            .distinctBy { it.title.lowercase() to it.startEpochMs }
            .sortedBy { it.startEpochMs }
            .take(CalendarSuggestionPrompt.MAX_EVENTS)
    }

    // Models copy the weekday from the prompt into the date or wrap the list
    // differently, so dates and times are searched for rather than parsed strictly.
    private fun decodeEvents(cleaned: String): List<EventJson> {
        val objectStart = cleaned.indexOf('{')
        val arrayStart = cleaned.indexOf('[')
        val startsWithArray = arrayStart in 0 until objectStart.coerceAtLeast(0) || (objectStart < 0 && arrayStart >= 0)
        if (startsWithArray) return decodeArray(cleaned)
        return extractJson(cleaned, '{', '}')
            ?.let { json -> runCatching { lenientJson.decodeFromString<EventsJson>(json) }.getOrNull()?.events }
            ?: decodeEachObject(cleaned)
    }

    // An answer cut off by the output limit still holds complete event objects
    // before the truncation point.
    private fun decodeEachObject(cleaned: String): List<EventJson> = EVENT_OBJECT.findAll(cleaned)
        .mapNotNull { match -> runCatching { lenientJson.decodeFromString<EventJson>(match.value) }.getOrNull() }
        .filter { it.date.isNotBlank() }
        .toList()

    private fun decodeArray(cleaned: String): List<EventJson> = extractJson(cleaned, '[', ']')
        ?.let { json -> runCatching { lenientJson.decodeFromString<List<EventJson>>(json) }.getOrNull() }
        .orEmpty()

    private fun EventJson.toSuggestion(timeZone: TimeZone): CalendarEventSuggestion? {
        val cleanTitle = title.clean(MAX_TITLE_LENGTH).ifBlank { return null }
        val dateText = DATE_PATTERN.find(date)?.value ?: return null
        val day = runCatching { LocalDate.parse(dateText) }.getOrNull() ?: return null
        val startTime = parseTime(time)
        val start = day.atTime(startTime ?: LocalTime(0, 0)).toInstant(timeZone)
        val end = if (startTime == null) start + 1.days else start + durationMinutes.coerceIn(MIN_DURATION_MINUTES, MAX_DURATION_MINUTES).minutes
        return CalendarEventSuggestion(
            title = cleanTitle,
            startEpochMs = start.toEpochMilliseconds(),
            endEpochMs = end.toEpochMilliseconds(),
            isAllDay = startTime == null,
            location = location.clean(MAX_LOCATION_LENGTH),
            details = details.clean(MAX_DETAILS_LENGTH),
        )
    }

    private fun parseTime(raw: String): LocalTime? {
        val match = TIME_PATTERN.find(raw) ?: return null
        val hour = match.groupValues[1].toInt()
        val minute = match.groupValues[2].toInt()
        if (hour !in 0..23 || minute !in 0..59) return null
        return LocalTime(hour, minute)
    }

    private fun String.clean(maxLength: Int): String = replace(WHITESPACE, " ").trim().take(maxLength).trim()

    private fun extractJson(text: String, open: Char, close: Char): String? {
        val start = text.indexOf(open)
        val end = text.lastIndexOf(close)
        if (start < 0 || end <= start) return null
        return text.substring(start, end + 1)
    }

    @Serializable
    private data class EventsJson(
        val events: List<EventJson> = emptyList(),
    )

    @Serializable
    private data class EventJson(
        val title: String = "",
        val date: String = "",
        val time: String = "",
        val durationMinutes: Int = DEFAULT_DURATION_MINUTES,
        val location: String = "",
        val details: String = "",
    )

    private const val DEFAULT_DURATION_MINUTES = 60
    private const val MIN_DURATION_MINUTES = 5
    private const val MAX_DURATION_MINUTES = 24 * 60
    private const val MAX_TITLE_LENGTH = 80
    private const val MAX_LOCATION_LENGTH = 120
    private const val MAX_DETAILS_LENGTH = 300
    private val DATE_PATTERN = Regex("\\d{4}-\\d{2}-\\d{2}")
    private val EVENT_OBJECT = Regex("\\{[^{}]*\\}")
    private val TIME_PATTERN = Regex("(\\d{1,2}):(\\d{2})")
    private val WHITESPACE = Regex("\\s+")
    private val lenientJson = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
}
