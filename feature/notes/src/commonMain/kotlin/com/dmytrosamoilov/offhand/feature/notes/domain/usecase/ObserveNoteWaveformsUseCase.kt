package com.dmytrosamoilov.offhand.feature.notes.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.NoteWaveformRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ObserveNoteWaveformsUseCase(
    private val repository: NoteWaveformRepository,
) {
    operator fun invoke(): Flow<Map<Long, List<Int>>> =
        repository.observeWaveforms().map { waveforms -> waveforms.associate { it.noteId to it.levels } }
}
