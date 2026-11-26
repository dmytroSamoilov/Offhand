package com.dmytrosamoilov.offhand.core.device

enum class DeviceTier {
    UNSUPPORTED,
    TRANSCRIPTS_ONLY,
    FULL,
    ;

    val writesSummaries: Boolean get() = this == FULL
}

// A phone's memory and cores never change while the app runs, so the tier is
// worked out once: download progress asks for it many times a second. The
// smoke tests switch the tier after start and build one that asks every time.
class DeviceTierProvider(
    private val checker: DeviceCapabilityChecker,
    private val isRemembered: Boolean = true,
) {
    private val remembered: DeviceTier by lazy { checker.snapshot().tier() }

    fun current(): DeviceTier = if (isRemembered) remembered else checker.snapshot().tier()

    fun writesSummaries(): Boolean = current().writesSummaries
}
