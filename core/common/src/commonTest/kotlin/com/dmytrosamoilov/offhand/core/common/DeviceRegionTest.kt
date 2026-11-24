package com.dmytrosamoilov.offhand.core.common

import kotlin.test.Test
import kotlin.test.assertEquals

class DeviceRegionTest {

    @Test
    fun `countries in the EU are asked first`() {
        assertEquals(true, DeviceRegion { "DE" }.asksBeforeCollecting)
        assertEquals(true, DeviceRegion { "pt" }.asksBeforeCollecting)
    }

    @Test
    fun `countries outside are not asked`() {
        assertEquals(
            listOf(false, false, false, false, false, false),
            listOf("US", "IN", "UA", "GB", "CH", "NO").map { code -> DeviceRegion { code }.asksBeforeCollecting },
        )
    }

    @Test
    fun `a phone without a region is asked`() {
        assertEquals(true, DeviceRegion { "" }.asksBeforeCollecting)
    }
}
