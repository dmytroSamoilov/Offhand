package com.dmytrosamoilov.offhand.feature.settings.domain.usecase

import com.dmytrosamoilov.offhand.core.device.DeviceTierProvider

class IsSummaryAvailableUseCase(
    private val deviceTier: DeviceTierProvider,
) {
    operator fun invoke(): Boolean = deviceTier.writesSummaries()
}
