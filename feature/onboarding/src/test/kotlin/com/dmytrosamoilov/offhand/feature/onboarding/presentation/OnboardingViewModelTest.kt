package com.dmytrosamoilov.offhand.feature.onboarding.presentation

import android.content.Context
import com.dmytrosamoilov.offhand.core.ai.api.AvailableModel
import com.dmytrosamoilov.offhand.core.ai.api.HardwareBackend
import com.dmytrosamoilov.offhand.core.ai.api.ModelFamily
import com.dmytrosamoilov.offhand.core.ai.api.ModelManager
import com.dmytrosamoilov.offhand.core.device.DeviceCapability
import com.dmytrosamoilov.offhand.core.device.DeviceCapabilityChecker
import com.dmytrosamoilov.offhand.core.security.AppLockManager
import com.dmytrosamoilov.offhand.feature.onboarding.domain.usecase.CompleteOnboardingUseCase
import com.dmytrosamoilov.offhand.feature.onboarding.domain.usecase.SetTelemetryConsentUseCase
import com.dmytrosamoilov.offhand.feature.onboarding.service.ModelDownloadService
import io.mockk.coVerify
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.mockkObject
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
    private val completeOnboarding: CompleteOnboardingUseCase = mockk(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { modelManager.model } returns testModel
        every { modelManager.speechModelSizeInBytes } returns 375_485_327L
        every { appLockManager.isDeviceSecure } returns true
        mockkObject(ModelDownloadService.Companion)
        justRun { ModelDownloadService.start(any()) }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    private fun viewModel(): OnboardingViewModel = OnboardingViewModel(
        context = mockk<Context>(relaxed = true),
        deviceCapabilityChecker = deviceCapabilityChecker,
        modelManager = modelManager,
        appLockManager = appLockManager,
        setTelemetryConsent = setTelemetryConsent,
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
    }

    @Test
    fun `privacy continue skips lock step on secure device`() = runTest(dispatcher) {
        val viewModel = capableViewModel()

        viewModel.onPrivacyContinue()

        assertEquals(OnboardingStep.TELEMETRY_CONSENT, viewModel.uiState.value.step)
    }

    @Test
    fun `privacy continue shows lock step on unsecured device`() = runTest(dispatcher) {
        every { appLockManager.isDeviceSecure } returns false
        val viewModel = capableViewModel()

        viewModel.onPrivacyContinue()

        assertEquals(OnboardingStep.DEVICE_LOCK, viewModel.uiState.value.step)
    }

    @Test
    fun `skipping lock step moves to consent`() = runTest(dispatcher) {
        every { appLockManager.isDeviceSecure } returns false
        val viewModel = capableViewModel()
        viewModel.onPrivacyContinue()

        viewModel.onDeviceLockSkipped()

        assertEquals(OnboardingStep.TELEMETRY_CONSENT, viewModel.uiState.value.step)
    }

    @Test
    fun `lock recheck advances once device becomes secure`() = runTest(dispatcher) {
        every { appLockManager.isDeviceSecure } returns false
        val viewModel = capableViewModel()
        viewModel.onPrivacyContinue()

        every { appLockManager.isDeviceSecure } returns true
        viewModel.onDeviceLockRecheck()

        assertEquals(OnboardingStep.TELEMETRY_CONSENT, viewModel.uiState.value.step)
    }

    @Test
    fun `lock recheck keeps step while device stays unsecured`() = runTest(dispatcher) {
        every { appLockManager.isDeviceSecure } returns false
        val viewModel = capableViewModel()
        viewModel.onPrivacyContinue()

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
    fun `consent choice persists preference and moves to download step`() = runTest(dispatcher) {
        val viewModel = capableViewModel()
        viewModel.onPrivacyContinue()

        viewModel.onConsentChosen(true)
        dispatcher.scheduler.advanceUntilIdle()

        coVerify { setTelemetryConsent(true) }
        assertEquals(OnboardingStep.MODEL_DOWNLOAD, viewModel.uiState.value.step)
        coVerify(exactly = 0) { completeOnboarding() }
    }

    @Test
    fun `download continue starts download and completes onboarding`() = runTest(dispatcher) {
        val viewModel = capableViewModel()
        viewModel.onPrivacyContinue()
        viewModel.onConsentChosen(false)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onDownloadContinue()
        dispatcher.scheduler.advanceUntilIdle()

        verify { ModelDownloadService.start(any()) }
        coVerify { completeOnboarding() }
    }
}
