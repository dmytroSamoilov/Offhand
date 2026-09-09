package com.dmytrosamoilov.offhand.core.data.repository

import com.dmytrosamoilov.offhand.core.data.database.NoteDao
import com.dmytrosamoilov.offhand.core.data.database.NoteStyleDao
import com.dmytrosamoilov.offhand.core.data.database.toDomain
import com.dmytrosamoilov.offhand.core.data.database.toEntity
import com.dmytrosamoilov.offhand.core.data.domain.CustomNoteStyle
import com.dmytrosamoilov.offhand.core.data.domain.CustomNoteStylesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class RoomCustomNoteStylesRepository(
    private val noteStyleDao: NoteStyleDao,
    private val noteDao: NoteDao,
) : CustomNoteStylesRepository {

    override fun observeStyles(): Flow<List<CustomNoteStyle>> =
        noteStyleDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun getStyle(id: Long): CustomNoteStyle? = noteStyleDao.getById(id)?.toDomain()

    override suspend fun createStyle(style: CustomNoteStyle): Long =
        noteStyleDao.insert(style.copy(id = 0).toEntity())

    override suspend fun updateStyle(style: CustomNoteStyle) = noteStyleDao.update(style.toEntity())

    override suspend fun deleteStyle(id: Long) {
        noteDao.clearCustomStyle(id)
        noteStyleDao.deleteById(id)
    }
}
