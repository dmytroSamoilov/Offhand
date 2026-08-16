package com.dmytrosamoilov.offhand.feature.notes.presentation

import com.dmytrosamoilov.offhand.feature.notes.domain.DateLabelFormatter
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toJavaLocalDateTime

class DateLabelFormatterImpl : DateLabelFormatter {

    override fun dateTime(dateTime: LocalDateTime): String =
        DATE_TIME_FORMATTER.format(dateTime.toJavaLocalDateTime())

    override fun day(date: LocalDate): String =
        DAY_FORMATTER.format(date.toJavaLocalDate())

    override fun time(dateTime: LocalDateTime): String =
        TIME_FORMATTER.format(dateTime.toJavaLocalDateTime())

    private companion object {
        val DATE_TIME_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("MMM d, yyyy · HH:mm", Locale.US)
        val DAY_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US)
        val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.US)
    }
}
