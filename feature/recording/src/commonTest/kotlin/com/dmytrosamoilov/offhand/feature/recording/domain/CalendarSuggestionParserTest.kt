@file:OptIn(ExperimentalTime::class)

package com.dmytrosamoilov.offhand.feature.recording.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class CalendarSuggestionParserTest {

    private val zone = TimeZone.of("Europe/Berlin")

    @Test
    fun `thinking is dropped and timed events resolve in the given zone`() {
        val raw = "<thinking>Two items.</thinking>\n" +
            """{"events": [{"title": "Sync with Maria", "date": "2026-09-15", "time": "15:00", "durationMinutes": 30, "location": "Northwind office", "details": "Maria wants a demo."}]}"""

        val events = CalendarSuggestionParser.parse(raw, zone)

        val event = events.single()
        assertEquals("Sync with Maria", event.title)
        assertEquals(LocalDateTime(2026, 9, 15, 15, 0), Instant.fromEpochMilliseconds(event.startEpochMs).toLocalDateTime(zone))
        assertEquals(30 * 60_000L, event.endEpochMs - event.startEpochMs)
        assertFalse(event.isAllDay)
        assertEquals("Northwind office", event.location)
    }

    @Test
    fun `missing time becomes an all-day event and duration is clamped`() {
        val raw = """{"events": [{"title": "Send proposal", "date": "2026-09-18", "time": "", "durationMinutes": 5000}]}"""

        val event = CalendarSuggestionParser.parse(raw, zone).single()

        assertTrue(event.isAllDay)
        assertEquals(LocalDateTime(2026, 9, 18, 0, 0), Instant.fromEpochMilliseconds(event.startEpochMs).toLocalDateTime(zone))
        assertEquals(24 * 60 * 60_000L, event.endEpochMs - event.startEpochMs)
    }

    @Test
    fun `events without a usable date or title are dropped and the rest sorted and capped`() {
        val many = (1..12).map { day -> """{"title": "Day $day", "date": "2026-10-${day.toString().padStart(2, '0')}", "time": "09:00"}""" }
        val raw = """{"events": [{"title": "No date", "date": "sometime"}, {"title": "", "date": "2026-10-01"}, ${many.reversed().joinToString()}]}"""

        val events = CalendarSuggestionParser.parse(raw, zone)

        assertEquals(CalendarSuggestionPrompt.MAX_EVENTS, events.size)
        assertEquals("Day 1", events.first().title)
        assertTrue(events.zipWithNext().all { (a, b) -> a.startEpochMs <= b.startEpochMs })
    }

    @Test
    fun `duplicates and unparsable answers are handled`() {
        val duplicated = """{"events": [{"title": "Dentist", "date": "2026-09-25", "time": "9:00"}, {"title": "dentist", "date": "2026-09-25", "time": "09:00"}]}"""

        assertEquals(1, CalendarSuggestionParser.parse(duplicated, zone).size)
        assertTrue(CalendarSuggestionParser.parse("I found nothing.", zone).isEmpty())
        assertTrue(CalendarSuggestionParser.parse("""{"events": []}""", zone).isEmpty())
    }

    @Test
    fun `weekday-prefixed dates and wrapped times and bare arrays are accepted`() {
        val raw = """[{"title": "Standup", "date": "Thursday 2026-09-10", "time": "at 09:30"}]"""

        val event = CalendarSuggestionParser.parse(raw, zone).single()

        assertEquals(LocalDateTime(2026, 9, 10, 9, 30), Instant.fromEpochMilliseconds(event.startEpochMs).toLocalDateTime(zone))
        assertFalse(event.isAllDay)
    }

    @Test
    fun `an answer truncated by the output limit keeps its complete events`() {
        val raw = """{"events": [{"title": "Standup", "date": "2026-09-10", "time": "09:30"}, {"title": "Review", "date": "2026-09-1"""

        val events = CalendarSuggestionParser.parse(raw, zone)

        assertEquals(listOf("Standup"), events.map { it.title })
    }

    @Test
    fun `prompt anchors on the recording weekday and date`() {
        val prompt = CalendarSuggestionPrompt.build(LocalDateTime(2026, 9, 9, 18, 5))

        assertTrue(prompt.contains("Wednesday, 2026-09-09 at 18:05"))
        assertTrue(prompt.contains("Thursday 2026-09-10, Friday 2026-09-11"))
        assertTrue(prompt.contains("Wednesday 2026-09-23."))
        assertTrue(prompt.contains("<thinking></thinking>"))
        assertTrue(prompt.contains("Never invent a date"))
    }
}
