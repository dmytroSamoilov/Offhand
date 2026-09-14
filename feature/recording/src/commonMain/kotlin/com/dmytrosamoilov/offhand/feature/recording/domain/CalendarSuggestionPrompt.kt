package com.dmytrosamoilov.offhand.feature.recording.domain

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.plus

internal object CalendarSuggestionPrompt {

    const val MAX_EVENTS = 10
    private const val UPCOMING_DAYS = 14

    fun build(recordedAt: LocalDateTime): String = listOf(
        intro(recordedAt),
        upcomingDays(recordedAt.date),
        EVENT_RULES,
        THINKING_RULE,
        OUTPUT_INTRO,
        JSON_SHAPE,
        JSON_RULES,
    ).joinToString(LINE_BREAK)

    private fun intro(recordedAt: LocalDateTime): String =
        "You will receive a note written from a voice recording. The recording was made on " +
            "${recordedAt.dayOfWeek.name.lowercase().replaceFirstChar(Char::uppercase)}, " +
            "${recordedAt.date} at ${twoDigits(recordedAt.hour)}:${twoDigits(recordedAt.minute)}. " +
            "Find every appointment, meeting, call, deadline or reminder in it that has a date or a time, " +
            "so the user can add it to their calendar."

    // Small models miscount weekdays; the next two weeks are spelled out so a
    // relative day resolves by lookup instead of arithmetic.
    private fun upcomingDays(recordedOn: LocalDate): String {
        val days = (1..UPCOMING_DAYS).map { offset ->
            val day = recordedOn.plus(offset, DateTimeUnit.DAY)
            "${day.dayOfWeek.name.lowercase().replaceFirstChar(Char::uppercase)} $day"
        }
        return "The days after the recording are: ${days.joinToString(", ")}."
    }

    private fun twoDigits(value: Int): String = value.toString().padStart(2, '0')

    private const val LINE_BREAK = "\n"

    private val EVENT_RULES = """
        Rules for the events:
        - Resolve relative expressions such as tomorrow, next Friday, in two weeks or on the 14th against the recording date using the list of days above, and write the resolved date as YYYY-MM-DD. A weekday alone means the next occurrence after the recording date.
        - "time": the start time in 24-hour HH:MM when one is said or clearly implied such as morning or after lunch; otherwise leave it empty and the event becomes an all-day entry.
        - "durationMinutes": the length when it is said, otherwise 60.
        - "title": at most 8 words that name the event, in the same language as the note.
        - "location": the place when one is said, otherwise empty.
        - "details": one sentence with what was said about the event, including who is involved.
        - Skip anything that has no determinable date. Never invent a date, a time or a participant. When the same event is mentioned several times, list it once.
        - List at most $MAX_EVENTS events, in the order they happen.
    """.trimIndent()

    private const val THINKING_RULE =
        "First think inside one <thinking></thinking> block, in at most three short sentences: which " +
            "dated items the note contains and what each relative date resolves to. Close the block before you answer."

    private const val OUTPUT_INTRO =
        "After the thinking block, output a single JSON object, exactly in this shape:"

    private const val JSON_SHAPE =
        """{"events": [{"title": "...", "date": "YYYY-MM-DD", "time": "HH:MM", "durationMinutes": 60, "location": "...", "details": "..."}]}"""

    private val JSON_RULES = """
        Rules for the JSON output:
        - Output exactly one JSON object and no other text after it. When there are no dated events, output an empty list.
        - Never use double quotes inside the field values.
    """.trimIndent()
}
