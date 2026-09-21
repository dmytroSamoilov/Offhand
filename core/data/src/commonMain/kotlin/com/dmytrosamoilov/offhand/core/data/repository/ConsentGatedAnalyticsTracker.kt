package com.dmytrosamoilov.offhand.core.data.repository

import com.dmytrosamoilov.offhand.core.data.domain.UserPreferencesRepository
import com.dmytrosamoilov.offhand.core.data.domain.analytics.AnalyticsEvent
import com.dmytrosamoilov.offhand.core.data.domain.analytics.AnalyticsSink
import com.dmytrosamoilov.offhand.core.data.domain.analytics.AnalyticsTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

// Events exist only for users who agreed to telemetry: anything sent before
// the consent is known, or after it is withdrawn, is dropped here and never
// reaches Firebase.
class ConsentGatedAnalyticsTracker(
    private val sink: AnalyticsSink,
    userPreferences: UserPreferencesRepository,
    scope: CoroutineScope,
) : AnalyticsTracker {

    private val isConsentGranted = userPreferences.preferences
        .map { it.telemetryConsent }
        .stateIn(scope, SharingStarted.Eagerly, false)

    override fun track(event: AnalyticsEvent) {
        if (!isConsentGranted.value) return
        sink.log(event.name, event.params)
    }
}
