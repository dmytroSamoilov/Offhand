package com.dmytrosamoilov.offhand.core.data.domain

data class NoteSuggestions(
    val noteId: Long,
    val events: List<SuggestedEvent>,
)

data class SuggestedEvent(
    val event: CalendarEventSuggestion,
    val status: SuggestionStatus = SuggestionStatus.PENDING,
)

enum class SuggestionStatus {
    PENDING,
    ADDED,
    DISMISSED,
    ;

    companion object {
        fun fromName(name: String): SuggestionStatus = entries.firstOrNull { it.name == name } ?: PENDING
    }
}
