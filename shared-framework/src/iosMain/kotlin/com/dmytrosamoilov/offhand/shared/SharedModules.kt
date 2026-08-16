package com.dmytrosamoilov.offhand.shared

import com.dmytrosamoilov.offhand.core.ai.api.AiBackend
import com.dmytrosamoilov.offhand.core.ai.api.ModelManager
import com.dmytrosamoilov.offhand.core.ai.api.SpeechToText
import com.dmytrosamoilov.offhand.core.ai.api.di.coreAiApiModule
import com.dmytrosamoilov.offhand.core.common.BuildInfo
import com.dmytrosamoilov.offhand.core.common.ModelDownloadController
import kotlin.experimental.ExperimentalNativeApi
import com.dmytrosamoilov.offhand.core.data.di.coreDataModule
import com.dmytrosamoilov.offhand.core.device.di.coreDeviceModule
import com.dmytrosamoilov.offhand.core.security.di.coreSecurityModule
import com.dmytrosamoilov.offhand.feature.notes.di.featureNotesIosModule
import com.dmytrosamoilov.offhand.feature.notes.di.featureNotesModule
import com.dmytrosamoilov.offhand.feature.notes.domain.NoteShareLabels
import com.dmytrosamoilov.offhand.feature.notes.domain.NoteShareLabelsProvider
import com.dmytrosamoilov.offhand.feature.onboarding.di.featureOnboardingModule
import com.dmytrosamoilov.offhand.feature.recording.di.featureRecordingIosModule
import com.dmytrosamoilov.offhand.feature.recording.di.featureRecordingModule
import com.dmytrosamoilov.offhand.feature.recording.domain.AudioRecorder
import com.dmytrosamoilov.offhand.feature.recording.domain.DefaultNoteTitleProvider
import com.dmytrosamoilov.offhand.feature.settings.di.featureSettingsModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.bind
import org.koin.dsl.module

class IosPlatformDeps(
    val gemmaEngine: IosGemmaEngine,
    val whisperEngine: IosWhisperEngine,
    val audioSource: IosAudioSource,
    val noteTitleTemplate: String,
    val shareLabels: NoteShareLabels,
    val shareFallbackTitle: String,
)

fun startSharedKoin(deps: IosPlatformDeps) {
    startKoin {
        modules(sharedIosModules(deps))
    }
}

fun sharedIosModules(deps: IosPlatformDeps): List<Module> = listOf(
    coreAiApiModule,
    coreDeviceModule,
    coreSecurityModule,
    coreDataModule,
    featureNotesModule,
    featureNotesIosModule,
    featureOnboardingModule,
    featureRecordingModule,
    featureRecordingIosModule,
    featureSettingsModule,
    platformDepsModule(deps),
)

@OptIn(ExperimentalNativeApi::class)
private fun platformDepsModule(deps: IosPlatformDeps): Module = module {
    single { BuildInfo(isDebugBuild = Platform.isDebugBinary) }
    single { deps.gemmaEngine }
    single { deps.whisperEngine }
    single { IosFileDownloader() }
    single { IosModelManager(get(), get()) } bind ModelManager::class
    single<AiBackend> { IosAiBackend(get(), get()) }
    single<SpeechToText> { IosWhisperSpeechToText(get(), get(), get()) }
    single<AudioRecorder> { IosAudioRecorder(deps.audioSource) }
    single<DefaultNoteTitleProvider> { IosDefaultNoteTitleProvider(deps.noteTitleTemplate) }
    single<NoteShareLabelsProvider> {
        IosNoteShareLabelsProvider(deps.shareLabels, deps.shareFallbackTitle)
    }
    single<ModelDownloadController> {
        IosModelDownloadController(get(), CoroutineScope(SupervisorJob() + Dispatchers.Default))
    }
    factory { IosRootViewModel(get(), get()) }
}

class IosDefaultNoteTitleProvider(
    private val template: String,
) : DefaultNoteTitleProvider {

    override fun titleFor(nextNumber: Int): String = template.replace("%d", nextNumber.toString())
}

class IosNoteShareLabelsProvider(
    private val shareLabels: NoteShareLabels,
    private val shareFallbackTitle: String,
) : NoteShareLabelsProvider {

    override fun labels(): NoteShareLabels = shareLabels

    override fun fallbackTitle(): String = shareFallbackTitle
}

class IosModelDownloadController(
    private val modelManager: ModelManager,
    private val scope: CoroutineScope,
) : ModelDownloadController {

    override fun start() {
        scope.launch {
            runCatching { modelManager.ensureModelAvailable() }
        }
    }
}
