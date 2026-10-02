package com.dmytrosamoilov.offhand.feature.notes.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.FoldersRepository

class ReorderFoldersUseCase(
    private val repository: FoldersRepository,
) {
    suspend operator fun invoke(orderedIds: List<Long>) = repository.reorderFolders(orderedIds)
}
