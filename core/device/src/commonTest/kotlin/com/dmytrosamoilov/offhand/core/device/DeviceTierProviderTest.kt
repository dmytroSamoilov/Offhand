package com.dmytrosamoilov.offhand.core.device

import kotlin.test.Test
import kotlin.test.assertEquals

class DeviceTierProviderTest {

    private class CountingChecker(var totalRamMb: Long) : DeviceCapabilityChecker {
        var snapshots = 0

        override fun snapshot(): DeviceCapability {
            snapshots++
            return DeviceCapability(totalRamMb = totalRamMb, availableRamMb = 1_024, cpuCores = 8)
        }
    }

    @Test
    fun `the tier is worked out once however often it is asked for`() {
        val checker = CountingChecker(totalRamMb = 8_192)
        val provider = DeviceTierProvider(checker)

        repeat(5) { assertEquals(DeviceTier.FULL, provider.current()) }

        assertEquals(1, checker.snapshots)
    }

    @Test
    fun `nothing is measured until the tier is asked for`() {
        val checker = CountingChecker(totalRamMb = 8_192)

        DeviceTierProvider(checker)

        assertEquals(0, checker.snapshots)
    }

    @Test
    fun `a provider that does not remember follows the checker`() {
        val checker = CountingChecker(totalRamMb = 8_192)
        val provider = DeviceTierProvider(checker, isRemembered = false)

        assertEquals(DeviceTier.FULL, provider.current())
        checker.totalRamMb = 3_660

        assertEquals(DeviceTier.TRANSCRIPTS_ONLY, provider.current())
        assertEquals(false, provider.writesSummaries())
    }
}
