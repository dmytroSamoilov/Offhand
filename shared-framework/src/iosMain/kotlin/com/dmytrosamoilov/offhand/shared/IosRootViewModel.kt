package com.dmytrosamoilov.offhand.shared

import com.dmytrosamoilov.offhand.core.data.domain.ProFeature
import androidx.lifecycle.viewModelScope
import com.dmytrosamoilov.offhand.core.common.BaseViewModel
import com.dmytrosamoilov.offhand.core.data.domain.ModelDownloadLauncher
import com.dmytrosamoilov.offhand.core.data.domain.ProTrialNotice
import com.dmytrosamoilov.offhand.core.data.domain.ProUpgradeGate
import com.dmytrosamoilov.offhand.core.security.AppLockManager
import com.dmytrosamoilov.offhand.core.security.AppLockState
import com.dmytrosamoilov.offhand.feature.notes.domain.usecase.ClearShareCacheUseCase
import com.dmytrosamoilov.offhand.feature.notes.domain.usecase.DecideEarlyAdopterProUseCase
import com.dmytrosamoilov.offhand.feature.notes.domain.usecase.MarkEarlyAdopterThankedUseCase
import com.dmytrosamoilov.offhand.core.data.domain.isEarlyAdopterThanksPending
import com.dmytrosamoilov.offhand.feature.onboarding.domain.usecase.ObserveUserPreferencesUseCase
import com.dmytrosamoilov.offhand.feature.paywall.domain.usecase.AcknowledgeProTrialNoticeUseCase
import com.dmytrosamoilov.offhand.feature.paywall.domain.usecase.KeepProTrialInSyncUseCase
import com.dmytrosamoilov.offhand.feature.paywall.domain.usecase.SettleProTrialUseCase
import com.dmytrosamoilov.offhand.feature.recording.domain.PendingNotesCoordinator
import com.dmytrosamoilov.offhand.feature.recording.domain.NoteWaveformFiller
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.IsAiCoreDownloadedUseCase
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.ResumeInterruptedNotesUseCase
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.SweepOrphanedRecordingsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn

enum class IosRootPhase {
    LOADING,
    ONBOARDING,
    LOCKED,
    READY,
}

class IosRootViewModel(
    observeUserPreferences: ObserveUserPreferencesUseCase,
    private val appLockManager: AppLockManager,
    private val isAiCoreDownloaded: IsAiCoreDownloadedUseCase,
    private val modelDownloadLauncher: ModelDownloadLauncher,
    private val resumeInterruptedNotes: ResumeInterruptedNotesUseCase,
    private val sweepOrphanedRecordings: SweepOrphanedRecordingsUseCase,
    private val noteWaveformFiller: NoteWaveformFiller,
    private val clearShareCache: ClearShareCacheUseCase,
    private val decideEarlyAdopterPro: DecideEarlyAdopterProUseCase,
    private val markEarlyAdopterThanked: MarkEarlyAdopterThankedUseCase,
    private val settleProTrial: SettleProTrialUseCase,
    private val acknowledgeProTrialNotice: AcknowledgeProTrialNoticeUseCase,
    private val keepProTrialInSync: KeepProTrialInSyncUseCase,
    private val proUpgradeGate: ProUpgradeGate,
    pendingNotesCoordinator: PendingNotesCoordinator,
) : BaseViewModel() {

    val phase: StateFlow<IosRootPhase> = combine(
        observeUserPreferences(),
        appLockManager.lockState,
    ) { preferences, lockState ->
        when {
            !preferences.onboardingCompleted -> IosRootPhase.ONBOARDING
            preferences.appLockEnabled && lockState == AppLockState.LOCKED -> IosRootPhase.LOCKED
            else -> IosRootPhase.READY
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, IosRootPhase.LOADING)

    val isEarlyAdopterThanksShown: StateFlow<Boolean> = combine(phase, observeUserPreferences()) { phase, preferences ->
        phase == IosRootPhase.READY && preferences.isEarlyAdopterThanksPending
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val mutableTrialNotice = MutableStateFlow<ProTrialNotice?>(null)

    // Set once per cold start, after the app is READY: the trial ran out or
    // was removed and the user has not been told yet.
    val trialNotice: StateFlow<ProTrialNotice?> = mutableTrialNotice.asStateFlow()

    val isDeviceSecure: Boolean
        get() = appLockManager.isDeviceSecure

    init {
        skipLockForFirstRun(observeUserPreferences)
        finishStartupWhenReady()
        resumeModelDownloadWhenReady()
        keepProTrialInSyncWhenReady()
        // Picks notes back up the moment the model download finishes, instead of
        // leaving them stuck until the app is backgrounded and reopened.
        pendingNotesCoordinator.start()
    }

    fun onEarlyAdopterThanksDismissed() {
        launchSafely(showLoading = false) { markEarlyAdopterThanked() }
    }

    fun onTrialNoticeDismissed() {
        val notice = mutableTrialNotice.value ?: return
        mutableTrialNotice.value = null
        launchSafely(showLoading = false) { acknowledgeProTrialNotice(notice) }
    }

    // Called once the thank-you sheet is gone: a sheet cannot present the
    // paywall cover itself.
    fun onProBenefitsRequested() {
        proUpgradeGate.showBenefits()
    }

    // The widget opens the app when it cannot record by itself; without Pro
    // the paywall comes first, and the recording starts once it is passed.
    fun onWidgetRecordingRequested(onAllowed: () -> Unit) {
        launchSafely(showLoading = false) {
            if (proUpgradeGate.requirePro(ProFeature.WIDGET)) onAllowed()
        }
    }

    fun onUnlockAuthenticated() {
        appLockManager.markUnlocked()
    }

    // Called when the app backgrounds while no recording is in flight; the caller
    // owns the recording check because session state lives outside this module.
    fun lockOnBackground() {
        appLockManager.markLocked()
    }

    // Runs on every return to the foreground, so a download that the system
    // dropped while the app was away is picked up again without a relaunch.
    fun onReady() {
        launchSafely(showLoading = false) {
            if (phase.value != IosRootPhase.READY) return@launchSafely
            resumeInterruptedNotes()
            startModelDownloadIfMissing()
        }
    }

    private fun finishStartupWhenReady() {
        launchSafely(showLoading = false) {
            phase.first { it == IosRootPhase.READY }
            decideEarlyAdopterPro()
            settleProTrialOnColdStart()
            resumeInterruptedNotes()
            sweepOrphanedRecordings()
            noteWaveformFiller.start()
            clearShareCache()
        }
    }

    private suspend fun settleProTrialOnColdStart() {
        val settlement = settleProTrial()
        if (settlement.notice == ProTrialNotice.ENDED) showTrialEnded() else mutableTrialNotice.value = settlement.notice
    }

    // The end of the trial is told by the paywall itself, with what was used.
    private suspend fun showTrialEnded() {
        acknowledgeProTrialNotice(ProTrialNotice.ENDED)
        proUpgradeGate.showTrialEnded()
    }

    // A trial record from before a reinstall can arrive from iCloud after
    // the first launch already started a new one.
    private fun keepProTrialInSyncWhenReady() {
        launchSafely(showLoading = false) {
            phase.first { it == IosRootPhase.READY }
            keepProTrialInSync()
        }
    }

    // READY implies onboarding is complete, so the user has already agreed to the download.
    private fun resumeModelDownloadWhenReady() {
        launchSafely(showLoading = false) {
            phase.first { it == IosRootPhase.READY }
            startModelDownloadIfMissing()
        }
    }

    private suspend fun startModelDownloadIfMissing() {
        if (!isAiCoreDownloaded()) {
            modelDownloadLauncher.startWhenAllowed()
        }
    }

    private fun skipLockForFirstRun(observeUserPreferences: ObserveUserPreferencesUseCase) {
        launchSafely(showLoading = false) {
            if (!observeUserPreferences().first().onboardingCompleted) {
                appLockManager.markUnlocked()
                decideEarlyAdopterPro()
            }
        }
    }
}
