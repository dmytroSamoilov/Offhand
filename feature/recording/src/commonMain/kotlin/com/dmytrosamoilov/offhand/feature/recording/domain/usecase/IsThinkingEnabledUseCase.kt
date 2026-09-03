package com.dmytrosamoilov.offhand.feature.recording.domain.usecase

// Compile-time switch for the thinking block in the polish pass — flip it
// here. Wrapped in a use case so it can later be decided from device
// capabilities, for example available RAM.
private const val THINKING_ENABLED = false

class IsThinkingEnabledUseCase {

    operator fun invoke(): Boolean = THINKING_ENABLED
}
