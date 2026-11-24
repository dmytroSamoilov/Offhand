package com.dmytrosamoilov.offhand.feature.notes.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.ProStatusRepository
import com.dmytrosamoilov.offhand.core.data.domain.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

// Skipping silence is Pro: the switch only counts while the plan is.
class ObserveSkipSilenceUseCase(
    private val preferences: UserPreferencesRepository,
    private val proStatus: ProStatusRepository,
) {
    operator fun invoke(): Flow<Boolean> =
        combine(preferences.preferences, proStatus.observeStatus()) { prefs, status ->
            prefs.skipSilenceEnabled && status.isPro
        }.distinctUntilChanged()
}
