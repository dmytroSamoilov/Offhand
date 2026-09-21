package com.dmytrosamoilov.offhand.feature.paywall.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.ProStatus
import com.dmytrosamoilov.offhand.core.data.domain.ProStore

class RestoreProPurchasesUseCase(
    private val store: ProStore,
) {
    suspend operator fun invoke(): ProStatus? = store.restore()
}
