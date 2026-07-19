package com.dmytrosamoilov.offhand.core.data.repository

import com.dmytrosamoilov.offhand.core.data.database.NoteDao
import com.dmytrosamoilov.offhand.core.data.database.toDomain
import com.dmytrosamoilov.offhand.core.data.database.toEntity
import com.dmytrosamoilov.offhand.core.data.domain.Note
import com.dmytrosamoilov.offhand.core.data.domain.NotesRepository
import com.dmytrosamoilov.offhand.core.security.EncryptedAudioStore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
internal class RoomNotesRepository @Inject constructor(
    private val noteDao: NoteDao,
    private val audioStore: EncryptedAudioStore,
) : NotesRepository {

    override fun observeNotes(): Flow<List<Note>> =
        noteDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun getNote(id: Long): Note? = noteDao.getById(id)?.toDomain()

    override suspend fun createNote(note: Note): Long = noteDao.insert(note.toEntity())

    override suspend fun updateNote(note: Note) = noteDao.update(note.toEntity())

    override suspend fun deleteNote(id: Long) {
        noteDao.getById(id)?.audioFileName?.let(audioStore::delete)
        noteDao.deleteById(id)
    }
}
