package com.dmytrosamoilov.offhand.feature.recording.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.CalendarEventSuggestion
import com.dmytrosamoilov.offhand.core.data.domain.NoteSuggestionsRepository
import com.dmytrosamoilov.offhand.core.data.domain.NotesRepository

class SaveNoteSuggestionsUseCase(
    private val notesRepository: NotesRepository,
    private val noteSuggestionsRepository: NoteSuggestionsRepository,
) {
    suspend operator fun invoke(noteId: Long, events: List<CalendarEventSuggestion>): Boolean {
        if (notesRepository.getNote(noteId) == null) return false
        noteSuggestionsRepository.saveSuggestions(noteId, events)
        return true
    }
}
