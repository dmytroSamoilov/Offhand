package com.dmytrosamoilov.offhand.testing.fakes

import com.dmytrosamoilov.offhand.core.device.DeviceCapability
import com.dmytrosamoilov.offhand.core.device.DeviceCapabilityChecker

object SmokeDeviceTier {
    const val TRANSCRIPTS_ONLY = "transcripts"
    const val UNSUPPORTED = "unsupported"

    var requested: String? = null
}

class FakeDeviceCapabilityChecker : DeviceCapabilityChecker {

    override fun snapshot(): DeviceCapability = DeviceCapability(
        totalRamMb = when (SmokeDeviceTier.requested) {
            SmokeDeviceTier.TRANSCRIPTS_ONLY -> MID_RAM_MB
            SmokeDeviceTier.UNSUPPORTED -> LOW_RAM_MB
            else -> TOTAL_RAM_MB
        },
        availableRamMb = AVAILABLE_RAM_MB,
        cpuCores = CPU_CORES,
    )

    private companion object {
        const val TOTAL_RAM_MB = 8L * 1024L
        const val MID_RAM_MB = 3_660L
        const val LOW_RAM_MB = 1_400L
        const val AVAILABLE_RAM_MB = 1L * 1024L
        const val CPU_CORES = 8
    }
}
