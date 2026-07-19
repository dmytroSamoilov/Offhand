package com.dmytrosamoilov.offhand.di

import com.dmytrosamoilov.offhand.BuildConfig
import com.dmytrosamoilov.offhand.core.common.BuildInfo
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BuildModule {

    @Provides
    @Singleton
    fun provideBuildInfo(): BuildInfo = BuildInfo(isDebugBuild = BuildConfig.DEBUG)
}
