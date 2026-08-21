package com.dmytrosamoilov.offhand.feature.onboarding.presentation

import com.dmytrosamoilov.offhand.core.data.domain.NotePreset

data class OnboardingUiState(
    val step: OnboardingStep = OnboardingStep.DEVICE_CHECK,
    val deviceSpecs: DeviceSpecsUi? = null,
    val downloadSizeGb: String = "",
    val notePreset: NotePreset = NotePreset.DEFAULT,
    val isDeviceSecure: Boolean = false,
    val isAppLockEnabled: Boolean = true,
    val isTelemetryEnabled: Boolean = true,
    val currentPage: Int = 0,
    val pageCount: Int = 0,
)

enum class OnboardingStep {
    DEVICE_CHECK,
    DEVICE_INCOMPATIBLE,
    PRIVACY,
    NOTE_STYLE,
    DEVICE_LOCK,
    TELEMETRY_CONSENT,
    MODEL_DOWNLOAD,
}

data class DeviceSpecsUi(
    val totalRamGb: String,
    val requiredRamGb: String,
    val isRamSatisfied: Boolean,
    val cpuCores: Int,
    val requiredCpuCores: Int,
    val isCoresSatisfied: Boolean,
)
