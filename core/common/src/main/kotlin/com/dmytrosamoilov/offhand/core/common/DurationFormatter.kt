package com.dmytrosamoilov.offhand.core.common

import java.util.Locale

object DurationFormatter {

    fun format(ms: Long): String {
        val totalSeconds = ms / MS_PER_SECOND
        val hours = totalSeconds / SECONDS_PER_HOUR
        val minutes = (totalSeconds % SECONDS_PER_HOUR) / SECONDS_PER_MINUTE
        val seconds = totalSeconds % SECONDS_PER_MINUTE
        return when {
            hours > 0 -> String.format(Locale.US, "%d h %02d m", hours, minutes)
            minutes > 0 -> String.format(Locale.US, "%d m %02d s", minutes, seconds)
            else -> String.format(Locale.US, "%.1f s", ms / MS_PER_SECOND.toFloat())
        }
    }

    private const val MS_PER_SECOND = 1_000L
    private const val SECONDS_PER_MINUTE = 60L
    private const val SECONDS_PER_HOUR = 3_600L
}
