package com.dmytrosamoilov.offhand.feature.onboarding.presentation

import com.dmytrosamoilov.offhand.core.ai.api.AvailableModel
import com.dmytrosamoilov.offhand.core.ai.api.HardwareBackend
import android.content.Context
import com.dmytrosamoilov.offhand.core.ai.api.ModelManager
import com.dmytrosamoilov.offhand.core.device.DeviceCapability
import com.dmytrosamoilov.offhand.core.device.DeviceCapabilityChecker
import com.dmytrosamoilov.offhand.feature.onboarding.domain.usecase.CompleteOnboardingUseCase
import com.dmytrosamoilov.offhand.feature.onboarding.domain.usecase.SetTelemetryConsentUseCase
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
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
        id = "qwen3-1.7b",
        displayName = "Qwen3 1.7B",
        description = "test",
        modelId = "litert-community/Qwen3-1.7B",
        modelFile = "Qwen3_1.7B.litertlm",
        commitHash = "abc",
        sizeInBytes = 2_056_729_520,
        hardwareBackend = HardwareBackend.CPU,
        maxTokens = 4096,
        topK = 20,
        topP = 0.95f,
        temperature = 0.6f,
    )

    private val deviceCapabilityChecker: DeviceCapabilityChecker = mockk()
    private val modelManager: ModelManager = mockk()
    private val setTelemetryConsent: SetTelemetryConsentUseCase = mockk(relaxed = true)
    private val completeOnboarding: CompleteOnboardingUseCase = mockk(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { modelManager.model } returns testModel
        every { modelManager.speechModelSizeInBytes } returns 375_485_327L
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(): OnboardingViewModel = OnboardingViewModel(
        context = mockk<Context>(relaxed = true),
        deviceCapabilityChecker = deviceCapabilityChecker,
        modelManager = modelManager,
        setTelemetryConsent = setTelemetryConsent,
        completeOnboarding = completeOnboarding,
    )

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
    fun `capable device lands on model download step`() = runTest(dispatcher) {
        every { deviceCapabilityChecker.snapshot() } returns capableDevice

        val viewModel = viewModel()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(OnboardingStep.MODEL_DOWNLOAD, viewModel.uiState.value.step)
        assertEquals("2.3", viewModel.uiState.value.downloadSizeGb)
    }

    @Test
    fun `consent choice persists preference and completes onboarding`() = runTest(dispatcher) {
        every { deviceCapabilityChecker.snapshot() } returns capableDevice

        val viewModel = viewModel()
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.onConsentChosen(true)
        dispatcher.scheduler.advanceUntilIdle()

        coVerify { setTelemetryConsent(true) }
        coVerify { completeOnboarding() }
    }
}
