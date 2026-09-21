package com.dmytrosamoilov.offhand.feature.settings.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.ProOverride
import com.dmytrosamoilov.offhand.core.data.domain.UserPreferencesRepository

class SetProOverrideUseCase(
    private val repository: UserPreferencesRepository,
) {
    suspend operator fun invoke(override: ProOverride) = repository.setProOverride(override)
}
