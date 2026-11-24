package com.dmytrosamoilov.offhand.core.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.dmytrosamoilov.offhand.core.common.BuildInfo
import com.dmytrosamoilov.offhand.core.common.DeviceRegion
import com.dmytrosamoilov.offhand.core.data.domain.TelemetryConsent
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath

class TelemetryConsentPreferenceTest {

    private val directory: File = Files.createTempDirectory("consent").toFile()
    private val dataStoreScope = CoroutineScope(SupervisorJob())
    private val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.createWithPath(scope = dataStoreScope) {
        File(directory, "prefs.preferences_pb").absolutePath.toPath()
    }
    private var country = "DE"
    private val repository = DataStoreUserPreferencesRepository(
        dataStore,
        BuildInfo(isDeveloperBuild = false, platform = "android"),
        DeviceRegion { country },
    )

    @AfterTest
    fun tearDown() {
        dataStoreScope.cancel()
        directory.deleteRecursively()
    }

    @Test
    fun `a fresh install in the EU has not answered`() = runTest {
        assertEquals(TelemetryConsent.UNANSWERED, consent())
    }

    @Test
    fun `sharing in onboarding is stored as shared`() = runTest {
        repository.setTelemetryConsent(true)

        assertEquals(TelemetryConsent.SHARED, consent())
    }

    @Test
    fun `not sharing in onboarding is stored as declined`() = runTest {
        repository.setTelemetryConsent(false)

        assertEquals(TelemetryConsent.DECLINED, consent())
    }

    @Test
    fun `an install onboarded before the question keeps sharing`() = runTest {
        repository.setOnboardingCompleted(true)

        assertEquals(TelemetryConsent.SHARED, consent())
    }

    @Test
    fun `an install onboarded before the question keeps its switched-off setting`() = runTest {
        repository.setOnboardingCompleted(true)
        repository.setTelemetryConsent(false)

        assertEquals(TelemetryConsent.DECLINED, consent())
    }

    @Test
    fun `an install that left onboarding unfinished under the old default is asked`() = runTest {
        dataStore.edit { it[booleanPreferencesKey("onboarding_completed")] = false }

        assertEquals(TelemetryConsent.UNANSWERED, consent())
    }

    @Test
    fun `a fresh install outside the EU shares by default`() = runTest {
        country = "US"

        assertEquals(TelemetryConsent.SHARED, consent())
    }

    @Test
    fun `a fresh install outside the EU keeps a switched-off setting`() = runTest {
        country = "US"
        repository.setTelemetryConsent(false)

        assertEquals(TelemetryConsent.DECLINED, consent())
    }

    @Test
    fun `a phone that reports no region is asked`() = runTest {
        country = ""

        assertEquals(TelemetryConsent.UNANSWERED, consent())
    }

    private suspend fun consent(): TelemetryConsent = repository.preferences.first().telemetryConsent
}
