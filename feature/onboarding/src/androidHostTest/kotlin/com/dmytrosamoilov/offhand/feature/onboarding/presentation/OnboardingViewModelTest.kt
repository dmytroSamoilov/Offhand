package com.dmytrosamoilov.offhand.feature.onboarding.presentation

import com.dmytrosamoilov.offhand.core.data.domain.ModelDownloadLauncher
import com.dmytrosamoilov.offhand.core.data.domain.analytics.AnalyticsEvent
import com.dmytrosamoilov.offhand.core.data.domain.analytics.AnalyticsEvents
import com.dmytrosamoilov.offhand.core.data.domain.analytics.AnalyticsTracker
import com.dmytrosamoilov.offhand.core.device.DeviceCapability
import com.dmytrosamoilov.offhand.core.device.DeviceCapabilityChecker
import com.dmytrosamoilov.offhand.feature.onboarding.domain.usecase.CompleteOnboardingUseCase
import com.dmytrosamoilov.offhand.feature.onboarding.domain.usecase.SetAppLockEnabledUseCase
import io.mockk.coVerify
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
    private val weakDevice =
        DeviceCapability(totalRamMb = 4 * 1024, availableRamMb = 2 * 1024, cpuCores = 4)

    private val deviceCapabilityChecker: DeviceCapabilityChecker = mockk()
    private val setAppLockEnabled: SetAppLockEnabledUseCase = mockk(relaxed = true)
    private val completeOnboarding: CompleteOnboardingUseCase = mockk(relaxed = true)
    private val modelDownloadLauncher: ModelDownloadLauncher = mockk(relaxed = true)
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

    private fun viewModel(capability: DeviceCapability): OnboardingViewModel {
        every { deviceCapabilityChecker.snapshot() } returns capability
        val viewModel = OnboardingViewModel(
            modelDownloadLauncher = modelDownloadLauncher,
            deviceCapabilityChecker = deviceCapabilityChecker,
            setAppLockEnabled = setAppLockEnabled,
            completeOnboarding = completeOnboarding,
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
        assertEquals("4.0", state.deviceSpecs?.totalRamGb)
        assertEquals("5.0", state.deviceSpecs?.requiredRamGb)
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
}
