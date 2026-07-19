package com.dmytrosamoilov.offhand.feature.settings.presentation

import com.dmytrosamoilov.offhand.core.ai.api.AvailableModel
import com.dmytrosamoilov.offhand.core.ai.api.HardwareBackend
import com.dmytrosamoilov.offhand.core.ai.api.ModelManager
import com.dmytrosamoilov.offhand.core.ai.api.ModelState
import com.dmytrosamoilov.offhand.core.common.BuildInfo
import com.dmytrosamoilov.offhand.feature.settings.domain.usecase.ObserveDeveloperOptionsUseCase
import com.dmytrosamoilov.offhand.feature.settings.domain.usecase.ObserveDynamicColorUseCase
import com.dmytrosamoilov.offhand.feature.settings.domain.usecase.ObserveTelemetryConsentUseCase
import com.dmytrosamoilov.offhand.feature.settings.domain.usecase.SetDeveloperOptionsUseCase
import com.dmytrosamoilov.offhand.feature.settings.domain.usecase.SetDynamicColorUseCase
import com.dmytrosamoilov.offhand.feature.settings.domain.usecase.SetTelemetryConsentUseCase
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val dispatcher = StandardTestDispatcher()

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

    private val modelStateFlow = MutableStateFlow<ModelState>(ModelState.Ready)
    private val activeBackendFlow = MutableStateFlow(HardwareBackend.CPU)
    private val modelOverrideIdFlow = MutableStateFlow<String?>(null)
    private val modelManager: ModelManager = mockk()
    private val setTelemetryConsent: SetTelemetryConsentUseCase = mockk(relaxed = true)
    private val observeTelemetryConsent: ObserveTelemetryConsentUseCase = mockk()
    private val setDynamicColor: SetDynamicColorUseCase = mockk(relaxed = true)
    private val observeDynamicColor: ObserveDynamicColorUseCase = mockk()
    private val setDeveloperOptions: SetDeveloperOptionsUseCase = mockk(relaxed = true)
    private val observeDeveloperOptions: ObserveDeveloperOptionsUseCase = mockk()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { modelManager.model } returns testModel
        every { modelManager.availableModels } returns listOf(testModel)
        every { modelManager.modelOverrideId } returns modelOverrideIdFlow
        every { modelManager.modelState } returns modelStateFlow
        every { modelManager.activeBackend } returns activeBackendFlow
        coJustRun { modelManager.setHardwareBackend(any()) }
        coJustRun { modelManager.setModelOverride(any()) }
        coJustRun { modelManager.deleteModel() }
        every { observeTelemetryConsent() } returns flowOf(true)
        every { observeDynamicColor() } returns flowOf(false)
        every { observeDeveloperOptions() } returns flowOf(false)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(isDebugBuild: Boolean = true) = SettingsViewModel(
        modelManager = modelManager,
        observeTelemetryConsent = observeTelemetryConsent,
        setTelemetryConsent = setTelemetryConsent,
        observeDynamicColor = observeDynamicColor,
        setDynamicColor = setDynamicColor,
        observeDeveloperOptions = observeDeveloperOptions,
        setDeveloperOptions = setDeveloperOptions,
        buildInfo = BuildInfo(isDebugBuild = isDebugBuild),
    )

    @Test
    fun `state reflects model info, backend and telemetry`() = runTest(dispatcher) {
        val viewModel = viewModel()
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Qwen3 1.7B", state.model.displayName)
        assertEquals("1.9", state.model.sizeGb)
        assertEquals(ModelStatusUi.READY, state.model.status)
        assertEquals(BackendOptionUi.CPU, state.selectedBackend)
        assertTrue(state.isTelemetryEnabled)
    }

    @Test
    fun `state exposes catalog options with automatic selection`() = runTest(dispatcher) {
        val viewModel = viewModel()
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(listOf("qwen3-1.7b"), state.modelOptions.map { it.id })
        assertEquals(null, state.selectedModelId)
    }

    @Test
    fun `selecting a model persists the override`() = runTest(dispatcher) {
        val viewModel = viewModel()
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onModelSelected("qwen3-1.7b")
        dispatcher.scheduler.advanceUntilIdle()

        coVerify { modelManager.setModelOverride("qwen3-1.7b") }
    }

    @Test
    fun `selecting gpu persists backend`() = runTest(dispatcher) {
        val viewModel = viewModel()
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onBackendSelected(BackendOptionUi.GPU)
        dispatcher.scheduler.advanceUntilIdle()

        coVerify { modelManager.setHardwareBackend(HardwareBackend.GPU) }
    }

    @Test
    fun `download progress maps to percent`() = runTest(dispatcher) {
        val viewModel = viewModel()
        dispatcher.scheduler.advanceUntilIdle()

        modelStateFlow.value = ModelState.Downloading(0.42f, 1_000, 2_400)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(ModelStatusUi.DOWNLOADING, viewModel.uiState.value.model.status)
        assertEquals(42, viewModel.uiState.value.model.downloadPercent)
    }

    @Test
    fun `confirmed delete removes model`() = runTest(dispatcher) {
        val viewModel = viewModel()
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onDeleteModelRequested()
        assertTrue(viewModel.uiState.value.isDeleteModelConfirmationVisible)
        viewModel.onDeleteModelConfirmed()
        dispatcher.scheduler.advanceUntilIdle()

        coVerify { modelManager.deleteModel() }
    }

    @Test
    fun `telemetry toggle persists consent`() = runTest(dispatcher) {
        val viewModel = viewModel()
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onTelemetryChanged(false)
        dispatcher.scheduler.advanceUntilIdle()

        coVerify { setTelemetryConsent(false) }
    }

    @Test
    fun `dynamic color toggle persists preference`() = runTest(dispatcher) {
        val viewModel = viewModel()
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onDynamicColorChanged(true)
        dispatcher.scheduler.advanceUntilIdle()

        coVerify { setDynamicColor(true) }
    }

    @Test
    fun `developer section is visible on debug builds`() = runTest(dispatcher) {
        val viewModel = viewModel(isDebugBuild = true)
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isDeveloperSectionVisible)
    }

    @Test
    fun `developer section is hidden on release builds`() = runTest(dispatcher) {
        val viewModel = viewModel(isDebugBuild = false)
        dispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isDeveloperSectionVisible)
    }

    @Test
    fun `developer options toggle persists preference`() = runTest(dispatcher) {
        val viewModel = viewModel()
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onDeveloperOptionsChanged(true)
        dispatcher.scheduler.advanceUntilIdle()

        coVerify { setDeveloperOptions(true) }
    }

    @Test
    fun `downloaded engine-idle state shows model as ready`() = runTest(dispatcher) {
        val viewModel = viewModel()
        dispatcher.scheduler.advanceUntilIdle()

        modelStateFlow.value = ModelState.Downloaded
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(ModelStatusUi.READY, viewModel.uiState.value.model.status)
    }
}
