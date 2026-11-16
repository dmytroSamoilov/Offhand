package com.dmytrosamoilov.offhand.feature.paywall.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.ProTrialUsageCount
import com.dmytrosamoilov.offhand.core.data.domain.ProTrialUsageRepository

// The few lines the paywall shows after the trial: what was used, most
// important first. Empty when nothing from Pro was touched.
class GetProTrialRecapUseCase(
    private val usageRepository: ProTrialUsageRepository,
) {
    suspend operator fun invoke(): List<ProTrialUsageCount> =
        usageRepository.read().sortedBy { it.usage.ordinal }.take(MAX_LINES)

    private companion object {
        const val MAX_LINES = 3
    }
}
