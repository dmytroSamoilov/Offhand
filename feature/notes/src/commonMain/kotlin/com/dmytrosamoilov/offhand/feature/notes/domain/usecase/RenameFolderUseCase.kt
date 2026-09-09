package com.dmytrosamoilov.offhand.feature.notes.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.FoldersRepository
import com.dmytrosamoilov.offhand.feature.notes.domain.FolderNameValidation
import com.dmytrosamoilov.offhand.feature.notes.domain.FolderNameValidator
import kotlinx.coroutines.flow.first

class RenameFolderUseCase(
    private val repository: FoldersRepository,
) {
    suspend operator fun invoke(folderId: Long, name: String): FolderSaveResult {
        val existing = repository.observeFolders().first()
        return when (val validation = FolderNameValidator.validate(name, existing, excludingId = folderId)) {
            is FolderNameValidation.Invalid -> FolderSaveResult.Rejected(validation.error)
            is FolderNameValidation.Valid -> {
                repository.renameFolder(folderId, validation.name)
                FolderSaveResult.Saved(folderId)
            }
        }
    }
}
