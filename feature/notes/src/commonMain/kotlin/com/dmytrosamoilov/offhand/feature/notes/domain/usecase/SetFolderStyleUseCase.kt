package com.dmytrosamoilov.offhand.feature.notes.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.FoldersRepository
import com.dmytrosamoilov.offhand.core.data.domain.NoteStyleRef

class SetFolderStyleUseCase(
    private val repository: FoldersRepository,
) {
    suspend operator fun invoke(folderId: Long, style: NoteStyleRef?) = repository.setFolderStyle(folderId, style)
}
