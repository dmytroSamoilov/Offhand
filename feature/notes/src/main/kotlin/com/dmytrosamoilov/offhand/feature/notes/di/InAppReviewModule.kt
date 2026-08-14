package com.dmytrosamoilov.offhand.feature.notes.di

import com.dmytrosamoilov.offhand.core.common.BuildInfo
import com.dmytrosamoilov.offhand.feature.notes.domain.review.InAppReviewRules
import com.dmytrosamoilov.offhand.feature.notes.presentation.FakeInAppReviewLauncher
import com.dmytrosamoilov.offhand.feature.notes.presentation.InAppReviewLauncher
import com.dmytrosamoilov.offhand.feature.notes.presentation.PlayInAppReviewLauncher
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object InAppReviewModule {

    @Provides
    @Singleton
    fun provideInAppReviewRules(buildInfo: BuildInfo): InAppReviewRules =
        if (buildInfo.isDebugBuild) InAppReviewRules.DEBUG else InAppReviewRules.PRODUCTION

    @Provides
    @Singleton
    fun provideInAppReviewLauncher(
        buildInfo: BuildInfo,
        play: dagger.Lazy<PlayInAppReviewLauncher>,
        fake: dagger.Lazy<FakeInAppReviewLauncher>,
    ): InAppReviewLauncher = if (buildInfo.isDebugBuild) fake.get() else play.get()
}
