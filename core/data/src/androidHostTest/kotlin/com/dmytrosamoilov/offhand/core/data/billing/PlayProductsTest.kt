package com.dmytrosamoilov.offhand.core.data.billing

import com.dmytrosamoilov.offhand.core.data.domain.ProPlan
import kotlin.test.Test
import kotlin.test.assertEquals

class PlayProductsTest {

    @Test
    fun `no owned product means free`() {
        assertEquals(ProPlan.NONE, PlayProducts.planOf(emptyList(), null))
        assertEquals(ProPlan.NONE, PlayProducts.planOf(listOf("something_else"), ProPlan.MONTHLY))
    }

    @Test
    fun `the subscription is the remembered plan and yearly without one`() {
        assertEquals(ProPlan.YEARLY, PlayProducts.planOf(listOf("offhand_pro"), null))
        assertEquals(ProPlan.MONTHLY, PlayProducts.planOf(listOf("offhand_pro"), ProPlan.MONTHLY))
    }

    @Test
    fun `only the two subscription plans have a base plan`() {
        assertEquals("monthly", PlayProducts.basePlanOf(ProPlan.MONTHLY))
        assertEquals("yearly", PlayProducts.basePlanOf(ProPlan.YEARLY))
        assertEquals(null, PlayProducts.basePlanOf(ProPlan.LIFETIME))
    }

    @Test
    fun `lifetime beats yearly when both are owned`() {
        assertEquals(ProPlan.LIFETIME, PlayProducts.planOf(listOf("offhand_pro", "offhand_pro_lifetime"), ProPlan.MONTHLY))
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
