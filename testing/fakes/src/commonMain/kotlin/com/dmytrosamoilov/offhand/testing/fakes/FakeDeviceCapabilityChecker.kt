package com.dmytrosamoilov.offhand.testing.fakes

import com.dmytrosamoilov.offhand.core.device.DeviceCapability
import com.dmytrosamoilov.offhand.core.device.DeviceCapabilityChecker

class FakeDeviceCapabilityChecker : DeviceCapabilityChecker {

    override fun snapshot(): DeviceCapability = DeviceCapability(
        totalRamMb = TOTAL_RAM_MB,
        availableRamMb = AVAILABLE_RAM_MB,
        cpuCores = CPU_CORES,
    )

    private companion object {
        const val TOTAL_RAM_MB = 8L * 1024L
        const val AVAILABLE_RAM_MB = 4L * 1024L
        const val CPU_CORES = 8
    }
}
