package com.dmytrosamoilov.offhand.feature.paywall.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.ProStatusRepository
import com.dmytrosamoilov.offhand.core.data.domain.UserPreferencesRepository
import kotlinx.coroutines.flow.first

// A Pro switch already does nothing without Pro; once the plan has lapsed it
// is also turned off for good, so buying again starts from a clean slate.
// Runs on a cold start right after the trial is settled, when the status read
// here is final (a status read earlier could still be on its way).
class SwitchOffLapsedProSettingsUseCase(
    private val proStatus: ProStatusRepository,
    private val preferences: UserPreferencesRepository,
) {
    suspend operator fun invoke() {
        if (proStatus.observeStatus().first().isPro) return
        if (preferences.preferences.first().skipSilenceEnabled) preferences.setSkipSilenceEnabled(false)
    }
}
