package com.dmytrosamoilov.offhand.core.device

// Whether the system may stop the app's work once the screen is off. Some
// phones freeze or end an app that is not exempt, a recording included.
fun interface BatteryRestriction {
    fun isRestricted(): Boolean
}
