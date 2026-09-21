package com.dmytrosamoilov.offhand.core.data.billing

import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.dmytrosamoilov.offhand.core.data.domain.ProOffer
import com.dmytrosamoilov.offhand.core.data.domain.ProPlan
import com.dmytrosamoilov.offhand.core.data.domain.ProStatus
import com.dmytrosamoilov.offhand.core.data.domain.ProStore
import com.dmytrosamoilov.offhand.core.data.domain.PurchaseOutcome
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale
import kotlin.coroutines.resume

internal class PlayProStore(
    context: Context,
    private val activityHolder: ForegroundActivityHolder,
) : ProStore, PurchasesUpdatedListener {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val client = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .enableAutoServiceReconnection()
        .build()
    private val connected = CompletableDeferred<Unit>()
    private val mutableStatus = MutableStateFlow<ProStatus?>(null)
    private val details = mutableMapOf<ProPlan, ProductDetails>()
    private var pendingPurchase: CompletableDeferred<PurchaseOutcome>? = null

    override val status: Flow<ProStatus?> = mutableStatus.asStateFlow()

    init {
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode != BillingClient.BillingResponseCode.OK) return
                connected.complete(Unit)
                refresh()
            }

            override fun onBillingServiceDisconnected() = Unit
        })
    }

    override fun refresh() {
        scope.launch { publishStatus() }
    }

    override suspend fun restore(): ProStatus? = publishStatus()

    override suspend fun loadOffers(): List<ProOffer> {
        if (!awaitReady()) return emptyList()
        val subscription = queryDetails(PlayProducts.SUBSCRIPTION_ID, BillingClient.ProductType.SUBS)
        val lifetime = queryDetails(PlayProducts.LIFETIME_ID, BillingClient.ProductType.INAPP)
        subscription?.let { details[ProPlan.YEARLY] = it }
        lifetime?.let { details[ProPlan.LIFETIME] = it }
        return listOfNotNull(subscription?.toYearlyOffer(), lifetime?.toLifetimeOffer())
    }

    override suspend fun purchase(plan: ProPlan): PurchaseOutcome {
        val product = details[plan] ?: return PurchaseOutcome.FAILED
        val activity = activityHolder.activity ?: return PurchaseOutcome.FAILED
        val outcome = CompletableDeferred<PurchaseOutcome>()
        pendingPurchase = outcome
        val result = client.launchBillingFlow(activity, billingFlowParams(product))
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            pendingPurchase = null
            return PurchaseOutcome.FAILED
        }
        return outcome.await()
    }

    // Play also reports here when a purchase that was left pending completes
    // later, so the status is refreshed even with no purchase in flight.
    override fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        val pending = pendingPurchase
        pendingPurchase = null
        scope.launch {
            val outcome = outcomeOf(result, purchases.orEmpty())
            pending?.complete(outcome)
        }
    }

    private suspend fun outcomeOf(result: BillingResult, purchases: List<Purchase>): PurchaseOutcome = when {
        result.responseCode == BillingClient.BillingResponseCode.USER_CANCELED -> PurchaseOutcome.CANCELLED
        result.responseCode != BillingClient.BillingResponseCode.OK -> PurchaseOutcome.FAILED
        purchases.any { it.purchaseState == Purchase.PurchaseState.PURCHASED } -> {
            publishStatus()
            PurchaseOutcome.PURCHASED
        }
        else -> PurchaseOutcome.PENDING
    }

    // A query Play could not answer is not a lapse: the published status only
    // changes when both product types were read, so the cached plan survives
    // a disconnected service or a flaky network.
    private suspend fun publishStatus(): ProStatus? {
        if (!awaitReady()) return null
        val status = queryStatus() ?: return null
        mutableStatus.value = status
        return status
    }

    private suspend fun awaitReady(): Boolean {
        if (client.isReady) return true
        withTimeoutOrNull(CONNECTION_TIMEOUT_MS) { connected.await() } ?: return false
        return client.isReady
    }

    private suspend fun queryStatus(): ProStatus? {
        val subscriptions = queryPurchases(BillingClient.ProductType.SUBS) ?: return null
        val oneTime = queryPurchases(BillingClient.ProductType.INAPP) ?: return null
        val owned = (subscriptions + oneTime).filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
        owned.filterNot { it.isAcknowledged }.forEach { acknowledge(it) }
        return ProStatus(plan = PlayProducts.planOf(owned.flatMap { it.products }))
    }

    private suspend fun queryPurchases(type: String): List<Purchase>? = suspendCancellableCoroutine { continuation ->
        val params = QueryPurchasesParams.newBuilder().setProductType(type).build()
        client.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                continuation.resume(purchases)
            } else {
                Timber.w("Purchase query failed: ${result.responseCode}")
                continuation.resume(null)
            }
        }
    }

    private suspend fun queryDetails(productId: String, type: String): ProductDetails? =
        suspendCancellableCoroutine { continuation ->
            val product = QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId)
                .setProductType(type)
                .build()
            val params = QueryProductDetailsParams.newBuilder().setProductList(listOf(product)).build()
            client.queryProductDetailsAsync(params) { _, result ->
                continuation.resume(result.productDetailsList.firstOrNull())
            }
        }

    private fun acknowledge(purchase: Purchase) {
        val params = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
        client.acknowledgePurchase(params) { result ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) Timber.w("Acknowledge failed")
        }
    }

    private fun billingFlowParams(product: ProductDetails): BillingFlowParams {
        val productParams = BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(product)
        product.offerToken()?.let { productParams.setOfferToken(it) }
        return BillingFlowParams.newBuilder().setProductDetailsParamsList(listOf(productParams.build())).build()
    }

    private companion object {
        const val CONNECTION_TIMEOUT_MS = 5_000L
    }
}

private fun ProductDetails.offerToken(): String? = when (productType) {
    BillingClient.ProductType.SUBS -> preferredOffer()?.offerToken
    else -> purchaseOption()?.offerToken
}

// The offer with a free phase is the trial one; a returning subscriber who
// no longer qualifies only gets the base plan back from Play.
private fun ProductDetails.preferredOffer(): ProductDetails.SubscriptionOfferDetails? =
    subscriptionOfferDetails?.maxByOrNull { it.trialDays() }

// The lifetime product has a single "Buy" purchase option in the console.
private fun ProductDetails.purchaseOption(): ProductDetails.OneTimePurchaseOfferDetails? =
    oneTimePurchaseOfferDetailsList?.firstOrNull()

private fun ProductDetails.SubscriptionOfferDetails.trialDays(): Int =
    pricingPhases.pricingPhaseList
        .filter { it.priceAmountMicros == 0L }
        .sumOf { isoPeriodToDays(it.billingPeriod) }

private fun ProductDetails.toYearlyOffer(): ProOffer? {
    val offer = preferredOffer() ?: return null
    val paidPhase = offer.pricingPhases.pricingPhaseList.lastOrNull { it.priceAmountMicros > 0 } ?: return null
    return ProOffer(
        plan = ProPlan.YEARLY,
        price = paidPhase.formattedPrice,
        trialDays = offer.trialDays(),
        priceMicros = paidPhase.priceAmountMicros,
        monthlyPrice = formatMonthly(paidPhase.priceAmountMicros, paidPhase.priceCurrencyCode),
    )
}

private fun ProductDetails.toLifetimeOffer(): ProOffer? {
    val option = purchaseOption() ?: return null
    return ProOffer(plan = ProPlan.LIFETIME, price = option.formattedPrice, priceMicros = option.priceAmountMicros)
}

private fun formatMonthly(yearlyMicros: Long, currencyCode: String): String? = runCatching {
    val format = NumberFormat.getCurrencyInstance(Locale.getDefault()).apply { currency = Currency.getInstance(currencyCode) }
    format.format(yearlyMicros / MICROS_PER_UNIT / MONTHS_PER_YEAR)
}.getOrNull()

private const val MICROS_PER_UNIT = 1_000_000.0
private const val MONTHS_PER_YEAR = 12
