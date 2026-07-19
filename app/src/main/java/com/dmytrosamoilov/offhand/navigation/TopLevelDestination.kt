package com.dmytrosamoilov.offhand.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.dmytrosamoilov.offhand.R
import kotlinx.serialization.Serializable

@Serializable
data object NotesRoute

@Serializable
data object SettingsRoute

enum class TopLevelDestination(
    @param:StringRes val labelRes: Int,
    val icon: ImageVector,
    val route: Any,
) {
    NOTES(R.string.destination_notes, Icons.AutoMirrored.Filled.Notes, NotesRoute),
    SETTINGS(R.string.destination_settings, Icons.Filled.Settings, SettingsRoute),
}
