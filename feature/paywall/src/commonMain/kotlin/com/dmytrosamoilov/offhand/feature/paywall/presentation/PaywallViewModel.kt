package com.dmytrosamoilov.offhand.feature.paywall.presentation

import androidx.lifecycle.viewModelScope
import com.dmytrosamoilov.offhand.core.common.BaseViewModel
import com.dmytrosamoilov.offhand.core.data.domain.ProFeature
import com.dmytrosamoilov.offhand.core.data.domain.ProOffer
import com.dmytrosamoilov.offhand.core.data.domain.ProPlan
import com.dmytrosamoilov.offhand.core.data.domain.ProUpgradeGate
import com.dmytrosamoilov.offhand.core.data.domain.PurchaseOutcome
import com.dmytrosamoilov.offhand.feature.paywall.domain.usecase.LoadProOffersUseCase
import com.dmytrosamoilov.offhand.feature.paywall.domain.usecase.ObserveProStatusUseCase
import com.dmytrosamoilov.offhand.feature.paywall.domain.usecase.PurchaseProUseCase
import com.dmytrosamoilov.offhand.feature.paywall.domain.usecase.RestoreProPurchasesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PaywallViewModel(
    private val loadProOffers: LoadProOffersUseCase,
    private val purchasePro: PurchaseProUseCase,
    private val restoreProPurchases: RestoreProPurchasesUseCase,
    observeProStatus: ObserveProStatusUseCase,
    private val proUpgradeGate: ProUpgradeGate,
) : BaseViewModel() {

    private val mutableUiState = MutableStateFlow(PaywallUiState())
    val uiState: StateFlow<PaywallUiState> = mutableUiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeProStatus().collect { status ->
                mutableUiState.update { it.copy(isPro = status.isPro) }
            }
        }
        loadOffers()
    }

    // The same instance serves every presentation, so a reopened paywall
    // starts from a clean slate and fresh prices.
    fun onOpened() {
        val feature = proUpgradeGate.requestedFeature.value ?: ProFeature.GENERAL
        mutableUiState.update {
            it.copy(feature = feature, selectedPlan = ProPlan.YEARLY, message = null, isPurchasing = false)
        }
        loadOffers()
    }

    fun onRetryOffers() = loadOffers()

    fun onPlanSelected(plan: ProPlan) {
        mutableUiState.update { it.copy(selectedPlan = plan) }
    }

    fun onPurchaseClicked() {
        val state = mutableUiState.value
        if (state.isPurchasing || state.selectedOffer == null) return
        launchSafely(showLoading = false) {
            mutableUiState.update { it.copy(isPurchasing = true) }
            val outcome = purchasePro(state.selectedPlan)
            mutableUiState.update { it.copy(isPurchasing = false, message = outcome.toMessage()) }
        }
    }

    fun onRestoreClicked() {
        launchSafely(showLoading = false) {
            mutableUiState.update { it.copy(isPurchasing = true) }
            val restored = restoreProPurchases()
            val message = PaywallMessageUi.NothingToRestore.takeUnless { restored?.isPro == true }
            mutableUiState.update { it.copy(isPurchasing = false, message = message) }
        }
    }

    fun onMessageDismissed() {
        mutableUiState.update { it.copy(message = null) }
    }

    fun onClosed() {
        proUpgradeGate.onPaywallClosed()
    }

    private fun loadOffers() {
        launchSafely(showLoading = false) {
            mutableUiState.update { it.copy(isLoadingOffers = true) }
            val offers = loadProOffers().toUi()
            mutableUiState.update { it.copy(offers = offers, isLoadingOffers = false) }
        }
    }
}

private fun List<ProOffer>.toUi(): List<ProOfferUi> {
    val yearlyMicros = firstOrNull { it.plan == ProPlan.YEARLY }?.priceMicros ?: 0L
    return map { offer ->
        ProOfferUi(
            plan = offer.plan,
            price = offer.price,
            trialDays = offer.trialDays,
            monthlyPrice = offer.monthlyPrice,
            yearsOfYearly = offer.yearsOfYearly(yearlyMicros),
        )
    }
}

private fun ProOffer.yearsOfYearly(yearlyMicros: Long): Int? {
    if (plan != ProPlan.LIFETIME || yearlyMicros <= 0L || priceMicros <= 0L) return null
    return ((priceMicros + yearlyMicros - 1) / yearlyMicros).toInt()
}

private fun PurchaseOutcome.toMessage(): PaywallMessageUi? = when (this) {
    PurchaseOutcome.PURCHASED, PurchaseOutcome.CANCELLED -> null
    PurchaseOutcome.PENDING -> PaywallMessageUi.Pending
    PurchaseOutcome.FAILED -> PaywallMessageUi.Failed
}
