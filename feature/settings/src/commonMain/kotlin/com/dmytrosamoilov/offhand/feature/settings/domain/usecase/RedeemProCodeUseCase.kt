package com.dmytrosamoilov.offhand.feature.settings.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.ProPlan
import com.dmytrosamoilov.offhand.core.data.domain.ProStore
import com.dmytrosamoilov.offhand.core.data.domain.PurchaseOutcome

// Play has no redeem call of its own: a code is entered inside the purchase
// sheet, so redeeming means starting the yearly subscription's flow.
class RedeemProCodeUseCase(
    private val store: ProStore,
) {
    suspend operator fun invoke(): PurchaseOutcome = store.purchase(ProPlan.YEARLY)
}
