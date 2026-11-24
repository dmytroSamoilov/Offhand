package com.dmytrosamoilov.offhand.feature.onboarding.presentation

import com.dmytrosamoilov.offhand.core.common.DecimalFormatter
import com.dmytrosamoilov.offhand.core.device.DeviceCapability
import com.dmytrosamoilov.offhand.core.device.MIN_CPU_CORES
import com.dmytrosamoilov.offhand.core.device.MIN_SUPPORTED_RAM_MB
import com.dmytrosamoilov.offhand.core.device.NOMINAL_MIN_SUPPORTED_GB

private const val MB_PER_GB = 1024f

internal fun DeviceCapability.toDeviceSpecsUi(): DeviceSpecsUi = DeviceSpecsUi(
    totalRamGb = DecimalFormatter.oneDecimal(totalRamMb / MB_PER_GB),
    requiredRamGb = NOMINAL_MIN_SUPPORTED_GB.toString(),
    isRamSatisfied = totalRamMb >= MIN_SUPPORTED_RAM_MB,
    cpuCores = cpuCores,
    requiredCpuCores = MIN_CPU_CORES,
    isCoresSatisfied = cpuCores >= MIN_CPU_CORES,
)
