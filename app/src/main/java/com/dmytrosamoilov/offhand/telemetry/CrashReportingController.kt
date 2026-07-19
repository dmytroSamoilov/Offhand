package com.dmytrosamoilov.offhand.telemetry

import android.content.Context
import com.dmytrosamoilov.offhand.core.data.domain.UserPreferencesRepository
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@Singleton
class CrashReportingController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userPreferences: UserPreferencesRepository,
) {

    // Application-lifetime scope: lives as long as the process, never cancelled.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun start() {
        if (FirebaseApp.getApps(context).isEmpty()) return
        scope.launch {
            userPreferences.preferences
                .map { it.telemetryConsent }
                .distinctUntilChanged()
                .collect { granted ->
                    FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(granted)
                }
        }
    }
}
