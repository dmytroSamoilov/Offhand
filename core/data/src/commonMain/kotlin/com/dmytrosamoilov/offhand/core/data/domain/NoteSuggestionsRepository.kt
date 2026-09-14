package com.dmytrosamoilov.offhand.core.data.domain

import kotlinx.coroutines.flow.Flow

interface NoteSuggestionsRepository {

    fun observeSuggestions(noteId: Long): Flow<NoteSuggestions?>

    suspend fun saveSuggestions(noteId: Long, events: List<CalendarEventSuggestion>)

    suspend fun clearSuggestions(noteId: Long)

    suspend fun updateStatus(noteId: Long, index: Int, status: SuggestionStatus)
}
