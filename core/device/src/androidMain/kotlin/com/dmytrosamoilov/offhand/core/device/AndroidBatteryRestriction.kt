package com.dmytrosamoilov.offhand.core.device

import android.content.Context
import android.os.PowerManager

class AndroidBatteryRestriction(
    private val context: Context,
) : BatteryRestriction {

    override fun isRestricted(): Boolean {
        val powerManager = context.getSystemService(PowerManager::class.java) ?: return false
        return !powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }
}
