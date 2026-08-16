package com.dmytrosamoilov.offhand.feature.notes.presentation

import kotlinx.datetime.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class DateLabelFormatterImplTest {

    private val formatter = DateLabelFormatterImpl()
    private val dateTime = LocalDateTime(2025, 6, 15, 15, 6, 40)

    @Test
    fun `dateTime formats month name, day, year and 24-hour time`() {
        assertEquals("Jun 15, 2025 · 15:06", formatter.dateTime(dateTime))
    }

    @Test
    fun `day formats month name, day and year without time`() {
        assertEquals("Jun 15, 2025", formatter.day(dateTime.date))
    }

    @Test
    fun `time formats as zero-padded 24-hour hour and minute`() {
        assertEquals("15:06", formatter.time(dateTime))
    }

    @Test
    fun `single digit hour and minute are zero-padded`() {
        val earlyMorning = LocalDateTime(2025, 1, 5, 4, 7, 0)

        assertEquals("04:07", formatter.time(earlyMorning))
    }
}
