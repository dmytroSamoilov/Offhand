package com.dmytrosamoilov.offhand.core.common

object DurationFormatter {

    fun format(ms: Long): String {
        val totalSeconds = ms / MS_PER_SECOND
        val hours = totalSeconds / SECONDS_PER_HOUR
        val minutes = (totalSeconds % SECONDS_PER_HOUR) / SECONDS_PER_MINUTE
        val seconds = totalSeconds % SECONDS_PER_MINUTE
        return when {
            hours > 0 -> "$hours h ${minutes.padded()} m"
            minutes > 0 -> "$minutes m ${seconds.padded()} s"
            else -> "${tenthsOfSecond(ms)} s"
        }
    }

    private fun Long.padded(): String = toString().padStart(2, '0')

    private fun tenthsOfSecond(ms: Long): String {
        val tenths = (ms + TENTH_ROUNDING_MS) / MS_PER_TENTH
        return "${tenths / TENTHS_PER_SECOND}.${tenths % TENTHS_PER_SECOND}"
    }

    private const val MS_PER_SECOND = 1_000L
    private const val SECONDS_PER_MINUTE = 60L
    private const val SECONDS_PER_HOUR = 3_600L
    private const val MS_PER_TENTH = 100L
    private const val TENTH_ROUNDING_MS = 50L
    private const val TENTHS_PER_SECOND = 10L
}
