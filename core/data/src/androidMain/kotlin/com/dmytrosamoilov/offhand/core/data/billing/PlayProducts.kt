package com.dmytrosamoilov.offhand.core.data.billing

import com.dmytrosamoilov.offhand.core.data.domain.ProPlan

internal object PlayProducts {
    const val SUBSCRIPTION_ID = "offhand_pro"
    const val LIFETIME_ID = "offhand_pro_lifetime"
    const val MONTHLY_BASE_PLAN = "monthly"
    const val YEARLY_BASE_PLAN = "yearly"

    // Play reports an owned subscription by product only, never by base plan,
    // so the plan bought on this install is remembered; without that memory
    // (a reinstall, or a subscriber from before the monthly plan) it reads
    // as yearly.
    fun planOf(productIds: List<String>, rememberedSubscription: ProPlan?): ProPlan = when {
        LIFETIME_ID in productIds -> ProPlan.LIFETIME
        SUBSCRIPTION_ID in productIds -> rememberedSubscription ?: ProPlan.YEARLY
        else -> ProPlan.NONE
    }

    fun basePlanOf(plan: ProPlan): String? = when (plan) {
        ProPlan.MONTHLY -> MONTHLY_BASE_PLAN
        ProPlan.YEARLY -> YEARLY_BASE_PLAN
        else -> null
    }
}

// ISO 8601 durations as Play reports offer phases: P2W, P14D, P1M, P1Y.
internal fun isoPeriodToDays(period: String): Int {
    val amount = period.drop(1).dropLast(1).toIntOrNull() ?: return 0
    return when (period.lastOrNull()) {
        'D' -> amount
        'W' -> amount * 7
        'M' -> amount * 30
        'Y' -> amount * 365
        else -> 0
    }
}
