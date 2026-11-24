package com.dmytrosamoilov.offhand.feature.notes.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.ProPlan
import com.dmytrosamoilov.offhand.core.data.domain.ProStatusRepository
import com.dmytrosamoilov.offhand.core.data.domain.ProTrialNotice
import com.dmytrosamoilov.offhand.core.data.domain.ProTrialRepository
import com.dmytrosamoilov.offhand.core.data.domain.ProUpgradeGate
import kotlinx.coroutines.flow.first

class OfferProTrialUseCase(
    private val trialRepository: ProTrialRepository,
    private val proStatusRepository: ProStatusRepository,
    private val proUpgradeGate: ProUpgradeGate,
) {

    suspend operator fun invoke() {
        if (trialRepository.isNoticeSeen(ProTrialNotice.OFFERED)) return
        trialRepository.markNoticeSeen(ProTrialNotice.OFFERED)
        if (proStatusRepository.observeStatus().first().plan == ProPlan.TRIAL) {
            proUpgradeGate.showTrialOffer()
        }
    }
}
