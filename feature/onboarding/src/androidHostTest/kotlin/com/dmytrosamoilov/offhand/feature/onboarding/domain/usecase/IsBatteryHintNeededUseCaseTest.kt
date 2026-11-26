package com.dmytrosamoilov.offhand.feature.onboarding.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Test

class IsBatteryHintNeededUseCaseTest {

    private fun isNeeded(maker: String, isRestricted: Boolean): Boolean =
        IsBatteryHintNeededUseCase({ isRestricted }, { maker })()

    @Test
    fun `a restricted phone from a strict maker needs the hint`() {
        assertEquals(true, isNeeded(maker = "OnePlus", isRestricted = true))
        assertEquals(true, isNeeded(maker = "OPPO", isRestricted = true))
    }

    @Test
    fun `a phone that already exempts the app needs no hint`() {
        assertEquals(false, isNeeded(maker = "OnePlus", isRestricted = false))
    }

    @Test
    fun `Samsung and Pixel phones are never asked whatever their setting`() {
        assertEquals(false, isNeeded(maker = "samsung", isRestricted = true))
        assertEquals(false, isNeeded(maker = "Samsung", isRestricted = true))
        assertEquals(false, isNeeded(maker = "Google", isRestricted = true))
    }
}
