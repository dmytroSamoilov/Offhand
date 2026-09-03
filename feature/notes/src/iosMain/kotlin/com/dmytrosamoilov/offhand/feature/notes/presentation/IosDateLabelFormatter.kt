package com.dmytrosamoilov.offhand.feature.notes.presentation

import com.dmytrosamoilov.offhand.feature.notes.domain.DateLabelFormatter
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.number
import platform.Foundation.NSCalendar
import platform.Foundation.NSDate
import platform.Foundation.NSDateComponents
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale

class IosDateLabelFormatter : DateLabelFormatter {

    private val dateTimeFormatter = formatter("MMM d, yyyy · HH:mm")
    private val dayFormatter = formatter("MMM d, yyyy")
    private val timeFormatter = formatter("HH:mm")

    override fun dateTime(dateTime: LocalDateTime): String =
        dateTimeFormatter.stringFromDate(dateTime.toNsDate())

    override fun day(date: LocalDate): String =
        dayFormatter.stringFromDate(LocalDateTime(date.year, date.month, date.day, 0, 0).toNsDate())

    override fun time(dateTime: LocalDateTime): String =
        timeFormatter.stringFromDate(dateTime.toNsDate())

    private fun formatter(pattern: String): NSDateFormatter = NSDateFormatter().apply {
        dateFormat = pattern
        locale = NSLocale(localeIdentifier = "en_US_POSIX")
    }

    private fun LocalDateTime.toNsDate(): NSDate {
        val components = NSDateComponents().also {
            it.year = year.toLong()
            it.month = month.number.toLong()
            it.day = day.toLong()
            it.hour = hour.toLong()
            it.minute = minute.toLong()
            it.second = second.toLong()
        }
        return requireNotNull(NSCalendar.currentCalendar.dateFromComponents(components))
    }
}
