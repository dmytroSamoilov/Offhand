package com.dmytrosamoilov.offhand.feature.paywall.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.ProTrialNotice
import com.dmytrosamoilov.offhand.core.data.domain.ProTrialRepository

class AcknowledgeProTrialNoticeUseCase(
    private val trialRepository: ProTrialRepository,
) {
    suspend operator fun invoke(notice: ProTrialNotice) = trialRepository.markNoticeSeen(notice)
}
