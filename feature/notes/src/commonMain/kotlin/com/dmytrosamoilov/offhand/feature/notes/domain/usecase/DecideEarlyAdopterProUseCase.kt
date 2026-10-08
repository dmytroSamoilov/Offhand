package com.dmytrosamoilov.offhand.feature.notes.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.EarlyAdopterPro
import com.dmytrosamoilov.offhand.core.data.domain.NotesRepository
import com.dmytrosamoilov.offhand.core.data.domain.UserPreferencesRepository
import com.dmytrosamoilov.offhand.core.data.domain.analytics.AnalyticsEvents
import com.dmytrosamoilov.offhand.core.data.domain.analytics.AnalyticsTracker
import kotlinx.coroutines.flow.first

// Settles the early-adopter grant once per install: an install that finished
// onboarding before 1.5 and holds at least one note keeps Pro on this device.
// A fresh install is settled before onboarding ends, so its first note never
// counts. The root screens show the thank-you while the grant is stored and
// not yet acknowledged.
class DecideEarlyAdopterProUseCase(
    private val userPreferences: UserPreferencesRepository,
    private val notesRepository: NotesRepository,
    private val analyticsTracker: AnalyticsTracker,
) {

    suspend operator fun invoke() {
        val preferences = userPreferences.preferences.first()
        if (preferences.earlyAdopterPro != EarlyAdopterPro.UNDECIDED) return
        val granted = preferences.onboardingCompleted && notesRepository.countNotes() > 0
        userPreferences.setEarlyAdopterPro(if (granted) EarlyAdopterPro.GRANTED else EarlyAdopterPro.NONE)
        if (granted) analyticsTracker.track(AnalyticsEvents.proGrandfathered())
    }
}
