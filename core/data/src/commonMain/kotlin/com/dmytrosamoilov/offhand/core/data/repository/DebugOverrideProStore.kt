package com.dmytrosamoilov.offhand.core.data.repository

import com.dmytrosamoilov.offhand.core.data.domain.ProOffer
import com.dmytrosamoilov.offhand.core.data.domain.ProOverride
import com.dmytrosamoilov.offhand.core.data.domain.ProPlan
import com.dmytrosamoilov.offhand.core.data.domain.ProStatus
import com.dmytrosamoilov.offhand.core.data.domain.ProStore
import com.dmytrosamoilov.offhand.core.data.domain.PurchaseOutcome
import com.dmytrosamoilov.offhand.core.data.domain.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

// Transparent in release builds. With the debug override set, it plays the
// store: FREE hides any purchase, PRO grants lifetime, and a purchase made
// while overridden flips the override to PRO, so the whole paywall path can
// be walked on a dev install that the real store does not know.
internal class DebugOverrideProStore(
    private val platform: ProStore,
    private val preferences: UserPreferencesRepository,
) : ProStore {

    private val override: Flow<ProOverride> = preferences.preferences
        .map { it.proOverride }
        .distinctUntilChanged()

    override val status: Flow<ProStatus?> = combine(override, platform.status) { override, status ->
        when (override) {
            ProOverride.STORE -> status
            ProOverride.FREE -> ProStatus.FREE
            ProOverride.PRO -> ProStatus.LIFETIME
        }
    }

    override suspend fun loadOffers(): List<ProOffer> {
        val offers = platform.loadOffers()
        if (offers.isNotEmpty() || currentOverride() == ProOverride.STORE) return offers
        return listOf(
            ProOffer(ProPlan.YEARLY, SAMPLE_YEARLY_PRICE, SAMPLE_TRIAL_DAYS, SAMPLE_YEARLY_MICROS, SAMPLE_MONTHLY_PRICE),
            ProOffer(ProPlan.LIFETIME, SAMPLE_LIFETIME_PRICE, priceMicros = SAMPLE_LIFETIME_MICROS),
        )
    }

    override suspend fun purchase(plan: ProPlan): PurchaseOutcome {
        if (currentOverride() == ProOverride.STORE) return platform.purchase(plan)
        preferences.setProOverride(ProOverride.PRO)
        return PurchaseOutcome.PURCHASED
    }

    override suspend fun restore(): ProStatus? = when (currentOverride()) {
        ProOverride.STORE -> platform.restore()
        ProOverride.FREE -> ProStatus.FREE
        ProOverride.PRO -> ProStatus.LIFETIME
    }

    override fun refresh() = platform.refresh()

    private suspend fun currentOverride(): ProOverride = override.first()

    private companion object {
        const val SAMPLE_YEARLY_PRICE = "$24.99"
        const val SAMPLE_LIFETIME_PRICE = "$59.99"
        const val SAMPLE_TRIAL_DAYS = 14
        const val SAMPLE_MONTHLY_PRICE = "$2.08"
        const val SAMPLE_YEARLY_MICROS = 24_990_000L
        const val SAMPLE_LIFETIME_MICROS = 59_990_000L
    }
}
