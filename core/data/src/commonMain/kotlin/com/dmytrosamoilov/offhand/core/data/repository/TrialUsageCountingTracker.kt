package com.dmytrosamoilov.offhand.core.data.repository

import com.dmytrosamoilov.offhand.core.data.domain.ProTrialRepository
import com.dmytrosamoilov.offhand.core.data.domain.ProTrialState
import com.dmytrosamoilov.offhand.core.data.domain.ProTrialUsageRepository
import com.dmytrosamoilov.offhand.core.data.domain.analytics.AnalyticsEvent
import com.dmytrosamoilov.offhand.core.data.domain.analytics.AnalyticsTracker
import com.dmytrosamoilov.offhand.core.data.domain.analytics.proTrialUsage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

// Sits in front of the real tracker and counts the Pro features used while
// the free trial runs. The count stays on the phone and does not depend on
// the telemetry consent, which only the wrapped tracker answers to.
class TrialUsageCountingTracker(
    private val delegate: AnalyticsTracker,
    private val trialRepository: ProTrialRepository,
    private val usageRepository: ProTrialUsageRepository,
    private val scope: CoroutineScope,
) : AnalyticsTracker {

    override fun track(event: AnalyticsEvent) {
        delegate.track(event)
        val usage = event.proTrialUsage() ?: return
        scope.launch {
            if (trialRepository.currentState() is ProTrialState.Active) {
                usageRepository.add(usage.usage, usage.count)
            }
        }
    }
}
