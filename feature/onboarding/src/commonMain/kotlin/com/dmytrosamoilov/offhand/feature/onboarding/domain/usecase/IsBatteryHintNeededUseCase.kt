package com.dmytrosamoilov.offhand.feature.onboarding.domain.usecase

import com.dmytrosamoilov.offhand.core.device.BatteryRestriction
import com.dmytrosamoilov.offhand.core.device.DeviceMaker

// Samsung and Pixel phones leave a recording alone while the screen is off,
// so their owners are not sent to system settings for nothing.
class IsBatteryHintNeededUseCase(
    private val batteryRestriction: BatteryRestriction,
    private val deviceMaker: DeviceMaker,
) {
    operator fun invoke(): Boolean =
        deviceMaker.name().lowercase() !in LENIENT_MAKERS && batteryRestriction.isRestricted()

    private companion object {
        val LENIENT_MAKERS = setOf("samsung", "google")
    }
}
