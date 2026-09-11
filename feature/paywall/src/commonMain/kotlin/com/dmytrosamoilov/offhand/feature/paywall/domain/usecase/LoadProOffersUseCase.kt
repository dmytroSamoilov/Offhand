package com.dmytrosamoilov.offhand.feature.paywall.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.ProOffer
import com.dmytrosamoilov.offhand.core.data.domain.ProStore

class LoadProOffersUseCase(
    private val store: ProStore,
) {
    suspend operator fun invoke(): List<ProOffer> = store.loadOffers()
}
