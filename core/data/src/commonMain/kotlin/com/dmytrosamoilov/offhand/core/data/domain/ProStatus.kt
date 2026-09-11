package com.dmytrosamoilov.offhand.core.data.domain

enum class ProPlan {
    NONE,
    YEARLY,
    LIFETIME,
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
    }
}
