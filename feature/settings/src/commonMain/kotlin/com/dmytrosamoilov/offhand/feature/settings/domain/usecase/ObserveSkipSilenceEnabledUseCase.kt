package com.dmytrosamoilov.offhand.feature.settings.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.ProStatusRepository
import com.dmytrosamoilov.offhand.core.data.domain.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

// The switch reads as on only while the plan is Pro, the same rule playback follows.
class ObserveSkipSilenceEnabledUseCase(
    private val preferences: UserPreferencesRepository,
    private val proStatus: ProStatusRepository,
) {
    operator fun invoke(): Flow<Boolean> =
        combine(preferences.preferences, proStatus.observeStatus()) { prefs, status ->
            prefs.skipSilenceEnabled && status.isPro
        }.distinctUntilChanged()
}
