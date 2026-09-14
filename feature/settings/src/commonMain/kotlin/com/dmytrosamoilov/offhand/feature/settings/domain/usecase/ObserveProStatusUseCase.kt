package com.dmytrosamoilov.offhand.feature.settings.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.ProStatus
import com.dmytrosamoilov.offhand.core.data.domain.ProStatusRepository
import kotlinx.coroutines.flow.Flow

class ObserveProStatusUseCase(
    private val repository: ProStatusRepository,
) {
    operator fun invoke(): Flow<ProStatus> = repository.observeStatus()
}
