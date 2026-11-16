package com.dmytrosamoilov.offhand.core.data.domain

// INCLUDED is the early-adopter grant: Pro on this device without a purchase.
// TRIAL is the fourteen free days every install starts with.
enum class ProPlan {
    NONE,
    YEARLY,
    LIFETIME,
    INCLUDED,
    TRIAL,
}

data class ProStatus(
    val plan: ProPlan = ProPlan.NONE,
    val isTrial: Boolean = false,
    val renewsAtMs: Long? = null,
) {
    val isPro: Boolean
        get() = plan != ProPlan.NONE

    // Pro that will not run out by itself: a purchase or the grant. The
    // trial unlocks everything too, but its owner is still offered a plan.
    val isProForGood: Boolean
        get() = isPro && plan != ProPlan.TRIAL

    companion object {
        val FREE = ProStatus()
        val LIFETIME = ProStatus(plan = ProPlan.LIFETIME)
        val INCLUDED = ProStatus(plan = ProPlan.INCLUDED)

        fun trial(endsAtMs: Long) = ProStatus(plan = ProPlan.TRIAL, isTrial = true, renewsAtMs = endsAtMs)
    }
}
