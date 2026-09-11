package com.dmytrosamoilov.offhand.feature.settings.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.ProOverride
import com.dmytrosamoilov.offhand.core.data.domain.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class ObserveProOverrideUseCase(
    private val repository: UserPreferencesRepository,
) {
    operator fun invoke(): Flow<ProOverride> =
        repository.preferences.map { it.proOverride }.distinctUntilChanged()
}
