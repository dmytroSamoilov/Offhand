package com.dmytrosamoilov.offhand.feature.recording.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.NoteStyleRef
import com.dmytrosamoilov.offhand.core.data.domain.ProStatusRepository
import com.dmytrosamoilov.offhand.core.data.domain.UserPreferencesRepository
import kotlinx.coroutines.flow.first

// The style a new note gets: the folder's own style when it has one, else
// the default from Settings.
class GetNoteStyleUseCase(
    private val repository: UserPreferencesRepository,
    private val entitlements: ProStatusRepository,
    private val getFolderStyle: GetFolderStyleUseCase,
) {
    suspend operator fun invoke(folderId: Long? = null): NoteStyleRef {
        getFolderStyle(folderId)?.let { return it }
        val style = repository.preferences.first().noteStyle
        if (style is NoteStyleRef.BuiltIn) return style
        val unlocked = entitlements.observeStatus().first().isPro
        return if (unlocked) style else NoteStyleRef.DEFAULT
    }
}
