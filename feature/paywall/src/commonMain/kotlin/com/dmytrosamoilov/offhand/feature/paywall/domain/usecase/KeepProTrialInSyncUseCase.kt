package com.dmytrosamoilov.offhand.feature.paywall.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.ProTrialRepository

class KeepProTrialInSyncUseCase(
    private val trialRepository: ProTrialRepository,
) {
    suspend operator fun invoke() = trialRepository.keepInSync()
}
