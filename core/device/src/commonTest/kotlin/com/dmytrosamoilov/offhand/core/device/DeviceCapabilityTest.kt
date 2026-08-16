package com.dmytrosamoilov.offhand.core.device

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DeviceCapabilityTest {

    private fun capability(totalRamMb: Long, cpuCores: Int) = DeviceCapability(
        totalRamMb = totalRamMb,
        availableRamMb = totalRamMb / 2,
        cpuCores = cpuCores,
    )

    @Test
    fun deviceWithEnoughRamAndCoresIsCapable() {
        assertTrue(capability(totalRamMb = 8 * 1024, cpuCores = 8).isLocalLlmCapable())
    }

    @Test
    fun deviceAtExactThresholdsIsCapable() {
        assertTrue(capability(totalRamMb = MIN_TOTAL_RAM_MB, cpuCores = MIN_CPU_CORES).isLocalLlmCapable())
    }

    @Test
    fun deviceBelowRamThresholdIsNotCapable() {
        assertFalse(capability(totalRamMb = MIN_TOTAL_RAM_MB - 1, cpuCores = 8).isLocalLlmCapable())
    }

    @Test
    fun deviceBelowCoreThresholdIsNotCapable() {
        assertFalse(capability(totalRamMb = 8 * 1024, cpuCores = MIN_CPU_CORES - 1).isLocalLlmCapable())
    }
}
