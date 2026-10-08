package com.dmytrosamoilov.offhand.core.data.domain

// INCLUDED is the early-adopter grant: Pro on this device without a purchase.
enum class ProPlan {
    NONE,
    YEARLY,
    LIFETIME,
    INCLUDED,
}

data class ProStatus(
    val plan: ProPlan = ProPlan.NONE,
    val isTrial: Boolean = false,
    val renewsAtMs: Long? = null,
) {
    val isPro: Boolean
        get() = plan != ProPlan.NONE

    companion object {
        val FREE = ProStatus()
        val LIFETIME = ProStatus(plan = ProPlan.LIFETIME)
        val INCLUDED = ProStatus(plan = ProPlan.INCLUDED)
    }
}
