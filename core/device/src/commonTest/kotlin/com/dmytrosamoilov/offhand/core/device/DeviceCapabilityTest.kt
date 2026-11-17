package com.dmytrosamoilov.offhand.core.device

import kotlin.test.Test
import kotlin.test.assertEquals

class DeviceCapabilityTest {

    private fun tierOf(totalRamMb: Long, cpuCores: Int = 8) = DeviceCapability(
        totalRamMb = totalRamMb,
        availableRamMb = totalRamMb / 2,
        cpuCores = cpuCores,
    ).tier()

    @Test
    fun phonesSoldWithSixGigabytesOrMoreGetTheFullApp() {
        assertEquals(DeviceTier.FULL, tierOf(totalRamMb = 5_530))
        assertEquals(DeviceTier.FULL, tierOf(totalRamMb = 7_450))
        assertEquals(DeviceTier.FULL, tierOf(totalRamMb = MIN_TOTAL_RAM_MB))
    }

    @Test
    fun phonesSoldWithThreeToFiveGigabytesGetTranscriptsOnly() {
        assertEquals(DeviceTier.TRANSCRIPTS_ONLY, tierOf(totalRamMb = 2_760))
        assertEquals(DeviceTier.TRANSCRIPTS_ONLY, tierOf(totalRamMb = 3_660))
        assertEquals(DeviceTier.TRANSCRIPTS_ONLY, tierOf(totalRamMb = 4_610))
        assertEquals(DeviceTier.TRANSCRIPTS_ONLY, tierOf(totalRamMb = MIN_TOTAL_RAM_MB - 1))
        assertEquals(DeviceTier.TRANSCRIPTS_ONLY, tierOf(totalRamMb = MIN_SUPPORTED_RAM_MB))
    }

    @Test
    fun phonesWithLessMemoryOrFewCoresAreNotSupported() {
        assertEquals(DeviceTier.UNSUPPORTED, tierOf(totalRamMb = 1_850))
        assertEquals(DeviceTier.UNSUPPORTED, tierOf(totalRamMb = MIN_SUPPORTED_RAM_MB - 1))
        assertEquals(DeviceTier.UNSUPPORTED, tierOf(totalRamMb = 8_192, cpuCores = MIN_CPU_CORES - 1))
    }

    @Test
    fun onlyTheFullTierWritesSummaries() {
        assertEquals(listOf(false, false, true), DeviceTier.entries.map { it.writesSummaries })
    }
}
