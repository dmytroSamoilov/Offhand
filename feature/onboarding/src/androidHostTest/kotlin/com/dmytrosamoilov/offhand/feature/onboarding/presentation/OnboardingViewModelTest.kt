package com.dmytrosamoilov.offhand.feature.onboarding.presentation

import com.dmytrosamoilov.offhand.core.data.domain.ModelDownloadLauncher
import com.dmytrosamoilov.offhand.core.data.domain.analytics.AnalyticsEvent
import com.dmytrosamoilov.offhand.core.data.domain.analytics.AnalyticsEvents
import com.dmytrosamoilov.offhand.core.data.domain.analytics.AnalyticsTracker
import com.dmytrosamoilov.offhand.core.device.DeviceCapability
import com.dmytrosamoilov.offhand.core.device.DeviceCapabilityChecker
import com.dmytrosamoilov.offhand.core.data.domain.TelemetryConsent
import com.dmytrosamoilov.offhand.feature.onboarding.domain.usecase.AnswerTelemetryConsentUseCase
import com.dmytrosamoilov.offhand.feature.onboarding.domain.usecase.CompleteOnboardingUseCase
import com.dmytrosamoilov.offhand.feature.onboarding.domain.usecase.GetTelemetryConsentUseCase
import com.dmytrosamoilov.offhand.feature.onboarding.domain.usecase.SetAppLockEnabledUseCase
import io.mockk.coVerify
import io.mockk.coEvery
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private val capableDevice =
        DeviceCapability(totalRamMb = 12 * 1024, availableRamMb = 6 * 1024, cpuCores = 8)
    private val midDevice =
        DeviceCapability(totalRamMb = 3_660, availableRamMb = 1_800, cpuCores = 8)
    private val weakDevice =
        DeviceCapability(totalRamMb = 1_850, availableRamMb = 900, cpuCores = 4)

    private val deviceCapabilityChecker: DeviceCapabilityChecker = mockk()
    private val setAppLockEnabled: SetAppLockEnabledUseCase = mockk(relaxed = true)
    private val completeOnboarding: CompleteOnboardingUseCase = mockk(relaxed = true)
    private val modelDownloadLauncher: ModelDownloadLauncher = mockk(relaxed = true)
    private val getTelemetryConsent: GetTelemetryConsentUseCase = mockk()
    private val answerTelemetryConsent: AnswerTelemetryConsentUseCase = mockk(relaxed = true)
    private val tracked = mutableListOf<AnalyticsEvent>()
    private val analyticsTracker = object : AnalyticsTracker {
        override fun track(event: AnalyticsEvent) {
            tracked += event
        }
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(
        capability: DeviceCapability,
        consent: TelemetryConsent = TelemetryConsent.SHARED,
    ): OnboardingViewModel {
        every { deviceCapabilityChecker.snapshot() } returns capability
        coEvery { getTelemetryConsent() } returns consent
        val viewModel = OnboardingViewModel(
            modelDownloadLauncher = modelDownloadLauncher,
            deviceCapabilityChecker = deviceCapabilityChecker,
            setAppLockEnabled = setAppLockEnabled,
            completeOnboarding = completeOnboarding,
            getTelemetryConsent = getTelemetryConsent,
            answerTelemetryConsent = answerTelemetryConsent,
            analyticsTracker = analyticsTracker,
        )
        dispatcher.scheduler.advanceUntilIdle()
        return viewModel
    }

    @Test
    fun `a weak device lands on the incompatible step with its specs`() = runTest(dispatcher) {
        val viewModel = viewModel(weakDevice)

        val state = viewModel.uiState.value
        assertEquals(OnboardingStep.DEVICE_INCOMPATIBLE, state.step)
        assertEquals("1.8", state.deviceSpecs?.totalRamGb)
        assertEquals("3", state.deviceSpecs?.requiredRamGb)
        assertEquals(false, state.deviceSpecs?.isRamSatisfied)
        assertEquals(listOf("device_unsupported"), tracked.map { it.name })
        verify(exactly = 0) { modelDownloadLauncher.startIfAllowed() }
    }

    @Test
    fun `a capable device lands on the welcome screen and starts the download`() = runTest(dispatcher) {
        val viewModel = viewModel(capableDevice)

        assertEquals(OnboardingStep.WELCOME, viewModel.uiState.value.step)
        verify(exactly = 1) { modelDownloadLauncher.startIfAllowed() }
    }

    @Test
    fun `a middle-tier phone first learns that summaries are off, then reaches the welcome screen`() = runTest(dispatcher) {
        val viewModel = viewModel(midDevice)

        assertEquals(OnboardingStep.TRANSCRIPTS_ONLY, viewModel.uiState.value.step)
        assertEquals(false, viewModel.uiState.value.isSummaryAvailable)
        verify(exactly = 1) { modelDownloadLauncher.startIfAllowed() }

        viewModel.onContinueWithTranscriptsClicked()

        assertEquals(OnboardingStep.WELCOME, viewModel.uiState.value.step)
        assertEquals(
            listOf("device_transcripts_only", "onboarding_step"),
            tracked.map { it.name },
        )
        assertEquals(AnalyticsEvents.onboardingStep("transcripts_only"), tracked.last())
    }

    @Test
    fun `recording the first note ends onboarding with the lock off`() = runTest(dispatcher) {
        val viewModel = viewModel(capableDevice)

        viewModel.onRecordFirstNoteClicked()
        dispatcher.scheduler.advanceUntilIdle()

        coVerifyOrder {
            setAppLockEnabled(false)
            completeOnboarding()
        }
        assertEquals(
            listOf(AnalyticsEvents.onboardingStep("record_first_note"), AnalyticsEvents.onboardingCompleted()),
            tracked,
        )
    }

    @Test
    fun `looking around first ends onboarding with the lock off`() = runTest(dispatcher) {
        val viewModel = viewModel(capableDevice)

        viewModel.onLookAroundClicked()
        dispatcher.scheduler.advanceUntilIdle()

        coVerify { setAppLockEnabled(false) }
        coVerify { completeOnboarding() }
        assertEquals(
            listOf(AnalyticsEvents.onboardingStep("look_around"), AnalyticsEvents.onboardingCompleted()),
            tracked,
        )
    }

    @Test
    fun `an unsupported device cannot finish onboarding`() = runTest(dispatcher) {
        val viewModel = viewModel(weakDevice)

        viewModel.onLookAroundClicked()
        dispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { completeOnboarding() }
    }

    @Test
    fun `a fresh install is asked about analytics before the device is checked`() = runTest(dispatcher) {
        val viewModel = viewModel(weakDevice, consent = TelemetryConsent.UNANSWERED)

        assertEquals(OnboardingStep.ANALYTICS_CONSENT, viewModel.uiState.value.step)
        assertEquals(emptyList<AnalyticsEvent>(), tracked)
        verify(exactly = 0) { deviceCapabilityChecker.snapshot() }
        verify(exactly = 0) { modelDownloadLauncher.startIfAllowed() }
    }

    @Test
    fun `sharing stores the answer, says so and then checks the device`() = runTest(dispatcher) {
        val viewModel = viewModel(capableDevice, consent = TelemetryConsent.UNANSWERED)

        viewModel.onShareAnalyticsClicked()
        dispatcher.scheduler.advanceUntilIdle()

        coVerify { answerTelemetryConsent(true) }
        assertEquals(listOf(AnalyticsEvents.onboardingStep("analytics_shared")), tracked)
        assertEquals(OnboardingStep.WELCOME, viewModel.uiState.value.step)
        verify(exactly = 1) { modelDownloadLauncher.startIfAllowed() }
    }

    @Test
    fun `not sharing stores the answer and continues to the device check`() = runTest(dispatcher) {
        val viewModel = viewModel(weakDevice, consent = TelemetryConsent.UNANSWERED)

        viewModel.onDontShareAnalyticsClicked()
        dispatcher.scheduler.advanceUntilIdle()

        coVerify { answerTelemetryConsent(false) }
        assertEquals(OnboardingStep.DEVICE_INCOMPATIBLE, viewModel.uiState.value.step)
        assertEquals(listOf("device_unsupported"), tracked.map { it.name })
    }

    @Test
    fun `a second tap on the consent buttons is ignored`() = runTest(dispatcher) {
        val viewModel = viewModel(capableDevice, consent = TelemetryConsent.UNANSWERED)

        viewModel.onShareAnalyticsClicked()
        viewModel.onDontShareAnalyticsClicked()
        dispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { answerTelemetryConsent(any()) }
        coVerify(exactly = 0) { answerTelemetryConsent(false) }
        verify(exactly = 1) { modelDownloadLauncher.startIfAllowed() }
    }

    @Test
    fun `an answer given earlier skips the question`() = runTest(dispatcher) {
        val viewModel = viewModel(capableDevice, consent = TelemetryConsent.DECLINED)

        assertEquals(OnboardingStep.WELCOME, viewModel.uiState.value.step)
        coVerify(exactly = 0) { answerTelemetryConsent(any()) }
    }

    @Test
    fun `the device buttons do nothing while the question is open`() = runTest(dispatcher) {
        val viewModel = viewModel(midDevice, consent = TelemetryConsent.UNANSWERED)

        viewModel.onContinueWithTranscriptsClicked()
        viewModel.onLookAroundClicked()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(OnboardingStep.ANALYTICS_CONSENT, viewModel.uiState.value.step)
        coVerify(exactly = 0) { completeOnboarding() }
    }
}
