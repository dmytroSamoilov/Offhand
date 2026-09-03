package com.dmytrosamoilov.offhand.feature.notes.domain

interface ShareCacheDirectoryProvider {

    fun shareDirectoryPath(): String

    suspend fun clearShareDirectory()
}
