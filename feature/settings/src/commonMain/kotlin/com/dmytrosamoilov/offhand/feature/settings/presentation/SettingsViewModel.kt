package com.dmytrosamoilov.offhand.feature.settings.presentation

import androidx.lifecycle.viewModelScope
import com.dmytrosamoilov.offhand.core.common.BaseViewModel
import com.dmytrosamoilov.offhand.core.common.BuildInfo
import com.dmytrosamoilov.offhand.core.data.domain.AudioImportSource
import com.dmytrosamoilov.offhand.core.data.domain.NoteStyleRef
import com.dmytrosamoilov.offhand.core.data.domain.ProFeature
import com.dmytrosamoilov.offhand.core.data.domain.ProOverride
import com.dmytrosamoilov.offhand.core.data.domain.ProPlan
import com.dmytrosamoilov.offhand.core.data.domain.ProStatus
import com.dmytrosamoilov.offhand.core.data.domain.ProUpgradeGate
import com.dmytrosamoilov.offhand.core.data.domain.analytics.AnalyticsEvents
import com.dmytrosamoilov.offhand.core.data.domain.analytics.AnalyticsTracker
import com.dmytrosamoilov.offhand.core.security.AppLockManager
import com.dmytrosamoilov.offhand.feature.settings.domain.usecase.IsCustomNoteStylesAvailableUseCase
import com.dmytrosamoilov.offhand.feature.settings.domain.usecase.ObserveAppLockEnabledUseCase
import com.dmytrosamoilov.offhand.feature.settings.domain.usecase.ObserveCustomNoteStylesUseCase
import com.dmytrosamoilov.offhand.feature.settings.domain.usecase.ObserveDynamicColorUseCase
import com.dmytrosamoilov.offhand.feature.settings.domain.usecase.ObserveNoteStyleUseCase
import com.dmytrosamoilov.offhand.feature.settings.domain.usecase.ObserveProOverrideUseCase
import com.dmytrosamoilov.offhand.feature.settings.domain.usecase.ObserveProStatusUseCase
import com.dmytrosamoilov.offhand.feature.settings.domain.usecase.ObserveSmartSuggestionsEnabledUseCase
import com.dmytrosamoilov.offhand.feature.settings.domain.usecase.SetAppLockEnabledUseCase
import com.dmytrosamoilov.offhand.feature.settings.domain.usecase.SetDynamicColorUseCase
import com.dmytrosamoilov.offhand.feature.settings.domain.usecase.SetNoteStyleUseCase
import com.dmytrosamoilov.offhand.feature.settings.domain.usecase.SetProOverrideUseCase
import com.dmytrosamoilov.offhand.feature.settings.domain.usecase.SetSmartSuggestionsEnabledUseCase
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.ImportAudioResult
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.ImportAudioUseCase
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.IsAudioImportAvailableUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    observeDynamicColor: ObserveDynamicColorUseCase,
    private val setDynamicColor: SetDynamicColorUseCase,
    observeNoteStyle: ObserveNoteStyleUseCase,
    private val setNoteStyle: SetNoteStyleUseCase,
    observeCustomNoteStyles: ObserveCustomNoteStylesUseCase,
    isCustomNoteStylesAvailable: IsCustomNoteStylesAvailableUseCase,
    observeAppLockEnabled: ObserveAppLockEnabledUseCase,
    private val setAppLockEnabled: SetAppLockEnabledUseCase,
    private val appLockManager: AppLockManager,
    private val importAudio: ImportAudioUseCase,
    isAudioImportAvailable: IsAudioImportAvailableUseCase,
    observeSmartSuggestionsEnabled: ObserveSmartSuggestionsEnabledUseCase,
    private val setSmartSuggestionsEnabled: SetSmartSuggestionsEnabledUseCase,
    observeProStatus: ObserveProStatusUseCase,
    observeProOverride: ObserveProOverrideUseCase,
    private val setProOverride: SetProOverrideUseCase,
    private val proUpgradeGate: ProUpgradeGate,
    buildInfo: BuildInfo,
    private val analyticsTracker: AnalyticsTracker,
) : BaseViewModel() {

    private val mutableUiState = MutableStateFlow(
        SettingsUiState(isDeviceSecure = appLockManager.isDeviceSecure),
    )
    val uiState: StateFlow<SettingsUiState> = mutableUiState.asStateFlow()

    init {
        collect(observeDynamicColor()) { enabled -> copy(isDynamicColorEnabled = enabled) }
        collect(observeNoteStyle()) { style -> copy(noteStyle = style) }
        collect(observeAppLockEnabled()) { enabled -> copy(isAppLockEnabled = enabled) }
        collect(observeCustomNoteStyles()) { styles -> copy(customStyles = styles.map { it.toOptionUi() }) }
        collect(isCustomNoteStylesAvailable()) { unlocked -> copy(isCustomStylesUnlocked = unlocked) }
        collect(isAudioImportAvailable()) { unlocked -> copy(isAudioImportUnlocked = unlocked) }
        collect(observeSmartSuggestionsEnabled()) { enabled -> copy(isSmartSuggestionsEnabled = enabled) }
        collect(observeProStatus()) { status -> copy(pro = status.toUi(), isSmartSuggestionsUnlocked = status.isPro) }
        if (buildInfo.isDeveloperBuild) collect(observeProOverride()) { override -> copy(proOverride = override) }
    }

    private fun <T> collect(flow: Flow<T>, reduce: SettingsUiState.(T) -> SettingsUiState) {
        viewModelScope.launch { flow.collect { value -> mutableUiState.update { it.reduce(value) } } }
    }

    // A passcode can be added or removed in system settings while this screen is
    // backgrounded, and neither change notifies the app.
    fun onScreenShown() {
        mutableUiState.update { it.copy(isDeviceSecure = appLockManager.isDeviceSecure) }
    }

    fun onNoteStyleSelected(style: NoteStyleRef) {
        launchSafely(showLoading = false) {
            if (style is NoteStyleRef.Custom && !proUpgradeGate.requirePro(ProFeature.CUSTOM_STYLES)) return@launchSafely
            setNoteStyle(style)
            analyticsTracker.track(AnalyticsEvents.defaultStyleChanged(style))
        }
    }

    fun onDynamicColorChanged(enabled: Boolean) {
        launchSafely(showLoading = false) {
            setDynamicColor(enabled)
        }
    }

    fun onAppLockChanged(enabled: Boolean) {
        launchSafely(showLoading = false) {
            val effective = enabled && appLockManager.isDeviceSecure
            setAppLockEnabled(effective)
            analyticsTracker.track(AnalyticsEvents.appLockToggled(effective))
        }
    }

    // Off is free for everyone; switching it on is the Pro action.
    fun onSmartSuggestionsChanged(enabled: Boolean) {
        launchSafely(showLoading = false) {
            if (enabled && !proUpgradeGate.requirePro(ProFeature.SMART_SUGGESTIONS)) return@launchSafely
            setSmartSuggestionsEnabled(enabled)
            analyticsTracker.track(AnalyticsEvents.smartSuggestionsToggled(enabled))
        }
    }

    fun onUpgradeClicked() {
        launchSafely(showLoading = false) {
            proUpgradeGate.requirePro(ProFeature.GENERAL)
        }
    }

    // The redemption itself happens in the store; the app only counts the tap.
    fun onRedeemCodeClicked() {
        analyticsTracker.track(AnalyticsEvents.redeemCodeClicked())
    }

    fun onProOverrideSelected(override: ProOverride) {
        launchSafely(showLoading = false) {
            setProOverride(override)
        }
    }

    // The picker only opens once the gate has passed, so a free user meets
    // the paywall before choosing files.
    fun onImportAudioClicked() {
        launchSafely(showLoading = false) {
            if (!proUpgradeGate.requirePro(ProFeature.AUDIO_IMPORT)) return@launchSafely
            mutableUiState.update { it.copy(isImportPickerRequested = true) }
        }
    }

    fun onImportPickerOpened() {
        mutableUiState.update { it.copy(isImportPickerRequested = false) }
    }

    // unreadableCount: files the picker handed over that could not be staged.
    fun onAudioImportSelected(sources: List<AudioImportSource>, unreadableCount: Int) {
        if (sources.isEmpty() && unreadableCount == 0) return
        launchSafely(showLoading = false) {
            val result = importAudio(sources)
            val notice = importNotice(
                hasUnreadable = unreadableCount > 0,
                startedCount = if (result == ImportAudioResult.STARTED) sources.size else 0,
            )
            mutableUiState.update { it.copy(importNotice = notice) }
        }
    }

    private fun importNotice(hasUnreadable: Boolean, startedCount: Int): ImportNoticeUi? = when {
        hasUnreadable -> ImportNoticeUi.Unreadable
        startedCount > 0 -> ImportNoticeUi.Started(startedCount)
        else -> null
    }

    fun onImportNoticeDismissed() {
        mutableUiState.update { it.copy(importNotice = null) }
    }
}

private fun ProStatus.toUi(): ProStatusUi = when {
    plan == ProPlan.LIFETIME -> ProStatusUi.Lifetime
    plan == ProPlan.YEARLY && isTrial -> ProStatusUi.Trial(renewsAtMs)
    plan == ProPlan.YEARLY -> ProStatusUi.Yearly(renewsAtMs)
    else -> ProStatusUi.Free
}
