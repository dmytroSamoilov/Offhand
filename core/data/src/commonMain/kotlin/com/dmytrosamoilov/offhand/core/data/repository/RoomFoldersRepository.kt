@file:OptIn(ExperimentalTime::class)

package com.dmytrosamoilov.offhand.core.data.repository

import com.dmytrosamoilov.offhand.core.data.database.FolderDao
import com.dmytrosamoilov.offhand.core.data.database.FolderEntity
import com.dmytrosamoilov.offhand.core.data.database.NoteDao
import com.dmytrosamoilov.offhand.core.data.database.toDomain
import com.dmytrosamoilov.offhand.core.data.domain.Folder
import com.dmytrosamoilov.offhand.core.data.domain.FoldersRepository
import com.dmytrosamoilov.offhand.core.data.domain.NoteStyleRef
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class RoomFoldersRepository(
    private val folderDao: FolderDao,
    private val noteDao: NoteDao,
) : FoldersRepository {

    override fun observeFolders(): Flow<List<Folder>> =
        folderDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun createFolder(name: String): Long = folderDao.insert(
        FolderEntity(
            name = name,
            createdAtEpochMs = Clock.System.now().toEpochMilliseconds(),
            position = folderDao.nextPosition(),
        ),
    )

    override suspend fun renameFolder(id: Long, name: String) = folderDao.rename(id, name)

    override suspend fun setFolderStyle(id: Long, style: NoteStyleRef?) = folderDao.setStyle(id, style?.storageKey())

    override suspend fun reorderFolders(orderedIds: List<Long>) {
        orderedIds.forEachIndexed { position, id -> folderDao.setPosition(id, position) }
    }

    override suspend fun deleteFolder(id: Long) {
        noteDao.clearFolder(id)
        folderDao.deleteById(id)
    }
}
