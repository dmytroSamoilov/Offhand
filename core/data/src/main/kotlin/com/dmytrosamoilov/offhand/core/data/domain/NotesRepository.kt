package com.dmytrosamoilov.offhand.core.data.domain

import kotlinx.coroutines.flow.Flow

interface NotesRepository {

    fun observeNotes(): Flow<List<Note>>

    suspend fun getNote(id: Long): Note?

    suspend fun createNote(note: Note): Long

    suspend fun updateNote(note: Note)

    suspend fun deleteNote(id: Long)
}
