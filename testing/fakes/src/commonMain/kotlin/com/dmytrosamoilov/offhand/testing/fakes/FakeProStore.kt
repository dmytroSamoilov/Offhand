package com.dmytrosamoilov.offhand.testing.fakes

import com.dmytrosamoilov.offhand.core.data.domain.ProOffer
import com.dmytrosamoilov.offhand.core.data.domain.ProPlan
import com.dmytrosamoilov.offhand.core.data.domain.ProStatus
import com.dmytrosamoilov.offhand.core.data.domain.ProStore
import com.dmytrosamoilov.offhand.core.data.domain.PurchaseOutcome
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

// Smoke builds start on the lifetime plan so every flow runs unlocked; the
// paywall flow walks the free path through the debug override in Settings,
// which simulates the purchase itself.
class FakeProStore : ProStore {

    private val mutableStatus = MutableStateFlow<ProStatus?>(ProStatus.LIFETIME)

    override val status: Flow<ProStatus?> = mutableStatus.asStateFlow()

    override suspend fun loadOffers(): List<ProOffer> = listOf(
        ProOffer(plan = ProPlan.YEARLY, price = "$24.99", trialDays = 14, priceMicros = 24_990_000, monthlyPrice = "$2.08"),
        ProOffer(plan = ProPlan.LIFETIME, price = "$59.99", priceMicros = 59_990_000),
    )

    override suspend fun purchase(plan: ProPlan): PurchaseOutcome {
        mutableStatus.value = ProStatus(plan = plan, isTrial = plan == ProPlan.YEARLY)
        return PurchaseOutcome.PURCHASED
    }

    override suspend fun restore(): ProStatus? = mutableStatus.value

    override fun refresh() = Unit
}
