package com.dmytrosamoilov.offhand.core.data.repository

import com.dmytrosamoilov.offhand.core.data.database.NoteSuggestionsDao
import com.dmytrosamoilov.offhand.core.data.database.toDomain
import com.dmytrosamoilov.offhand.core.data.database.toEntity
import com.dmytrosamoilov.offhand.core.data.domain.CalendarEventSuggestion
import com.dmytrosamoilov.offhand.core.data.domain.NoteSuggestions
import com.dmytrosamoilov.offhand.core.data.domain.NoteSuggestionsRepository
import com.dmytrosamoilov.offhand.core.data.domain.SuggestedEvent
import com.dmytrosamoilov.offhand.core.data.domain.SuggestionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class RoomNoteSuggestionsRepository(
    private val noteSuggestionsDao: NoteSuggestionsDao,
) : NoteSuggestionsRepository {

    override fun observeSuggestions(noteId: Long): Flow<NoteSuggestions?> =
        noteSuggestionsDao.observeByNoteId(noteId).map { entity -> entity?.toDomain() }

    override suspend fun saveSuggestions(noteId: Long, events: List<CalendarEventSuggestion>) =
        noteSuggestionsDao.upsert(NoteSuggestions(noteId, events.map(::SuggestedEvent)).toEntity())

    override suspend fun clearSuggestions(noteId: Long) = noteSuggestionsDao.deleteByNoteId(noteId)

    override suspend fun updateStatus(noteId: Long, index: Int, status: SuggestionStatus) {
        val current = noteSuggestionsDao.getByNoteId(noteId)?.toDomain() ?: return
        if (index !in current.events.indices) return
        val events = current.events.mapIndexed { position, suggested ->
            if (position == index) suggested.copy(status = status) else suggested
        }
        noteSuggestionsDao.upsert(current.copy(events = events).toEntity())
    }
}
