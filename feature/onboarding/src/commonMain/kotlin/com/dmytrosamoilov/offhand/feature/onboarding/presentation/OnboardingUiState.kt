package com.dmytrosamoilov.offhand.feature.onboarding.presentation

data class OnboardingUiState(
    val step: OnboardingStep = OnboardingStep.DEVICE_CHECK,
    val deviceSpecs: DeviceSpecsUi? = null,
    val isSummaryAvailable: Boolean = true,
)

enum class OnboardingStep {
    DEVICE_CHECK,
    DEVICE_INCOMPATIBLE,
    TRANSCRIPTS_ONLY,
    WELCOME,
}

enum class OnboardingChoice(val analyticsName: String) {
    TRANSCRIPTS_ONLY("transcripts_only"),
    RECORD_FIRST_NOTE("record_first_note"),
    LOOK_AROUND("look_around"),
}

data class DeviceSpecsUi(
    val totalRamGb: String,
    val requiredRamGb: String,
    val isRamSatisfied: Boolean,
    val cpuCores: Int,
    val requiredCpuCores: Int,
    val isCoresSatisfied: Boolean,
)
