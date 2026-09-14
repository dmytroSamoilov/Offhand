package com.dmytrosamoilov.offhand.feature.paywall.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.ProPlan
import com.dmytrosamoilov.offhand.core.data.domain.ProStore
import com.dmytrosamoilov.offhand.core.data.domain.PurchaseOutcome

class PurchaseProUseCase(
    private val store: ProStore,
) {
    suspend operator fun invoke(plan: ProPlan): PurchaseOutcome {
        if (plan == ProPlan.NONE) return PurchaseOutcome.FAILED
        return store.purchase(plan)
    }
}
