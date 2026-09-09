package com.dmytrosamoilov.offhand.core.designsystem.focus

import androidx.compose.foundation.focusGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import android.os.SystemClock

@Composable
fun Modifier.userInitiatedFocusOnly(): Modifier {
    val lastTouchUptimeMillis = remember { mutableLongStateOf(0L) }
    return this
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    lastTouchUptimeMillis.longValue = event.changes.maxOf { it.uptimeMillis }
                }
            }
        }
        .focusProperties {
            onEnter = {
                val sinceTouch = SystemClock.uptimeMillis() - lastTouchUptimeMillis.longValue
                if (sinceTouch > TOUCH_FOCUS_WINDOW_MS) cancelFocusChange()
            }
        }
        .focusGroup()
}

private const val TOUCH_FOCUS_WINDOW_MS = 1_000L
