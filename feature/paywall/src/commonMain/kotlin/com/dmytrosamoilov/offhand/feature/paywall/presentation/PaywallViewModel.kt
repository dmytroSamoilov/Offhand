package com.dmytrosamoilov.offhand.feature.paywall.presentation

import androidx.lifecycle.viewModelScope
import com.dmytrosamoilov.offhand.core.common.BaseViewModel
import com.dmytrosamoilov.offhand.core.data.domain.ProFeature
import com.dmytrosamoilov.offhand.core.data.domain.ProOffer
import com.dmytrosamoilov.offhand.core.data.domain.ProPlan
import com.dmytrosamoilov.offhand.core.data.domain.ProUpgradeGate
import com.dmytrosamoilov.offhand.core.data.domain.PurchaseOutcome
import com.dmytrosamoilov.offhand.core.data.domain.analytics.AnalyticsEvents
import com.dmytrosamoilov.offhand.core.data.domain.analytics.AnalyticsTracker
import com.dmytrosamoilov.offhand.feature.paywall.domain.usecase.LoadProOffersUseCase
import com.dmytrosamoilov.offhand.feature.paywall.domain.usecase.ObserveProStatusUseCase
import com.dmytrosamoilov.offhand.feature.paywall.domain.usecase.PurchaseProUseCase
import com.dmytrosamoilov.offhand.feature.paywall.domain.usecase.RestoreProPurchasesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PaywallViewModel(
    private val loadProOffers: LoadProOffersUseCase,
    private val purchasePro: PurchaseProUseCase,
    private val restoreProPurchases: RestoreProPurchasesUseCase,
    private val observeProStatus: ObserveProStatusUseCase,
    private val proUpgradeGate: ProUpgradeGate,
    private val analyticsTracker: AnalyticsTracker,
) : BaseViewModel() {

    private val mutableUiState = MutableStateFlow(PaywallUiState())
    private var isDismissalTracked = false

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
    // starts from a clean slate and fresh prices. The status is read here
    // rather than taken from the collected state, which a first presentation
    // may not have received yet.
    fun onOpened() {
        isDismissalTracked = false
        val feature = proUpgradeGate.requestedFeature.value ?: ProFeature.GENERAL
        mutableUiState.update {
            it.copy(
                feature = feature,
                selectedPlan = ProPlan.YEARLY,
                message = null,
                isPurchasing = false,
                mode = PaywallMode.OPENING,
            )
        }
        launchSafely(showLoading = false) {
            val isPro = observeProStatus().first().isPro
            mutableUiState.update { it.copy(isPro = isPro, mode = if (isPro) PaywallMode.BENEFITS else PaywallMode.OFFER) }
            if (isPro) return@launchSafely
            analyticsTracker.track(AnalyticsEvents.paywallShown(feature))
            loadOffers()
        }
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
            analyticsTracker.track(AnalyticsEvents.purchaseStarted(state.selectedPlan, state.feature))
            val outcome = purchasePro(state.selectedPlan)
            if (outcome == PurchaseOutcome.PURCHASED) trackPurchase(state)
            mutableUiState.update { it.copy(isPurchasing = false, message = outcome.toMessage()) }
        }
    }

    private fun trackPurchase(state: PaywallUiState) {
        val trial = state.selectedOffer?.trialDays?.let { it > 0 } ?: false
        analyticsTracker.track(AnalyticsEvents.purchaseCompleted(state.selectedPlan, trial, state.feature))
    }

    fun onRestoreClicked() {
        analyticsTracker.track(AnalyticsEvents.restoreClicked())
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

    // Several controls can close one presentation (the close button, the
    // free-version link, system back), so the dismissal is reported once.
    fun onClosed() {
        val state = mutableUiState.value
        if (!state.isPro && !isDismissalTracked) {
            isDismissalTracked = true
            analyticsTracker.track(AnalyticsEvents.paywallDismissed(state.feature))
        }
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
