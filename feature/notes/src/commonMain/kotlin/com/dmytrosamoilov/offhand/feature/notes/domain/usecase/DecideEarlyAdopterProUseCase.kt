package com.dmytrosamoilov.offhand.feature.notes.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.EarlyAdopterPro
import com.dmytrosamoilov.offhand.core.data.domain.NotesRepository
import com.dmytrosamoilov.offhand.core.data.domain.ProStatusRepository
import com.dmytrosamoilov.offhand.core.data.domain.UserPreferencesRepository
import com.dmytrosamoilov.offhand.core.data.domain.analytics.AnalyticsEvents
import com.dmytrosamoilov.offhand.core.data.domain.analytics.AnalyticsTracker
import kotlinx.coroutines.flow.first

// Settles the early-adopter grant once per install: an install that finished
// onboarding before 1.5 and holds at least one note keeps Pro on this device.
// A fresh install is settled before onboarding ends, so its first note never
// counts. The root screens show the thank-you while the grant is stored and
// not yet acknowledged; an install that already pays (trial or subscription)
// is granted quietly, with the thank-you marked as seen, because "Pro is
// yours" would read as a message about their purchase.
class DecideEarlyAdopterProUseCase(
    private val userPreferences: UserPreferencesRepository,
    private val notesRepository: NotesRepository,
    private val proStatusRepository: ProStatusRepository,
    private val analyticsTracker: AnalyticsTracker,
) {

    suspend operator fun invoke() {
        val preferences = userPreferences.preferences.first()
        if (preferences.earlyAdopterPro != EarlyAdopterPro.UNDECIDED) return
        val granted = preferences.onboardingCompleted && notesRepository.countNotes() > 0
        if (!granted) {
            userPreferences.setEarlyAdopterPro(EarlyAdopterPro.NONE)
            return
        }
        // Read before the grant is stored, while it cannot stand in for the store yet
        val hasPurchase = proStatusRepository.observeStatus().first().isPro
        userPreferences.setEarlyAdopterPro(EarlyAdopterPro.GRANTED)
        if (hasPurchase) userPreferences.setEarlyAdopterThanked()
        analyticsTracker.track(AnalyticsEvents.proGrandfathered(hasPurchase))
    }
}
