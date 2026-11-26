package com.dmytrosamoilov.offhand.feature.onboarding.presentation

data class OnboardingUiState(
    val step: OnboardingStep = OnboardingStep.DEVICE_CHECK,
    val deviceSpecs: DeviceSpecsUi? = null,
    val isSummaryAvailable: Boolean = true,
)

enum class OnboardingStep {
    DEVICE_CHECK,
    ANALYTICS_CONSENT,
    DEVICE_INCOMPATIBLE,
    TRANSCRIPTS_ONLY,
    BACKGROUND_ACTIVITY,
    WELCOME,
}

enum class OnboardingChoice(val analyticsName: String) {
    ANALYTICS_SHARED("analytics_shared"),
    TRANSCRIPTS_ONLY("transcripts_only"),
    BATTERY_SETTINGS("battery_settings"),
    BATTERY_SKIPPED("battery_skipped"),
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
