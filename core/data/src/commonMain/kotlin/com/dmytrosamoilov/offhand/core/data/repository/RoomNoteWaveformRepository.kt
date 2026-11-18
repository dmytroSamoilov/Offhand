package com.dmytrosamoilov.offhand.core.data.repository

import com.dmytrosamoilov.offhand.core.data.database.NoteWaveformDao
import com.dmytrosamoilov.offhand.core.data.database.NoteWaveformEntity
import com.dmytrosamoilov.offhand.core.data.domain.NoteWaveform
import com.dmytrosamoilov.offhand.core.data.domain.NoteWaveformRepository
import com.dmytrosamoilov.offhand.core.data.domain.ProtectedDataState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class RoomNoteWaveformRepository(
    private val noteWaveformDao: NoteWaveformDao,
    private val protectedData: ProtectedDataState,
) : NoteWaveformRepository {

    override fun observeWaveforms(): Flow<List<NoteWaveform>> =
        noteWaveformDao.observeAll().map { entities -> entities.map { it.toDomain() } }.retryWhileUnreadable(protectedData)

    override suspend fun saveWaveform(waveform: NoteWaveform) = noteWaveformDao.upsert(
        NoteWaveformEntity(noteId = waveform.noteId, levels = waveform.levels.joinToString(LEVEL_SEPARATOR)),
    )

    private fun NoteWaveformEntity.toDomain() = NoteWaveform(
        noteId = noteId,
        levels = levels.split(LEVEL_SEPARATOR).mapNotNull(String::toIntOrNull),
    )

    private companion object {
        const val LEVEL_SEPARATOR = ","
    }
}
