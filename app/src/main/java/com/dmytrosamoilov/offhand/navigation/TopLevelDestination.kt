package com.dmytrosamoilov.offhand.navigation

import androidx.annotation.StringRes
import com.dmytrosamoilov.offhand.R
import kotlinx.serialization.Serializable

@Serializable
data object NotesGraphRoute

@Serializable
data object NotesRoute

@Serializable
data object NewNoteStyleFromNoteRoute

@Serializable
data object SettingsGraphRoute

@Serializable
data object SettingsRoute

@Serializable
data object AboutSupportRoute

@Serializable
data object BackupRoute

@Serializable
data object NoteStylesRoute

@Serializable
data class NoteStyleEditorRoute(val styleId: Long)

enum class TopLevelDestination(
    @param:StringRes val labelRes: Int,
    val route: Any,
) {
    NOTES(R.string.destination_notes, NotesGraphRoute),
    SETTINGS(R.string.destination_settings, SettingsGraphRoute),
    ;

    val testTag: String
        get() = "tab_${name.lowercase()}"
}
