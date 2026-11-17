package com.dmytrosamoilov.offhand.core.device

enum class DeviceTier {
    UNSUPPORTED,
    TRANSCRIPTS_ONLY,
    FULL,
    ;

    val writesSummaries: Boolean get() = this == FULL
}

class DeviceTierProvider(
    private val checker: DeviceCapabilityChecker,
) {
    fun current(): DeviceTier = checker.snapshot().tier()

    fun writesSummaries(): Boolean = current().writesSummaries
}
