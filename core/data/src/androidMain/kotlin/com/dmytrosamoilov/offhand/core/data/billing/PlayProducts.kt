package com.dmytrosamoilov.offhand.core.data.billing

import com.dmytrosamoilov.offhand.core.data.domain.ProPlan

internal object PlayProducts {
    const val SUBSCRIPTION_ID = "offhand_pro"
    const val LIFETIME_ID = "offhand_pro_lifetime"

    fun planOf(productIds: List<String>): ProPlan = when {
        LIFETIME_ID in productIds -> ProPlan.LIFETIME
        SUBSCRIPTION_ID in productIds -> ProPlan.YEARLY
        else -> ProPlan.NONE
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
