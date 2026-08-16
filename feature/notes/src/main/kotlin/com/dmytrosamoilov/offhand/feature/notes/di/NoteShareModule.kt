package com.dmytrosamoilov.offhand.feature.notes.di

import com.dmytrosamoilov.offhand.feature.notes.domain.NoteShareLabelsProvider
import com.dmytrosamoilov.offhand.feature.notes.domain.ShareCacheDirectoryProvider
import com.dmytrosamoilov.offhand.feature.notes.presentation.NoteShareLabelsProviderImpl
import com.dmytrosamoilov.offhand.feature.notes.presentation.ShareCacheDirectoryProviderImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class NoteShareModule {

    @Binds
    abstract fun bindNoteShareLabelsProvider(
        implementation: NoteShareLabelsProviderImpl,
    ): NoteShareLabelsProvider

    @Binds
    abstract fun bindShareCacheDirectoryProvider(
        implementation: ShareCacheDirectoryProviderImpl,
    ): ShareCacheDirectoryProvider
}
