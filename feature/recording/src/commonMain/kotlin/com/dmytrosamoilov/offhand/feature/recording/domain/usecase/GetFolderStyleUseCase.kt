package com.dmytrosamoilov.offhand.feature.recording.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.FoldersRepository
import com.dmytrosamoilov.offhand.core.data.domain.NoteStyleRef
import com.dmytrosamoilov.offhand.core.data.domain.ProStatusRepository
import kotlinx.coroutines.flow.first

// A folder's own style is a Pro feature: a lapsed plan falls back to the
// default style without touching what the folder has stored.
class GetFolderStyleUseCase(
    private val foldersRepository: FoldersRepository,
    private val entitlements: ProStatusRepository,
) {
    suspend operator fun invoke(folderId: Long?): NoteStyleRef? {
        if (folderId == null) return null
        val style = foldersRepository.observeFolders().first().firstOrNull { it.id == folderId }?.style ?: return null
        return style.takeIf { entitlements.observeStatus().first().isPro }
    }
}
