package com.dmytrosamoilov.offhand.feature.paywall.presentation

import com.dmytrosamoilov.offhand.core.data.domain.ProFeature
import com.dmytrosamoilov.offhand.core.data.domain.ProPlan

data class PaywallUiState(
    val feature: ProFeature = ProFeature.GENERAL,
    val offers: List<ProOfferUi> = emptyList(),
    val selectedPlan: ProPlan = ProPlan.YEARLY,
    val isLoadingOffers: Boolean = true,
    val isPurchasing: Boolean = false,
    val isPro: Boolean = false,
    val message: PaywallMessageUi? = null,
) {
    val selectedOffer: ProOfferUi?
        get() = offers.firstOrNull { it.plan == selectedPlan }
}

// yearsOfYearly: how many yearly terms the lifetime price equals, rounded up;
// null when the store gave no comparable amounts.
data class ProOfferUi(
    val plan: ProPlan,
    val price: String,
    val trialDays: Int,
    val monthlyPrice: String? = null,
    val yearsOfYearly: Int? = null,
)

sealed interface PaywallMessageUi {
    data object Failed : PaywallMessageUi
    data object Pending : PaywallMessageUi
    data object NothingToRestore : PaywallMessageUi
}
