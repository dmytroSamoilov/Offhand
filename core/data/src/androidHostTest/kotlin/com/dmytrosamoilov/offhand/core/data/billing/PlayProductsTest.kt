package com.dmytrosamoilov.offhand.core.data.billing

import com.dmytrosamoilov.offhand.core.data.domain.ProPlan
import kotlin.test.Test
import kotlin.test.assertEquals

class PlayProductsTest {

    @Test
    fun `no owned product means free`() {
        assertEquals(ProPlan.NONE, PlayProducts.planOf(emptyList()))
        assertEquals(ProPlan.NONE, PlayProducts.planOf(listOf("something_else")))
    }

    @Test
    fun `the subscription maps to yearly`() {
        assertEquals(ProPlan.YEARLY, PlayProducts.planOf(listOf("offhand_pro")))
    }

    @Test
    fun `lifetime beats yearly when both are owned`() {
        assertEquals(ProPlan.LIFETIME, PlayProducts.planOf(listOf("offhand_pro", "offhand_pro_lifetime")))
    }

    @Test
    fun `offer phases are converted to days`() {
        assertEquals(14, isoPeriodToDays("P2W"))
        assertEquals(14, isoPeriodToDays("P14D"))
        assertEquals(30, isoPeriodToDays("P1M"))
        assertEquals(365, isoPeriodToDays("P1Y"))
        assertEquals(0, isoPeriodToDays("P"))
        assertEquals(0, isoPeriodToDays("garbage"))
    }
}
