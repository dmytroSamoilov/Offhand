package com.dmytrosamoilov.offhand.testing.fakes

import com.dmytrosamoilov.offhand.core.ai.api.AiBackend
import com.dmytrosamoilov.offhand.core.common.DeviceRegion
import com.dmytrosamoilov.offhand.core.common.NetworkMonitor
import com.dmytrosamoilov.offhand.core.ai.api.ModelManager
import com.dmytrosamoilov.offhand.core.ai.api.SpeechToText
import com.dmytrosamoilov.offhand.core.data.di.PLATFORM_PRO_STORE
import com.dmytrosamoilov.offhand.core.data.domain.ProStore
import com.dmytrosamoilov.offhand.core.device.BatteryRestriction
import com.dmytrosamoilov.offhand.core.device.DeviceCapabilityChecker
import com.dmytrosamoilov.offhand.core.device.DeviceTierProvider
import com.dmytrosamoilov.offhand.feature.notes.domain.review.InAppReviewRules
import com.dmytrosamoilov.offhand.feature.notes.domain.survey.SurveyRules
import com.dmytrosamoilov.offhand.feature.recording.domain.AudioRecorder
import org.koin.core.qualifier.named
import org.koin.dsl.module

val smokeFakesModule = module {
    single<DeviceCapabilityChecker> { FakeDeviceCapabilityChecker() }
    // A flow picks its tier with a launch argument that is read after the
    // app has already asked for the tier once.
    single { DeviceTierProvider(get(), isRemembered = false) }
    single { unrestrictedBattery }
    single<NetworkMonitor> { FakeNetworkMonitor() }
    single { DeviceRegion { ANALYTICS_QUESTION_COUNTRY } }
    single<ModelManager> { FakeModelManager() }
    single<SpeechToText> { FakeSpeechToText() }
    single<AiBackend> { FakeAiBackend() }
    single<AudioRecorder> { FakeAudioRecorder() }
    single<ProStore>(named(PLATFORM_PRO_STORE)) { FakeProStore() }
    single<InAppReviewRules> { neverPromptForReview }
    // The usage question would put a card above the notes once a suite has
    // made three of them, and the flows find their rows by position.
    single<SurveyRules> { SurveyRules.NEVER }
}

// The flows walk the analytics question, which only a phone in the EU sees.
private const val ANALYTICS_QUESTION_COUNTRY = "DE"

val deviceTestModule = module {
    single { unrestrictedBattery }
    single<ProStore>(named(PLATFORM_PRO_STORE)) { FakeProStore() }
    single<InAppReviewRules> { neverPromptForReview }
    single<SurveyRules> { SurveyRules.NEVER }
}

// An emulator and a test phone are not exempt from battery optimisation, and
// the onboarding step about it would stand in the way of every flow.
private val unrestrictedBattery = BatteryRestriction { false }

// The debug rules ask for a review five minutes after install, which is
// mid-suite for the Maestro flows (emulator and device suites), and the store's prompt covers the screen.
private val neverPromptForReview = InAppReviewRules(
    minSavedRecordings = Int.MAX_VALUE,
    burstWindowMs = 0L,
    attemptGapMs = 0L,
    cooldownMs = 0L,
)
