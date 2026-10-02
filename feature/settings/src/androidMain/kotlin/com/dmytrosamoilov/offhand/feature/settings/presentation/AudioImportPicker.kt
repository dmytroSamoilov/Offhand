package com.dmytrosamoilov.offhand.feature.settings.presentation

import androidx.compose.runtime.staticCompositionLocalOf

// The system picker is another activity, so the app locks behind it and the
// lock screen replaces Settings before the result arrives. The activity owns
// the launcher and hands the result to the activity-scoped import flow, and
// Settings only asks for the picker through this seam.
fun interface AudioImportPicker {
    fun pick()
}

val LocalAudioImportPicker = staticCompositionLocalOf<AudioImportPicker> {
    error("No AudioImportPicker provided")
}
