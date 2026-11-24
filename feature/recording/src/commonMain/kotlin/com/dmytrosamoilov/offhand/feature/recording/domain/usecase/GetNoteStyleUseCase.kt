package com.dmytrosamoilov.offhand.feature.recording.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.NoteStyleRef
import com.dmytrosamoilov.offhand.core.data.domain.ProStatusRepository
import com.dmytrosamoilov.offhand.core.data.domain.UserPreferencesRepository
import kotlinx.coroutines.flow.first

// The style a new note gets: the folder's own style when it has one, else
// the default from Settings. Without Pro every new note is a Summary,
// whatever default a trial or a lapsed plan left behind.
class GetNoteStyleUseCase(
    private val repository: UserPreferencesRepository,
    private val entitlements: ProStatusRepository,
    private val getFolderStyle: GetFolderStyleUseCase,
) {
    suspend operator fun invoke(folderId: Long? = null): NoteStyleRef {
        getFolderStyle(folderId)?.let { return it }
        val unlocked = entitlements.observeStatus().first().isPro
        return if (unlocked) repository.preferences.first().noteStyle else NoteStyleRef.DEFAULT
    }
}
