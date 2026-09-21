package com.dmytrosamoilov.offhand.testing.fakes

import com.dmytrosamoilov.offhand.core.ai.api.AiBackend
import com.dmytrosamoilov.offhand.core.ai.api.ModelManager
import com.dmytrosamoilov.offhand.core.ai.api.SpeechToText
import com.dmytrosamoilov.offhand.core.data.di.PLATFORM_PRO_STORE
import com.dmytrosamoilov.offhand.core.data.domain.ProStore
import com.dmytrosamoilov.offhand.core.device.DeviceCapabilityChecker
import com.dmytrosamoilov.offhand.feature.notes.domain.review.InAppReviewRules
import com.dmytrosamoilov.offhand.feature.recording.domain.AudioRecorder
import org.koin.core.qualifier.named
import org.koin.dsl.module

val smokeFakesModule = module {
    single<DeviceCapabilityChecker> { FakeDeviceCapabilityChecker() }
    single<ModelManager> { FakeModelManager() }
    single<SpeechToText> { FakeSpeechToText() }
    single<AiBackend> { FakeAiBackend() }
    single<AudioRecorder> { FakeAudioRecorder() }
    single<ProStore>(named(PLATFORM_PRO_STORE)) { FakeProStore() }
    single<InAppReviewRules> { neverPromptForReview }
}

// The debug rules ask for a review five minutes after install, which is
// mid-suite for the Maestro flows, and the store's prompt covers the screen.
private val neverPromptForReview = InAppReviewRules(
    minSavedRecordings = Int.MAX_VALUE,
    minInstallAgeMs = Long.MAX_VALUE,
    burstWindowMs = 0L,
    attemptGapMs = 0L,
    cooldownMs = 0L,
)
