package com.dmytrosamoilov.offhand.feature.recording.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.ProStatusRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class IsAudioImportAvailableUseCase(
    private val repository: ProStatusRepository,
) {
    operator fun invoke(): Flow<Boolean> =
        repository.observeStatus().map { it.isPro }.distinctUntilChanged()
}
