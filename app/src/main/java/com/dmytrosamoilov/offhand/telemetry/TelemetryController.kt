package com.dmytrosamoilov.offhand.telemetry

import android.content.Context
import com.dmytrosamoilov.offhand.core.data.domain.UserPreferencesRepository
import com.google.firebase.analytics.FirebaseAnalytics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class TelemetryController(
    private val context: Context,
    private val userPreferences: UserPreferencesRepository,
) {

    // Application-lifetime scope: lives as long as the process, never cancelled.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun start() {
        scope.launch {
            userPreferences.preferences
                .map { it.telemetryConsent }
                .distinctUntilChanged()
                .collect(::applyConsent)
        }
    }

    // FirebaseAnalytics.getInstance can self-initialise measurement, so it is only
    // reachable behind a non-null Crashlytics — proof that FirebaseApp exists.
    private fun applyConsent(isGranted: Boolean) {
        if (!isGranted) {
            // Firebase that was never initialised has nothing to send and nothing
            // to switch off.
            FirebaseReporting.instanceOrNull(context)?.let { crashlytics ->
                crashlytics.setCrashlyticsCollectionEnabled(false)
                crashlytics.deleteUnsentReports()
                FirebaseAnalytics.getInstance(context).setAnalyticsCollectionEnabled(false)
            }
            return
        }
        FirebaseReporting.initialize(context)?.let { crashlytics ->
            crashlytics.setCrashlyticsCollectionEnabled(true)
            FirebaseAnalytics.getInstance(context).setAnalyticsCollectionEnabled(true)
        }
    }
}
