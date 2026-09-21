package com.dmytrosamoilov.offhand.feature.settings.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.CustomNoteStyle
import com.dmytrosamoilov.offhand.core.data.domain.CustomNoteStylesRepository
import kotlinx.coroutines.flow.Flow

class ObserveCustomNoteStylesUseCase(
    private val repository: CustomNoteStylesRepository,
) {
    operator fun invoke(): Flow<List<CustomNoteStyle>> = repository.observeStyles()
}
