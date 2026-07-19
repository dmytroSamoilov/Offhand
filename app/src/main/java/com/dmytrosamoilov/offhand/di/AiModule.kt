package com.dmytrosamoilov.offhand.di

import com.dmytrosamoilov.offhand.core.ai.api.AiBackend
import com.dmytrosamoilov.offhand.core.ai.api.ModelManager
import com.dmytrosamoilov.offhand.core.ai.api.SpeechToText
import com.dmytrosamoilov.offhand.core.ai.local.LiteRtLmManager
import com.dmytrosamoilov.offhand.core.ai.local.LocalAiBackend
import com.dmytrosamoilov.offhand.core.ai.local.WhisperSpeechToText
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class AiModule {

    @Binds
    abstract fun bindModelManager(implementation: LiteRtLmManager): ModelManager

    @Binds
    abstract fun bindAiBackend(implementation: LocalAiBackend): AiBackend

    @Binds
    abstract fun bindSpeechToText(implementation: WhisperSpeechToText): SpeechToText
}
