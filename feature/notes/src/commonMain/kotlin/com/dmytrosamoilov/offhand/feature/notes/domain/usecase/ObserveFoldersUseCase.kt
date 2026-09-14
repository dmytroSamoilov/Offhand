package com.dmytrosamoilov.offhand.feature.notes.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.Folder
import com.dmytrosamoilov.offhand.core.data.domain.FoldersRepository
import kotlinx.coroutines.flow.Flow

class ObserveFoldersUseCase(
    private val repository: FoldersRepository,
) {
    operator fun invoke(): Flow<List<Folder>> = repository.observeFolders()
}
