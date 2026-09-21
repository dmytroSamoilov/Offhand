package com.dmytrosamoilov.offhand.feature.recording.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.ProStatusRepository
import com.dmytrosamoilov.offhand.core.data.domain.UserPreferencesRepository
import kotlinx.coroutines.flow.first

// The pipeline step runs only for Pro users who switched suggestions on in
// Settings; either condition off skips the extraction entirely.
class IsCalendarSuggestionsAvailableUseCase(
    private val proStatus: ProStatusRepository,
    private val preferences: UserPreferencesRepository,
) {
    suspend operator fun invoke(): Boolean =
        preferences.preferences.first().smartSuggestionsEnabled && proStatus.observeStatus().first().isPro
}
