package com.dmytrosamoilov.offhand.core.data.repository

import com.dmytrosamoilov.offhand.core.data.domain.ProFeature
import com.dmytrosamoilov.offhand.core.data.domain.ProStatusRepository
import com.dmytrosamoilov.offhand.core.data.domain.ProUpgradeGate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update

internal class ProUpgradeCoordinator(
    private val statusRepository: ProStatusRepository,
) : ProUpgradeGate {

    private val requested = MutableStateFlow<ProFeature?>(null)
    private val closedCount = MutableStateFlow(0)

    override val requestedFeature: StateFlow<ProFeature?> = requested.asStateFlow()

    override suspend fun requirePro(feature: ProFeature): Boolean {
        if (isPro()) return true
        val generation = closedCount.value
        requested.value = feature
        closedCount.first { it != generation }
        return isPro()
    }

    override fun onPaywallClosed() {
        requested.value = null
        closedCount.update { it + 1 }
    }

    private suspend fun isPro(): Boolean = statusRepository.observeStatus().first().isPro
}
