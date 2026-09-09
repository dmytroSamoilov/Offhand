package com.dmytrosamoilov.offhand.core.data.domain

import kotlinx.coroutines.flow.Flow

interface FoldersRepository {

    fun observeFolders(): Flow<List<Folder>>

    suspend fun createFolder(name: String): Long

    suspend fun renameFolder(id: Long, name: String)

    suspend fun deleteFolder(id: Long)
}
