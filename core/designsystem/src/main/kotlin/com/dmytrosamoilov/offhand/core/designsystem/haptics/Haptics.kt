package com.dmytrosamoilov.offhand.core.designsystem.haptics

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalView

class Haptics(private val view: View) {

    fun confirm() = perform(HapticFeedbackConstants.CONFIRM)

    fun tick() = perform(HapticFeedbackConstants.CLOCK_TICK)

    fun gestureEnd() = perform(HapticFeedbackConstants.GESTURE_END)

    fun reject() = perform(HapticFeedbackConstants.REJECT)

    fun toggle(isOn: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            perform(if (isOn) HapticFeedbackConstants.TOGGLE_ON else HapticFeedbackConstants.TOGGLE_OFF)
        } else {
            confirm()
        }
    }

    private fun perform(constant: Int) {
        view.performHapticFeedback(constant)
    }
}

@Composable
fun haptics(): Haptics = Haptics(LocalView.current)
