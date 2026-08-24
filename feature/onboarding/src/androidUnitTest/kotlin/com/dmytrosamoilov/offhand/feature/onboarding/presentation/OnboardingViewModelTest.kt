package com.dmytrosamoilov.offhand.feature.onboarding.presentation

import com.dmytrosamoilov.offhand.core.ai.api.AvailableModel
import com.dmytrosamoilov.offhand.core.ai.api.HardwareBackend
import com.dmytrosamoilov.offhand.core.ai.api.ModelFamily
import com.dmytrosamoilov.offhand.core.ai.api.ModelManager
import com.dmytrosamoilov.offhand.core.common.ModelDownloadController
import com.dmytrosamoilov.offhand.core.device.DeviceCapability
import com.dmytrosamoilov.offhand.core.device.DeviceCapabilityChecker
import com.dmytrosamoilov.offhand.core.data.domain.NotePreset
import com.dmytrosamoilov.offhand.core.security.AppLockManager
import com.dmytrosamoilov.offhand.feature.onboarding.domain.usecase.CompleteOnboardingUseCase
import com.dmytrosamoilov.offhand.feature.onboarding.domain.usecase.SetAppLockEnabledUseCase
import com.dmytrosamoilov.offhand.feature.onboarding.domain.usecase.SetNotePresetUseCase
import com.dmytrosamoilov.offhand.feature.onboarding.domain.usecase.SetTelemetryConsentUseCase
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
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

    private val testModel = AvailableModel(
        id = "gemma-4-e2b",
        displayName = "Gemma 4 E2B",
        description = "test",
        modelId = "litert-community/gemma-4-E2B-it-litert-lm",
        modelFile = "gemma-4-E2B-it.litertlm",
        commitHash = "abc",
        sizeInBytes = 2_056_729_520,
        family = ModelFamily.GEMMA4,
        hardwareBackend = HardwareBackend.CPU,
        maxTokens = 4096,
        topK = 64,
        topP = 0.95f,
        temperature = 1.0f,
    )

    private val deviceCapabilityChecker: DeviceCapabilityChecker = mockk()
    private val modelManager: ModelManager = mockk()
    private val appLockManager: AppLockManager = mockk()
    private val setAppLockEnabled: SetAppLockEnabledUseCase = mockk(relaxed = true)
    private val setTelemetryConsent: SetTelemetryConsentUseCase = mockk(relaxed = true)
    private val setNotePreset: SetNotePresetUseCase = mockk(relaxed = true)
    private val completeOnboarding: CompleteOnboardingUseCase = mockk(relaxed = true)
    private val modelDownloadController: ModelDownloadController = mockk(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { modelManager.model } returns testModel
        every { modelManager.speechModelSizeInBytes } returns 375_485_327L
        every { appLockManager.isDeviceSecure } returns true
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    private fun viewModel(
        stepPolicy: OnboardingStepPolicy = OnboardingStepPolicy(asksNotificationPermission = false),
    ): OnboardingViewModel = OnboardingViewModel(
        modelDownloadController = modelDownloadController,
        deviceCapabilityChecker = deviceCapabilityChecker,
        modelManager = modelManager,
        appLockManager = appLockManager,
        setAppLockEnabled = setAppLockEnabled,
        setTelemetryConsent = setTelemetryConsent,
        setNotePreset = setNotePreset,
        completeOnboarding = completeOnboarding,
        stepPolicy = stepPolicy,
    )

    private fun capableViewModel(): OnboardingViewModel {
        every { deviceCapabilityChecker.snapshot() } returns capableDevice
        val viewModel = viewModel()
        dispatcher.scheduler.advanceUntilIdle()
        return viewModel
    }

    private fun lockStepViewModel(): OnboardingViewModel = capableViewModel().apply {
        onPrivacyContinue()
        onNoteStyleContinue()
        dispatcher.scheduler.advanceUntilIdle()
    }

    private fun consentStepViewModel(): OnboardingViewModel = lockStepViewModel().apply {
        onDeviceLockContinue()
        dispatcher.scheduler.advanceUntilIdle()
    }

    @Test
    fun `weak device lands on incompatible step with specs`() = runTest(dispatcher) {
        every { deviceCapabilityChecker.snapshot() } returns weakDevice

        val viewModel = viewModel()
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(OnboardingStep.DEVICE_INCOMPATIBLE, state.step)
        assertEquals("4.0", state.deviceSpecs?.totalRamGb)
        assertEquals("5.0", state.deviceSpecs?.requiredRamGb)
    }

    @Test
    fun `capable device lands on privacy step with download size`() = runTest(dispatcher) {
        val viewModel = capableViewModel()

        assertEquals(OnboardingStep.PRIVACY, viewModel.uiState.value.step)
        assertEquals("2.3", viewModel.uiState.value.downloadSizeGb)
        assertEquals(0, viewModel.uiState.value.currentPage)
        assertEquals(5, viewModel.uiState.value.pageCount)
    }

    @Test
    fun `lock page stays in the flow on an unsecured device`() = runTest(dispatcher) {
        every { appLockManager.isDeviceSecure } returns false

        val viewModel = capableViewModel()

        assertEquals(5, viewModel.uiState.value.pageCount)
        assertEquals(false, viewModel.uiState.value.isDeviceSecure)
    }

    @Test
    fun `page position advances with each step`() = runTest(dispatcher) {
        val viewModel = capableViewModel()

        viewModel.onPrivacyContinue()
        assertEquals(1, viewModel.uiState.value.currentPage)

        viewModel.onNoteStyleContinue()
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(2, viewModel.uiState.value.currentPage)

        viewModel.onDeviceLockContinue()
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(3, viewModel.uiState.value.currentPage)

        viewModel.onConsentContinue()
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(4, viewModel.uiState.value.currentPage)
        assertEquals(OnboardingStep.MODEL_DOWNLOAD, viewModel.uiState.value.step)
    }

    @Test
    fun `notification policy inserts the notifications step before the download`() =
        runTest(dispatcher) {
            every { deviceCapabilityChecker.snapshot() } returns capableDevice
            val viewModel = viewModel(OnboardingStepPolicy(asksNotificationPermission = true))
            dispatcher.scheduler.advanceUntilIdle()

            assertEquals(6, viewModel.uiState.value.pageCount)

            viewModel.onPrivacyContinue()
            viewModel.onNoteStyleContinue()
            dispatcher.scheduler.advanceUntilIdle()
            viewModel.onDeviceLockContinue()
            dispatcher.scheduler.advanceUntilIdle()
            viewModel.onConsentContinue()
            dispatcher.scheduler.advanceUntilIdle()
            assertEquals(OnboardingStep.NOTIFICATIONS, viewModel.uiState.value.step)
            assertEquals(4, viewModel.uiState.value.currentPage)

            viewModel.onNotificationsContinue()
            assertEquals(OnboardingStep.MODEL_DOWNLOAD, viewModel.uiState.value.step)
            assertEquals(5, viewModel.uiState.value.currentPage)
        }

    @Test
    fun `privacy continue moves to note style step`() = runTest(dispatcher) {
        val viewModel = capableViewModel()

        viewModel.onPrivacyContinue()

        assertEquals(OnboardingStep.NOTE_STYLE, viewModel.uiState.value.step)
        assertEquals(NotePreset.SUMMARY, viewModel.uiState.value.notePreset)
    }

    @Test
    fun `note style continue persists choice and moves to lock step`() = runTest(dispatcher) {
        val viewModel = capableViewModel()
        viewModel.onPrivacyContinue()

        viewModel.onNoteStyleSelected(NotePreset.VISIT)
        viewModel.onNoteStyleContinue()
        dispatcher.scheduler.advanceUntilIdle()

        coVerify { setNotePreset(NotePreset.VISIT) }
        assertEquals(OnboardingStep.DEVICE_LOCK, viewModel.uiState.value.step)
    }

    @Test
    fun `note style continue shows lock step on unsecured device`() = runTest(dispatcher) {
        every { appLockManager.isDeviceSecure } returns false
        val viewModel = capableViewModel()
        viewModel.onPrivacyContinue()

        viewModel.onNoteStyleContinue()
        dispatcher.scheduler.advanceUntilIdle()

        coVerify { setNotePreset(NotePreset.SUMMARY) }
        assertEquals(OnboardingStep.DEVICE_LOCK, viewModel.uiState.value.step)
    }

    @Test
    fun `lock step is opted into by default and persists on continue`() = runTest(dispatcher) {
        val viewModel = lockStepViewModel()

        assertEquals(true, viewModel.uiState.value.isAppLockEnabled)
        viewModel.onDeviceLockContinue()
        dispatcher.scheduler.advanceUntilIdle()

        coVerify { setAppLockEnabled(true) }
        assertEquals(OnboardingStep.TELEMETRY_CONSENT, viewModel.uiState.value.step)
    }

    @Test
    fun `declining the lock persists the refusal`() = runTest(dispatcher) {
        val viewModel = lockStepViewModel()

        viewModel.onAppLockToggled(false)
        viewModel.onDeviceLockContinue()
        dispatcher.scheduler.advanceUntilIdle()

        coVerify { setAppLockEnabled(false) }
        assertEquals(OnboardingStep.TELEMETRY_CONSENT, viewModel.uiState.value.step)
    }

    @Test
    fun `unsecured device cannot opt in however the toggle is left`() = runTest(dispatcher) {
        every { appLockManager.isDeviceSecure } returns false
        val viewModel = lockStepViewModel()

        viewModel.onAppLockToggled(true)
        viewModel.onDeviceLockContinue()
        dispatcher.scheduler.advanceUntilIdle()

        coVerify { setAppLockEnabled(false) }
        assertEquals(OnboardingStep.TELEMETRY_CONSENT, viewModel.uiState.value.step)
    }

    @Test
    fun `lock recheck turns the nudge into the opt-in once a passcode exists`() =
        runTest(dispatcher) {
            every { appLockManager.isDeviceSecure } returns false
            val viewModel = lockStepViewModel()

            every { appLockManager.isDeviceSecure } returns true
            viewModel.onDeviceLockRecheck()

            assertEquals(OnboardingStep.DEVICE_LOCK, viewModel.uiState.value.step)
            assertEquals(true, viewModel.uiState.value.isDeviceSecure)
        }

    @Test
    fun `lock recheck keeps step while device stays unsecured`() = runTest(dispatcher) {
        every { appLockManager.isDeviceSecure } returns false
        val viewModel = lockStepViewModel()

        viewModel.onDeviceLockRecheck()

        assertEquals(OnboardingStep.DEVICE_LOCK, viewModel.uiState.value.step)
        assertEquals(false, viewModel.uiState.value.isDeviceSecure)
    }

    @Test
    fun `lock recheck outside lock step changes nothing`() = runTest(dispatcher) {
        val viewModel = capableViewModel()

        viewModel.onDeviceLockRecheck()

        assertEquals(OnboardingStep.PRIVACY, viewModel.uiState.value.step)
    }

    @Test
    fun `telemetry is enabled by default and consent continue persists it`() = runTest(dispatcher) {
        val viewModel = consentStepViewModel()

        assertEquals(true, viewModel.uiState.value.isTelemetryEnabled)
        viewModel.onConsentContinue()
        dispatcher.scheduler.advanceUntilIdle()

        coVerify { setTelemetryConsent(true) }
        assertEquals(OnboardingStep.MODEL_DOWNLOAD, viewModel.uiState.value.step)
        coVerify(exactly = 0) { completeOnboarding() }
    }

    @Test
    fun `toggling telemetry off persists the refusal on continue`() = runTest(dispatcher) {
        val viewModel = consentStepViewModel()

        viewModel.onTelemetryToggled(false)
        viewModel.onConsentContinue()
        dispatcher.scheduler.advanceUntilIdle()

        coVerify { setTelemetryConsent(false) }
        assertEquals(OnboardingStep.MODEL_DOWNLOAD, viewModel.uiState.value.step)
    }

    @Test
    fun `swiping back returns to an earlier page without new persistence`() = runTest(dispatcher) {
        val viewModel = consentStepViewModel()

        viewModel.onPageSelected(0)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(OnboardingStep.PRIVACY, viewModel.uiState.value.step)
        assertEquals(0, viewModel.uiState.value.currentPage)
        assertEquals(3, viewModel.uiState.value.furthestPage)
        coVerify(exactly = 1) { setNotePreset(any()) }
        coVerify(exactly = 1) { setAppLockEnabled(any()) }
    }

    @Test
    fun `swiping forward replays the choices made on revisited pages`() = runTest(dispatcher) {
        val viewModel = consentStepViewModel()
        viewModel.onPageSelected(1)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onNoteStyleSelected(NotePreset.LEGAL)
        viewModel.onPageSelected(3)
        dispatcher.scheduler.advanceUntilIdle()

        coVerify { setNotePreset(NotePreset.LEGAL) }
        coVerify(exactly = 2) { setAppLockEnabled(true) }
        assertEquals(OnboardingStep.TELEMETRY_CONSENT, viewModel.uiState.value.step)
        assertEquals(3, viewModel.uiState.value.currentPage)
    }

    @Test
    fun `page selection cannot pass the furthest visited page`() = runTest(dispatcher) {
        val viewModel = capableViewModel()
        viewModel.onPrivacyContinue()

        viewModel.onPageSelected(4)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(OnboardingStep.NOTE_STYLE, viewModel.uiState.value.step)
        assertEquals(1, viewModel.uiState.value.currentPage)
        assertEquals(1, viewModel.uiState.value.furthestPage)
    }

    @Test
    fun `continue from a revisited page walks forward without losing the frontier`() =
        runTest(dispatcher) {
            val viewModel = consentStepViewModel()
            viewModel.onPageSelected(0)
            dispatcher.scheduler.advanceUntilIdle()

            viewModel.onPrivacyContinue()

            assertEquals(OnboardingStep.NOTE_STYLE, viewModel.uiState.value.step)
            assertEquals(1, viewModel.uiState.value.currentPage)
            assertEquals(3, viewModel.uiState.value.furthestPage)
        }

    @Test
    fun `download continue starts download and completes onboarding`() = runTest(dispatcher) {
        val viewModel = consentStepViewModel()
        viewModel.onConsentContinue()
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onDownloadContinue()
        dispatcher.scheduler.advanceUntilIdle()

        verify { modelDownloadController.start() }
        coVerify { completeOnboarding() }
    }
}
