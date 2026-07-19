package com.dmytrosamoilov.offhand.root

data class RootUiState(
    val phase: RootPhase = RootPhase.LOADING,
    val isDynamicColorEnabled: Boolean = false,
)

enum class RootPhase {
    LOADING,
    ONBOARDING,
    LOCKED,
    READY,
}
