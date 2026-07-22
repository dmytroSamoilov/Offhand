package com.dmytrosamoilov.offhand.feature.onboarding.presentation

data class OnboardingUiState(
    val step: OnboardingStep = OnboardingStep.DEVICE_CHECK,
    val deviceSpecs: DeviceSpecsUi? = null,
    val downloadSizeGb: String = "",
)

enum class OnboardingStep {
    DEVICE_CHECK,
    DEVICE_INCOMPATIBLE,
    PRIVACY,
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
