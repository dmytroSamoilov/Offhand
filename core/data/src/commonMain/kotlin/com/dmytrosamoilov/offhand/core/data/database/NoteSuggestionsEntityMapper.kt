package com.dmytrosamoilov.offhand.core.data.database

import com.dmytrosamoilov.offhand.core.data.domain.CalendarEventSuggestion
import com.dmytrosamoilov.offhand.core.data.domain.NoteSuggestions
import com.dmytrosamoilov.offhand.core.data.domain.SuggestedEvent
import com.dmytrosamoilov.offhand.core.data.domain.SuggestionStatus
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class SuggestedEventJson(
    val title: String,
    val startEpochMs: Long,
    val endEpochMs: Long,
    val isAllDay: Boolean,
    val location: String,
    val details: String,
    val status: String,
)

private val json = Json { ignoreUnknownKeys = true }

internal fun NoteSuggestionsEntity.toDomain(): NoteSuggestions = NoteSuggestions(
    noteId = noteId,
    events = decodeEvents(eventsJson),
)

internal fun NoteSuggestions.toEntity(): NoteSuggestionsEntity = NoteSuggestionsEntity(
    noteId = noteId,
    eventsJson = encodeEvents(events),
)

private fun decodeEvents(raw: String): List<SuggestedEvent> =
    runCatching { json.decodeFromString<List<SuggestedEventJson>>(raw) }
        .getOrDefault(emptyList())
        .map { it.toDomain() }

private fun SuggestedEventJson.toDomain(): SuggestedEvent = SuggestedEvent(
    event = CalendarEventSuggestion(title, startEpochMs, endEpochMs, isAllDay, location, details),
    status = SuggestionStatus.fromName(status),
)

private fun encodeEvents(events: List<SuggestedEvent>): String =
    json.encodeToString(events.map { it.toJson() })

private fun SuggestedEvent.toJson(): SuggestedEventJson = SuggestedEventJson(
    title = event.title,
    startEpochMs = event.startEpochMs,
    endEpochMs = event.endEpochMs,
    isAllDay = event.isAllDay,
    location = event.location,
    details = event.details,
    status = status.name,
)
