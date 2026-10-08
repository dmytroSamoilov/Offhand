package com.dmytrosamoilov.offhand.feature.onboarding.presentation

import com.dmytrosamoilov.offhand.core.common.DecimalFormatter
import com.dmytrosamoilov.offhand.core.device.DeviceCapability
import com.dmytrosamoilov.offhand.core.device.MIN_CPU_CORES
import com.dmytrosamoilov.offhand.core.device.MIN_TOTAL_RAM_MB

private const val MB_PER_GB = 1024f

internal fun DeviceCapability.toDeviceSpecsUi(): DeviceSpecsUi = DeviceSpecsUi(
    totalRamGb = formatGb(totalRamMb),
    requiredRamGb = formatGb(MIN_TOTAL_RAM_MB),
    isRamSatisfied = totalRamMb >= MIN_TOTAL_RAM_MB,
    cpuCores = cpuCores,
    requiredCpuCores = MIN_CPU_CORES,
    isCoresSatisfied = cpuCores >= MIN_CPU_CORES,
)

private fun formatGb(ramMb: Long): String = DecimalFormatter.oneDecimal(ramMb / MB_PER_GB)
