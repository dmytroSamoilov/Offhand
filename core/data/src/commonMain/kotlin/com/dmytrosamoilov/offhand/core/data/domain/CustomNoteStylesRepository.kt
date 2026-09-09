package com.dmytrosamoilov.offhand.core.data.domain

import kotlinx.coroutines.flow.Flow

interface CustomNoteStylesRepository {

    fun observeStyles(): Flow<List<CustomNoteStyle>>

    suspend fun getStyle(id: Long): CustomNoteStyle?

    suspend fun createStyle(style: CustomNoteStyle): Long

    suspend fun updateStyle(style: CustomNoteStyle)

    suspend fun deleteStyle(id: Long)
}
