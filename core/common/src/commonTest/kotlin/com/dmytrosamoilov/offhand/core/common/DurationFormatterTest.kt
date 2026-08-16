package com.dmytrosamoilov.offhand.core.common

import kotlin.test.Test
import kotlin.test.assertEquals

class DurationFormatterTest {

    @Test
    fun subMinuteDurationsShowOneDecimalOfSeconds() {
        assertEquals("0.0 s", DurationFormatter.format(0))
        assertEquals("3.7 s", DurationFormatter.format(3_700))
        assertEquals("59.9 s", DurationFormatter.format(59_900))
    }

    @Test
    fun subHourDurationsShowMinutesAndSeconds() {
        assertEquals("1 m 00 s", DurationFormatter.format(60_000))
        assertEquals("2 m 05 s", DurationFormatter.format(125_000))
        assertEquals("59 m 59 s", DurationFormatter.format(3_599_000))
    }

    @Test
    fun longDurationsShowHoursAndMinutes() {
        assertEquals("1 h 00 m", DurationFormatter.format(3_600_000))
        assertEquals("1 h 05 m", DurationFormatter.format(3_900_000))
        assertEquals("2 h 30 m", DurationFormatter.format(9_000_000))
    }
}
