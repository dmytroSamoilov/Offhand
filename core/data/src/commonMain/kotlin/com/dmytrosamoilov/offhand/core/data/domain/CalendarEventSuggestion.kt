package com.dmytrosamoilov.offhand.core.data.domain

data class CalendarEventSuggestion(
    val title: String,
    val startEpochMs: Long,
    val endEpochMs: Long,
    val isAllDay: Boolean,
    val location: String,
    val details: String,
)
