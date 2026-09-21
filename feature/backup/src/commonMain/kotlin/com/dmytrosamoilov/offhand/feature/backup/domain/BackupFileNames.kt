@file:OptIn(ExperimentalTime::class)

package com.dmytrosamoilov.offhand.feature.backup.domain

import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime

object BackupFileNames {

    const val EXTENSION = ".offhand"
    private const val PREFIX = "Offhand backup"
    private val ILLEGAL_CHARS = Regex("[\\\\/:*?\"<>|\\u0000-\\u001f\\u007f]")
    private val WHITESPACE_RUNS = Regex("\\s+")
    private const val MAX_LENGTH = 80

    fun suggested(): String {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val date = "${now.year}-${now.month.number.pad()}-${now.day.pad()}"
        val time = "${now.hour.pad()}-${now.minute.pad()}"
        return "$PREFIX $date $time"
    }

    fun sanitized(raw: String): String = raw
        .replace(ILLEGAL_CHARS, " ")
        .replace(WHITESPACE_RUNS, " ")
        .trim()
        .take(MAX_LENGTH)
        .ifEmpty { suggested() }

    fun withExtension(raw: String): String = sanitized(raw).removeSuffix(EXTENSION) + EXTENSION

    private fun Int.pad(): String = toString().padStart(2, '0')
}
