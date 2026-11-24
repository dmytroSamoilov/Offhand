package com.dmytrosamoilov.offhand.feature.notes.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.UserPreferencesRepository
import com.dmytrosamoilov.offhand.core.device.DeviceTierProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class IsSmartSuggestionsEnabledUseCase(
    private val repository: UserPreferencesRepository,
    private val deviceTier: DeviceTierProvider,
) {
    operator fun invoke(): Flow<Boolean> =
        repository.preferences.map { it.smartSuggestionsEnabled && deviceTier.writesSummaries() }.distinctUntilChanged()
}
