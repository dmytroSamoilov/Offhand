package com.dmytrosamoilov.offhand.feature.notes.domain

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

interface DateLabelFormatter {

    fun dateTime(dateTime: LocalDateTime): String

    fun day(date: LocalDate): String

    fun time(dateTime: LocalDateTime): String
}
