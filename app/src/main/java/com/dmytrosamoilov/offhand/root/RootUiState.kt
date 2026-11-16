package com.dmytrosamoilov.offhand.root

import com.dmytrosamoilov.offhand.core.data.domain.ProTrialNotice

data class RootUiState(
    val phase: RootPhase = RootPhase.LOADING,
    val isDynamicColorEnabled: Boolean = false,
    val isEarlyAdopterThanksShown: Boolean = false,
    val trialNotice: ProTrialNotice? = null,
)

enum class RootPhase {
    LOADING,
    ONBOARDING,
    LOCKED,
    READY,
}
