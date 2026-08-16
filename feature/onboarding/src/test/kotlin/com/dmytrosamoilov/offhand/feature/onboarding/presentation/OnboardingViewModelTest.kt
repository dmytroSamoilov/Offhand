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

    private fun viewModel(): OnboardingViewModel = OnboardingViewModel(
        modelDownloadController = modelDownloadController,
        deviceCapabilityChecker = deviceCapabilityChecker,
        modelManager = modelManager,
        appLockManager = appLockManager,
        setTelemetryConsent = setTelemetryConsent,
        setNotePreset = setNotePreset,
        completeOnboarding = completeOnboarding,
    )

    private fun capableViewModel(): OnboardingViewModel {
        every { deviceCapabilityChecker.snapshot() } returns capableDevice
        val viewModel = viewModel()
        dispatcher.scheduler.advanceUntilIdle()
        return viewModel
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
        assertEquals(4, viewModel.uiState.value.pageCount)
    }

    @Test
    fun `unsecured device adds the lock page to the flow`() = runTest(dispatcher) {
        every { appLockManager.isDeviceSecure } returns false

        val viewModel = capableViewModel()

        assertEquals(5, viewModel.uiState.value.pageCount)
    }

    @Test
    fun `page position advances with each step`() = runTest(dispatcher) {
        val viewModel = capableViewModel()

        viewModel.onPrivacyContinue()
        assertEquals(1, viewModel.uiState.value.currentPage)

        viewModel.onNoteStyleContinue()
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(2, viewModel.uiState.value.currentPage)

        viewModel.onConsentContinue()
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(3, viewModel.uiState.value.currentPage)
        assertEquals(OnboardingStep.MODEL_DOWNLOAD, viewModel.uiState.value.step)
    }

    @Test
    fun `privacy continue moves to note style step`() = runTest(dispatcher) {
        val viewModel = capableViewModel()

        viewModel.onPrivacyContinue()

        assertEquals(OnboardingStep.NOTE_STYLE, viewModel.uiState.value.step)
        assertEquals(NotePreset.SUMMARY, viewModel.uiState.value.notePreset)
    }

    @Test
    fun `note style continue persists choice and skips lock on secure device`() = runTest(dispatcher) {
        val viewModel = capableViewModel()
        viewModel.onPrivacyContinue()

        viewModel.onNoteStyleSelected(NotePreset.VISIT)
        viewModel.onNoteStyleContinue()
        dispatcher.scheduler.advanceUntilIdle()

        coVerify { setNotePreset(NotePreset.VISIT) }
        assertEquals(OnboardingStep.TELEMETRY_CONSENT, viewModel.uiState.value.step)
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
    fun `skipping lock step moves to consent`() = runTest(dispatcher) {
        every { appLockManager.isDeviceSecure } returns false
        val viewModel = capableViewModel()
        viewModel.onPrivacyContinue()
        viewModel.onNoteStyleContinue()
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onDeviceLockSkipped()

        assertEquals(OnboardingStep.TELEMETRY_CONSENT, viewModel.uiState.value.step)
    }

    @Test
    fun `lock recheck advances once device becomes secure`() = runTest(dispatcher) {
        every { appLockManager.isDeviceSecure } returns false
        val viewModel = capableViewModel()
        viewModel.onPrivacyContinue()
        viewModel.onNoteStyleContinue()
        dispatcher.scheduler.advanceUntilIdle()

        every { appLockManager.isDeviceSecure } returns true
        viewModel.onDeviceLockRecheck()

        assertEquals(OnboardingStep.TELEMETRY_CONSENT, viewModel.uiState.value.step)
    }

    @Test
    fun `lock recheck keeps step while device stays unsecured`() = runTest(dispatcher) {
        every { appLockManager.isDeviceSecure } returns false
        val viewModel = capableViewModel()
        viewModel.onPrivacyContinue()
        viewModel.onNoteStyleContinue()
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onDeviceLockRecheck()

        assertEquals(OnboardingStep.DEVICE_LOCK, viewModel.uiState.value.step)
    }

    @Test
    fun `lock recheck outside lock step changes nothing`() = runTest(dispatcher) {
        val viewModel = capableViewModel()

        viewModel.onDeviceLockRecheck()

        assertEquals(OnboardingStep.PRIVACY, viewModel.uiState.value.step)
    }

    @Test
    fun `telemetry is enabled by default and consent continue persists it`() = runTest(dispatcher) {
        val viewModel = capableViewModel()
        viewModel.onPrivacyContinue()
        viewModel.onNoteStyleContinue()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(true, viewModel.uiState.value.isTelemetryEnabled)
        viewModel.onConsentContinue()
        dispatcher.scheduler.advanceUntilIdle()

        coVerify { setTelemetryConsent(true) }
        assertEquals(OnboardingStep.MODEL_DOWNLOAD, viewModel.uiState.value.step)
        coVerify(exactly = 0) { completeOnboarding() }
    }

    @Test
    fun `toggling telemetry off persists the refusal on continue`() = runTest(dispatcher) {
        val viewModel = capableViewModel()
        viewModel.onPrivacyContinue()
        viewModel.onNoteStyleContinue()
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onTelemetryToggled(false)
        viewModel.onConsentContinue()
        dispatcher.scheduler.advanceUntilIdle()

        coVerify { setTelemetryConsent(false) }
        assertEquals(OnboardingStep.MODEL_DOWNLOAD, viewModel.uiState.value.step)
    }

    @Test
    fun `download continue starts download and completes onboarding`() = runTest(dispatcher) {
        val viewModel = capableViewModel()
        viewModel.onPrivacyContinue()
        viewModel.onNoteStyleContinue()
        viewModel.onConsentContinue()
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onDownloadContinue()
        dispatcher.scheduler.advanceUntilIdle()

        verify { modelDownloadController.start() }
        coVerify { completeOnboarding() }
    }
}
