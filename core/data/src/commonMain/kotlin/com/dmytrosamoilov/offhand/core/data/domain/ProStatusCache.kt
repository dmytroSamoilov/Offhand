package com.dmytrosamoilov.offhand.core.data.domain

interface ProStatusCache {

    suspend fun read(): ProStatus

    suspend fun write(status: ProStatus)
}
