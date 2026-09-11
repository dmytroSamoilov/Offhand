package com.dmytrosamoilov.offhand.core.data.domain

import kotlinx.coroutines.flow.Flow

// priceMicros lets the paywall compare the two plans; monthlyPrice is the
// yearly price spread over twelve months, formatted by the store's locale.
data class ProOffer(
    val plan: ProPlan,
    val price: String,
    val trialDays: Int = 0,
    val priceMicros: Long = 0,
    val monthlyPrice: String? = null,
)

enum class PurchaseOutcome {
    PURCHASED,
    PENDING,
    CANCELLED,
    FAILED,
}

// The store's own view of the purchase: null until it has answered once, so
// the repository can fall back to the cached status on an offline launch.
interface ProStore {

    val status: Flow<ProStatus?>

    suspend fun loadOffers(): List<ProOffer>

    suspend fun purchase(plan: ProPlan): PurchaseOutcome

    suspend fun restore(): ProStatus?

    fun refresh()
}
