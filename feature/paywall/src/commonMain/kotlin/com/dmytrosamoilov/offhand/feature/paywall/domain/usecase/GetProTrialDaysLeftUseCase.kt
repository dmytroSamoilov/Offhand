package com.dmytrosamoilov.offhand.feature.paywall.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.ProTrialPolicy
import com.dmytrosamoilov.offhand.core.data.domain.ProTrialRepository
import com.dmytrosamoilov.offhand.core.data.domain.ProTrialState
import kotlin.time.Clock

class GetProTrialDaysLeftUseCase(
    private val trialRepository: ProTrialRepository,
    private val nowMs: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) {

    suspend operator fun invoke(): Int? {
        val state = trialRepository.currentState() as? ProTrialState.Active ?: return null
        return ProTrialPolicy.daysLeft(state.endsAtMs, nowMs())
    }
}
