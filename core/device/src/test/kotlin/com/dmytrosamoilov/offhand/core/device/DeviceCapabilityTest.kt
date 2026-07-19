package com.dmytrosamoilov.offhand.core.device

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceCapabilityTest {

    private fun capability(totalRamMb: Long, cpuCores: Int) = DeviceCapability(
        totalRamMb = totalRamMb,
        availableRamMb = totalRamMb / 2,
        cpuCores = cpuCores,
    )

    @Test
    fun `device with enough ram and cores is capable`() {
        assertTrue(capability(totalRamMb = 8 * 1024, cpuCores = 8).isLocalLlmCapable())
    }

    @Test
    fun `device at exact thresholds is capable`() {
        assertTrue(capability(totalRamMb = MIN_TOTAL_RAM_MB, cpuCores = MIN_CPU_CORES).isLocalLlmCapable())
    }

    @Test
    fun `device below ram threshold is not capable`() {
        assertFalse(capability(totalRamMb = 4 * 1024, cpuCores = 8).isLocalLlmCapable())
    }

    @Test
    fun `device below core threshold is not capable`() {
        assertFalse(capability(totalRamMb = 12 * 1024, cpuCores = 2).isLocalLlmCapable())
    }

    @Test
    fun `device below both thresholds is not capable`() {
        assertFalse(capability(totalRamMb = 4 * 1024, cpuCores = 2).isLocalLlmCapable())
    }
}
