package com.dmytrosamoilov.offhand.core.data.domain

import kotlinx.coroutines.flow.Flow

interface ProStatusRepository {

    fun observeStatus(): Flow<ProStatus>
}
