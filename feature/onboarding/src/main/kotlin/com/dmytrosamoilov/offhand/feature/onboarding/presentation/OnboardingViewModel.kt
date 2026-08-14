package com.dmytrosamoilov.offhand.feature.onboarding.presentation

import android.content.Context
import com.dmytrosamoilov.offhand.core.ai.api.ModelManager
import com.dmytrosamoilov.offhand.core.common.BaseViewModel
import com.dmytrosamoilov.offhand.core.device.DeviceCapabilityChecker
import com.dmytrosamoilov.offhand.core.device.isLocalLlmCapable
import com.dmytrosamoilov.offhand.core.security.AppLockManager
import com.dmytrosamoilov.offhand.core.ui.component.NotePresetOption
import com.dmytrosamoilov.offhand.feature.onboarding.domain.usecase.CompleteOnboardingUseCase
import com.dmytrosamoilov.offhand.feature.onboarding.domain.usecase.SetNotePresetUseCase
import com.dmytrosamoilov.offhand.feature.onboarding.domain.usecase.SetTelemetryConsentUseCase
import com.dmytrosamoilov.offhand.feature.onboarding.service.ModelDownloadService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val deviceCapabilityChecker: DeviceCapabilityChecker,
    private val modelManager: ModelManager,
    private val appLockManager: AppLockManager,
    private val setTelemetryConsent: SetTelemetryConsentUseCase,
    private val setNotePreset: SetNotePresetUseCase,
    private val completeOnboarding: CompleteOnboardingUseCase,
) : BaseViewModel() {

    private val mutableUiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = mutableUiState.asStateFlow()

    private var pages: List<OnboardingStep> = emptyList()

    init {
        evaluateDevice()
    }

    fun onPrivacyContinue() {
        moveToNextPage()
    }

    fun onNoteStyleSelected(option: NotePresetOption) {
        mutableUiState.update { it.copy(notePreset = option) }
    }

    fun onNoteStyleContinue() {
        launchSafely {
            setNotePreset(uiState.value.notePreset.toDomain())
            moveToNextPage()
        }
    }

    fun onDeviceLockSkipped() {
        moveToNextPage()
    }

    fun onDeviceLockRecheck() {
        if (uiState.value.step != OnboardingStep.DEVICE_LOCK) return
        if (!appLockManager.isDeviceSecure) return
        moveToNextPage()
    }

    fun onTelemetryToggled(granted: Boolean) {
        mutableUiState.update { it.copy(isTelemetryEnabled = granted) }
    }

    fun onConsentContinue() {
        launchSafely {
            setTelemetryConsent(uiState.value.isTelemetryEnabled)
            moveToNextPage()
        }
    }

    fun onDownloadContinue() {
        launchSafely {
            ModelDownloadService.start(context)
            completeOnboarding()
        }
    }

    private fun moveToNextPage() {
        val nextIndex = pages.indexOf(uiState.value.step) + 1
        val next = pages.getOrNull(nextIndex) ?: return
        mutableUiState.update { it.copy(step = next, currentPage = nextIndex) }
    }

    private fun buildPages(): List<OnboardingStep> = buildList {
        add(OnboardingStep.PRIVACY)
        add(OnboardingStep.NOTE_STYLE)
        if (!appLockManager.isDeviceSecure) add(OnboardingStep.DEVICE_LOCK)
        add(OnboardingStep.TELEMETRY_CONSENT)
        add(OnboardingStep.MODEL_DOWNLOAD)
    }

    private fun evaluateDevice() {
        val capability = deviceCapabilityChecker.snapshot()
        mutableUiState.update { current ->
            if (capability.isLocalLlmCapable()) {
                pages = buildPages()
                current.copy(
                    step = OnboardingStep.PRIVACY,
                    currentPage = 0,
                    pageCount = pages.size,
                    downloadSizeGb = formatDownloadSizeGb(
                        modelManager.model.sizeInBytes + modelManager.speechModelSizeInBytes,
                    ),
                )
            } else {
                current.copy(
                    step = OnboardingStep.DEVICE_INCOMPATIBLE,
                    deviceSpecs = capability.toDeviceSpecsUi(),
                )
            }
        }
    }
}
