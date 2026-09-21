package com.dmytrosamoilov.offhand.feature.notes.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.FoldersRepository
import com.dmytrosamoilov.offhand.feature.notes.domain.FolderNameError
import com.dmytrosamoilov.offhand.feature.notes.domain.FolderNameValidation
import com.dmytrosamoilov.offhand.feature.notes.domain.FolderNameValidator
import kotlinx.coroutines.flow.first

sealed interface FolderSaveResult {
    data class Saved(val folderId: Long) : FolderSaveResult
    data class Rejected(val error: FolderNameError) : FolderSaveResult
}

class CreateFolderUseCase(
    private val repository: FoldersRepository,
) {
    suspend operator fun invoke(name: String): FolderSaveResult {
        val existing = repository.observeFolders().first()
        return when (val validation = FolderNameValidator.validate(name, existing)) {
            is FolderNameValidation.Invalid -> FolderSaveResult.Rejected(validation.error)
            is FolderNameValidation.Valid -> FolderSaveResult.Saved(repository.createFolder(validation.name))
        }
    }
}
