package com.dmytrosamoilov.offhand.feature.notes.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.FoldersRepository

class DeleteFolderUseCase(
    private val repository: FoldersRepository,
) {
    suspend operator fun invoke(folderId: Long) = repository.deleteFolder(folderId)
}
