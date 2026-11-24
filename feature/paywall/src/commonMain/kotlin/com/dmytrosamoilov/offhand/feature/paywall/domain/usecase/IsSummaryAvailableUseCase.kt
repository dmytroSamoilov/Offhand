package com.dmytrosamoilov.offhand.feature.paywall.domain.usecase

import com.dmytrosamoilov.offhand.core.device.DeviceTierProvider

class IsSummaryAvailableUseCase(
    private val deviceTier: DeviceTierProvider,
) {
    operator fun invoke(): Boolean = deviceTier.writesSummaries()
}
