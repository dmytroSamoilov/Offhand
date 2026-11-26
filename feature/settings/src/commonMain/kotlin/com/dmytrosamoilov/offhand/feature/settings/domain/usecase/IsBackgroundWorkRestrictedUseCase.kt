package com.dmytrosamoilov.offhand.feature.settings.domain.usecase

import com.dmytrosamoilov.offhand.core.device.BatteryRestriction

class IsBackgroundWorkRestrictedUseCase(
    private val batteryRestriction: BatteryRestriction,
) {
    operator fun invoke(): Boolean = batteryRestriction.isRestricted()
}
