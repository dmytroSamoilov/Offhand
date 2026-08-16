package com.dmytrosamoilov.offhand.feature.notes.di

import com.dmytrosamoilov.offhand.feature.notes.domain.DateLabelFormatter
import com.dmytrosamoilov.offhand.feature.notes.presentation.DateLabelFormatterImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class DateLabelModule {

    @Binds
    abstract fun bindDateLabelFormatter(implementation: DateLabelFormatterImpl): DateLabelFormatter
}
