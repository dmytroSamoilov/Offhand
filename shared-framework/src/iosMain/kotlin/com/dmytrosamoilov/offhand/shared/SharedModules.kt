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
import com.dmytrosamoilov.offhand.core.security.BackupCrypto
import com.dmytrosamoilov.offhand.core.security.di.coreSecurityModule
import com.dmytrosamoilov.offhand.feature.backup.di.featureBackupModule
import com.dmytrosamoilov.offhand.feature.notes.di.featureNotesIosModule
import com.dmytrosamoilov.offhand.feature.notes.di.featureNotesModule
import com.dmytrosamoilov.offhand.feature.notes.domain.NoteShareLabels
import com.dmytrosamoilov.offhand.feature.notes.domain.NoteShareLabelsProvider
import com.dmytrosamoilov.offhand.feature.onboarding.di.featureOnboardingModule
import com.dmytrosamoilov.offhand.feature.onboarding.presentation.OnboardingStepPolicy
import com.dmytrosamoilov.offhand.feature.recording.di.featureRecordingIosModule
import com.dmytrosamoilov.offhand.feature.recording.di.featureRecordingModule
import com.dmytrosamoilov.offhand.feature.recording.domain.AudioRecorder
import com.dmytrosamoilov.offhand.feature.recording.domain.DefaultNoteTitleProvider
import com.dmytrosamoilov.offhand.feature.settings.di.featureSettingsModule
import com.dmytrosamoilov.offhand.testing.fakes.smokeFakesModule
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
    val untitledNoteTitle: String,
    val shareLabels: NoteShareLabels,
    val shareFallbackTitle: String,
    val appVersion: String,
    val backupCrypto: IosBackupCryptoBridge,
)

fun startSharedKoin(deps: IosPlatformDeps) {
    startSharedKoin(deps, useSmokeFakes = false)
}

// Smoke-test builds swap the AI engines, the device gate and the microphone for
// canned fakes so a simulator without the 2.4 GB model can walk the whole flow.
fun startSharedKoin(deps: IosPlatformDeps, useSmokeFakes: Boolean) {
    startKoin {
        modules(sharedIosModules(deps) + if (useSmokeFakes) listOf(smokeFakesModule) else emptyList())
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
    featureBackupModule,
    platformDepsModule(deps),
)

@OptIn(ExperimentalNativeApi::class)
private fun platformDepsModule(deps: IosPlatformDeps): Module = module {
    single { BuildInfo(isDebugBuild = Platform.isDebugBinary, appVersion = deps.appVersion, platform = "ios") }
    single<BackupCrypto> { IosBackupCrypto(deps.backupCrypto) }
    single { deps.gemmaEngine }
    single { deps.whisperEngine }
    single { IosFileDownloader() }
    single { IosModelManager(get(), get()) } bind ModelManager::class
    single<AiBackend> { IosAiBackend(get(), get()) }
    single<SpeechToText> { IosWhisperSpeechToText(get(), get(), get()) }
    single<AudioRecorder> { IosAudioRecorder(deps.audioSource) }
    single<DefaultNoteTitleProvider> {
        IosDefaultNoteTitleProvider(deps.noteTitleTemplate, deps.untitledNoteTitle)
    }
    single<NoteShareLabelsProvider> {
        IosNoteShareLabelsProvider(deps.shareLabels, deps.shareFallbackTitle)
    }
    single<ModelDownloadController> {
        IosModelDownloadController(
            get(),
            get(),
            CoroutineScope(SupervisorJob() + Dispatchers.Default),
        )
    }
    single { OnboardingStepPolicy(asksNotificationPermission = true) }
    factory { IosRootViewModel(get(), get(), get(), get(), get(), get(), get(), get()) }
}

class IosDefaultNoteTitleProvider(
    private val template: String,
    private val untitled: String,
) : DefaultNoteTitleProvider {

    override fun titleFor(nextNumber: Int): String = template.replace("%d", nextNumber.toString())

    override fun untitledTitle(): String = untitled
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
    private val speechToText: SpeechToText,
    private val scope: CoroutineScope,
) : ModelDownloadController {

    // Speech first, matching Android: it is the smaller download and the one the
    // very first recording needs, and finishing it before the LLM keeps
    // ModelState.Ready a truthful signal that everything is on disk.
    override fun start() {
        scope.launch {
            runCatching { speechToText.prepare() }
            runCatching { modelManager.ensureModelAvailable() }
        }
    }
}
