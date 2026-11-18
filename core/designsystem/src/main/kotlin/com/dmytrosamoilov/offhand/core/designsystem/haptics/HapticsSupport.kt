package com.dmytrosamoilov.offhand.core.designsystem.haptics

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

// Haptics are only played on phones whose vibrator reports the tuned
// primitives (Pixels, recent flagships). A plain motor turns the same
// feedback into a rough buzz, so every other phone stays silent. The answer
// is a fact about the hardware, so it is asked once per process.
internal object HapticsSupport {

    @Volatile
    private var isRich: Boolean? = null

    fun isRich(context: Context): Boolean =
        isRich ?: supportsPrimitives(context.applicationContext).also { isRich = it }

    private fun supportsPrimitives(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return false
        val vibrator = vibratorOf(context) ?: return false
        return vibrator.hasVibrator() && vibrator.areAllPrimitivesSupported(
            VibrationEffect.Composition.PRIMITIVE_CLICK,
            VibrationEffect.Composition.PRIMITIVE_TICK,
        )
    }

    private fun vibratorOf(context: Context): Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            context.getSystemService(Vibrator::class.java)
        }
}
