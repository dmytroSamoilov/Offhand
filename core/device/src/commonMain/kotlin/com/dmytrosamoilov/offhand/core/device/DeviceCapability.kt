package com.dmytrosamoilov.offhand.core.device

data class DeviceCapability(
    val totalRamMb: Long,
    val availableRamMb: Long,
    val cpuCores: Int,
)

const val MIN_SUPPORTED_RAM_MB: Long = 1560L
const val MIN_TOTAL_RAM_MB: Long = 5L * 1024L
const val MIN_CPU_CORES: Int = 4
const val NOMINAL_MIN_SUPPORTED_GB: Int = 2

fun DeviceCapability.tier(): DeviceTier = when {
    cpuCores < MIN_CPU_CORES || totalRamMb < MIN_SUPPORTED_RAM_MB -> DeviceTier.UNSUPPORTED
    totalRamMb < MIN_TOTAL_RAM_MB -> DeviceTier.TRANSCRIPTS_ONLY
    else -> DeviceTier.FULL
}
