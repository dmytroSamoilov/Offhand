package com.dmytrosamoilov.offhand.shared

import com.dmytrosamoilov.offhand.core.data.domain.ProOffer
import com.dmytrosamoilov.offhand.core.data.domain.ProPlan
import com.dmytrosamoilov.offhand.core.data.domain.ProStatus
import com.dmytrosamoilov.offhand.core.data.domain.ProStore
import com.dmytrosamoilov.offhand.core.data.domain.PurchaseOutcome
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

// Swift owns StoreKit 2; it reports the current entitlement whenever
// Transaction.updates or a purchase changes it, and answers the one-shot
// calls through completion closures because async Swift cannot suspend Kotlin.
interface IosProStoreBridge {

    fun start(onStatus: (ProStatus) -> Unit)

    fun loadOffers(onLoaded: (List<ProOffer>) -> Unit)

    fun purchase(plan: ProPlan, onResult: (PurchaseOutcome) -> Unit)

    fun restore(onDone: (ProStatus) -> Unit)

    fun refresh()
}

class IosProStore(private val bridge: IosProStoreBridge) : ProStore {

    private val mutableStatus = MutableStateFlow<ProStatus?>(null)

    override val status: Flow<ProStatus?> = mutableStatus.asStateFlow()

    init {
        bridge.start { status -> mutableStatus.value = status }
    }

    override suspend fun loadOffers(): List<ProOffer> = suspendCancellableCoroutine { continuation ->
        bridge.loadOffers { offers -> continuation.resume(offers) }
    }

    override suspend fun purchase(plan: ProPlan): PurchaseOutcome = suspendCancellableCoroutine { continuation ->
        bridge.purchase(plan) { outcome -> continuation.resume(outcome) }
    }

    override suspend fun restore(): ProStatus = suspendCancellableCoroutine { continuation ->
        bridge.restore { status ->
            mutableStatus.value = status
            continuation.resume(status)
        }
    }

    override fun refresh() = bridge.refresh()
}
