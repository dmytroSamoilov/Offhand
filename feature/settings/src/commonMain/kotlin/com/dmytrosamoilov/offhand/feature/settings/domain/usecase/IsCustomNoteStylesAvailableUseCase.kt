package com.dmytrosamoilov.offhand.feature.settings.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.EntitlementsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class IsCustomNoteStylesAvailableUseCase(
    private val repository: EntitlementsRepository,
) {
    operator fun invoke(): Flow<Boolean> =
        repository.observeEntitlements().map { it.customStylesUnlocked }.distinctUntilChanged()
}
