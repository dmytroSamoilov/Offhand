package com.dmytrosamoilov.offhand.core.device

data class DeviceCapability(
    val totalRamMb: Long,
    val availableRamMb: Long,
    val cpuCores: Int,
)

const val MIN_TOTAL_RAM_MB: Long = 5L * 1024L
const val MIN_CPU_CORES: Int = 4

fun DeviceCapability.isLocalLlmCapable(): Boolean =
    totalRamMb >= MIN_TOTAL_RAM_MB && cpuCores >= MIN_CPU_CORES
