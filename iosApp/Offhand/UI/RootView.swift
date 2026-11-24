import OffhandShared
import SwiftUI

struct RootView: View {
    private let viewModel = AppViewModels.root
    private let sessionManager = SharedGraph.shared.sessionManager()
    private let proUpgradeGate = SharedGraph.shared.proUpgradeGate()
    private let aiCoreDownloadStatus = SharedGraph.shared.aiCoreDownloadStatus()
    private let sharedAudioImport = AppViewModels.sharedAudioImport
    @State private var sharedImportState = SharedAudioImportUiState(pendingSources: [], pendingUnreadableCount: 0, notice: nil)
    @State private var activityController = NoteActivityController()
    @State private var downloadActivityController = AiCoreDownloadActivityController()
    @State private var finishCoordinator = NoteFinishCoordinator()
    @State private var activeNoteId: Int64?
    @State private var settledNotes: [Int64: Bool] = [:]
    @State private var phase: IosRootPhase = .loading
    @State private var selectedTab = BottomBarDestination.notes
    @State private var isPaywallPresented = false
    @State private var isEarlyAdopterThanksShown = false
    @State private var showsBenefitsAfterThanks = false
    @State private var trialNotice: TrialNoticeItem?
    @State private var showsOfferAfterTrialNotice = false
    @ObservedObject private var notifications = NoteNotifications.shared
    @ObservedObject private var shortcuts = ShortcutRequests.shared
    @Environment(\.scenePhase) private var scenePhase

    var body: some View {
        Group {
            switch phase {
            case .loading:
                ProgressView()
                    .tint(OffhandColor.ink2)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .background(OffhandColor.ground.ignoresSafeArea())
            case .onboarding:
                OnboardingView { viewModel.onReady() }
            case .locked:
                LockScreenView { viewModel.onUnlockAuthenticated() }
            case .ready:
                MainTabView(selectedTab: $selectedTab)
                    .onAppear { viewModel.onReady() }
            }
        }
        // Any shared ViewModel that calls ProUpgradeGate.requirePro() raises this
        // cover; closing it resumes that call with the store's answer.
        .fullScreenCover(isPresented: $isPaywallPresented, onDismiss: { AppViewModels.paywall.onClosed() }) {
            PaywallView()
        }
        .task {
            for await newPhase in viewModel.phase {
                phase = newPhase
            }
        }
        .task {
            for await feature in proUpgradeGate.requestedFeature {
                isPaywallPresented = feature != nil
            }
        }
        .task {
            for await isShown in viewModel.isEarlyAdopterThanksShown {
                isEarlyAdopterThanksShown = isShown.boolValue
            }
        }
        .sheet(isPresented: earlyAdopterThanksBinding, onDismiss: showBenefitsIfRequested) {
            EarlyAdopterThanksSheet(
                onShowBenefits: {
                    showsBenefitsAfterThanks = true
                    viewModel.onEarlyAdopterThanksDismissed()
                },
                onDismiss: { viewModel.onEarlyAdopterThanksDismissed() }
            )
        }
        .task {
            for await notice in viewModel.trialNotice {
                trialNotice = notice == .removed ? TrialNoticeItem(notice: .removed) : nil
            }
        }
        .sheet(item: trialNoticeBinding, onDismiss: showOfferIfRequested) { item in
            TrialRemovedSheet(
                onUpgrade: {
                    showsOfferAfterTrialNotice = true
                    viewModel.onTrialNoticeDismissed()
                },
                onDismiss: { viewModel.onTrialNoticeDismissed() }
            )
        }
        .task { await observeSession() }
        .task { await observeProcessingIds() }
        .task { await observeProgress() }
        .task { await observeProcessingEvents() }
        .task { await observeAiCoreDownload() }
        .task {
            for await newState in sharedAudioImport.uiState {
                sharedImportState = newState
            }
        }
        // Audio shared from another app lands here as a file URL (see
        // CFBundleDocumentTypes); the share sheet gave no hint that import is
        // Pro, so a free user is asked before the paywall.
        .onOpenURL { url in
            if openRecordRequest(url) { return }
            guard url.isFileURL else { return }
            let staged = AudioFileStaging.stage(url)
            sharedAudioImport.onSharedAudioReceived(
                sources: [staged].compactMap { $0 },
                unreadableCount: staged == nil ? 1 : 0
            )
        }
        .offhandDialog(isPresented: proImportBinding) {
            OffhandDialog(
                title: String(localized: "Import with Offhand Pro"),
                message: String(localized: "Turning recordings and videos from other apps into notes is part of Offhand Pro. Upgrade to import the shared file."),
                confirmLabel: String(localized: "Upgrade"),
                onConfirm: { sharedAudioImport.onUpgradeClicked() },
                dismissLabel: String(localized: "Cancel"),
                onDismiss: { sharedAudioImport.onImportDeclined() }
            )
        }
        .offhandDialog(isPresented: sharedImportNoticeBinding) {
            OffhandDialog(
                title: sharedImportNoticeTitle,
                message: sharedImportState.notice.map { ImportNoticeText.message($0) },
                confirmLabel: String(localized: "OK"),
                onConfirm: { sharedAudioImport.onNoticeDismissed() },
                onDismiss: { sharedAudioImport.onNoticeDismissed() }
            )
        }
        .onAppear {
            NoteFinishCoordinator.onExpired = { [activityController] in
                activityController.suspendedWithPendingWork()
            }
            notifications.register()
            activityController.endLeftovers()
        }
        .onChange(of: shortcuts.pending) { runPendingShortcut() }
        .onChange(of: phase) { runPendingShortcut() }
        .onChange(of: notifications.noteIdToOpen) {
            guard let noteId = notifications.noteIdToOpen else { return }
            notifications.noteIdToOpen = nil
            selectedTab = .notes
            AppViewModels.notes.onNoteSelected(id: noteId)
        }
        .onReceive(
            NotificationCenter.default.publisher(
                for: UIApplication.didReceiveMemoryWarningNotification
            )
        ) { _ in
            releaseModelIfIdle()
        }
        .onChange(of: scenePhase) {
            switch scenePhase {
            case .background:
                handleBackgrounded()
            case .active:
                // A code redeemed in the App Store app lands as a transaction;
                // re-reading on activation shows it without a restart.
                SharedGraph.shared.refreshProStatus()
                finishCoordinator.appBecameActive()
                // Only the come-back reminder is stale on activation; note-ready
                // notifications must survive it.
                notifications.clearKeepOpenReminder()
                viewModel.onReady()
            default:
                break
            }
        }
    }

    // The home-screen widget's Record opens the app with this link: without
    // Pro the paywall comes first, and the record sheet asks for the
    // microphone and starts recording into the widget's folder. Only the widget's own scheme and host are
    // accepted, and the folder is a number or nothing.
    private func openRecordRequest(_ url: URL) -> Bool {
        guard !RecordWidgetShared.urlScheme.isEmpty,
              url.scheme == RecordWidgetShared.urlScheme,
              url.host == RecordWidgetShared.recordHost else { return false }
        let folderId = URLComponents(url: url, resolvingAgainstBaseURL: false)?
            .queryItems?
            .first { $0.name == RecordWidgetShared.folderQueryItem }?
            .value
            .flatMap { Int64($0) }
        viewModel.onWidgetRecordingRequested {
            shortcuts.requestedFolder = folderId.map { .folder($0) } ?? .unsorted
            shortcuts.requestedOrigin = .widget
            shortcuts.pending = .startRecording
        }
        return true
    }

    // A shortcut fired while the app was locked or still loading waits here
    // until the notes list can act on it.
    private func runPendingShortcut() {
        guard phase == .ready, let request = shortcuts.pending else { return }
        shortcuts.pending = nil
        switch request {
        case .startRecording:
            AppViewModels.recording.onRecordingExpectedFrom(origin: shortcuts.takeRequestedOrigin().analyticsOrigin)
            selectedTab = .notes
            shortcuts.recordSheetRequestId += 1
        case .stopRecording:
            if sessionManager.session.value.phase != .idle {
                AppViewModels.recording.onStopRecording()
            }
        }
    }

    // The paywall is a full-screen cover on this view, which a sheet cannot
    // raise, so the benefits open once the thank-you sheet has gone.
    private func showBenefitsIfRequested() {
        guard showsBenefitsAfterThanks else { return }
        showsBenefitsAfterThanks = false
        viewModel.onProBenefitsRequested()
    }

    // Same reason as the thank-you: the paywall opens once the sheet is gone.
    private func showOfferIfRequested() {
        guard showsOfferAfterTrialNotice else { return }
        showsOfferAfterTrialNotice = false
        viewModel.onProBenefitsRequested()
    }

    private var trialNoticeBinding: Binding<TrialNoticeItem?> {
        Binding(
            get: { trialNotice },
            set: { item in if item == nil { viewModel.onTrialNoticeDismissed() } }
        )
    }

    private var earlyAdopterThanksBinding: Binding<Bool> {
        Binding(
            get: { isEarlyAdopterThanksShown },
            set: { isShown in if !isShown { viewModel.onEarlyAdopterThanksDismissed() } }
        )
    }

    // The session only carries its note once the recorder has drained, so a
    // stopped recording first shows as being prepared and is settled when the
    // session is idle again: a discarded or too short one has no note and
    // ends the Live Activity there, a saved one waits for its outcome.
    private func observeSession() async {
        var previousPhase = SessionPhase.idle
        var previousPaused = false
        var isAwaitingSavedNote = false
        for await session in sessionManager.session {
            let phase = session.phase
            if phase == .recording {
                if previousPhase != .recording {
                    activeNoteId = nil
                    isAwaitingSavedNote = false
                    activityController.recordingStarted()
                } else if session.isPaused != previousPaused {
                    activityController.recordingPaused(session.isPaused)
                }
            } else if previousPhase == .recording {
                isAwaitingSavedNote = true
                activityController.processingProgressed(percent: nil)
            }
            if isAwaitingSavedNote && phase == .failed {
                isAwaitingSavedNote = false
                activityController.cancelled()
            } else if isAwaitingSavedNote && phase == .idle {
                isAwaitingSavedNote = false
                recordingSettled(noteId: session.noteId?.int64Value)
            }
            previousPhase = phase
            previousPaused = session.isPaused
        }
    }

    // A note that needs no writing (transcription switched off) is finished
    // before this runs, so its outcome may already be known.
    private func recordingSettled(noteId: Int64?) {
        guard let noteId else {
            activityController.cancelled()
            return
        }
        if let isCompleted = settledNotes.removeValue(forKey: noteId) {
            endActivity(isCompleted: isCompleted)
        } else if finishCoordinator.isManaging(noteId: noteId) {
            activityController.cancelled()
        } else {
            activeNoteId = noteId
        }
    }

    private func endActivity(isCompleted: Bool) {
        if isCompleted {
            activityController.finished()
        } else {
            activityController.cancelled()
        }
    }

    // A note that leaves processing without an outcome is waiting for the
    // on-device AI to download; nothing is being worked on, so the Live
    // Activity has nothing to show. A failure is reported just after the
    // note leaves, which is what the short wait is for, and a note that was
    // picked up again in the meantime is left alone.
    private func endActivityIfLeftWaiting(noteId: Int64) {
        Task { @MainActor in
            try? await Task.sleep(for: .seconds(1))
            let processing = (sessionManager.processingNoteIds.value as NSSet)
                .compactMap { ($0 as? NSNumber)?.int64Value }
            guard activeNoteId == noteId, !processing.contains(noteId) else { return }
            activeNoteId = nil
            activityController.cancelled()
        }
    }

    private func observeProcessingIds() async {
        var previousIds: Set<Int64> = []
        for await ids in sessionManager.processingNoteIds {
            // Iterated untyped for the same reason NoteFinishCoordinator.drive
            // reads noteProgress through NSDictionary: element-typed iteration
            // of a bridged Kotlin number collection force-casts and can abort.
            let current = Set((ids as NSSet).compactMap { ($0 as? NSNumber)?.int64Value })
            for added in current.subtracting(previousIds) {
                finishCoordinator.processingStarted(noteId: added)
                if finishCoordinator.isManaging(noteId: added) {
                    activityController.cancelled()
                } else if activeNoteId == nil {
                    activeNoteId = added
                    activityController.processingStartedWithoutRecording()
                }
            }
            for removed in previousIds.subtracting(current) {
                finishCoordinator.processingEnded(noteId: removed)
                endActivityIfLeftWaiting(noteId: removed)
            }
            if current.isEmpty && !previousIds.isEmpty {
                finishCoordinator.allProcessingFinished()
            }
            previousIds = current
        }
    }

    private func observeProcessingEvents() async {
        for await event in sessionManager.events {
            let eventNoteId: Int64
            switch onEnum(of: event) {
            case .completed(let completed):
                eventNoteId = completed.noteId
                notifications.noteReady(noteId: completed.noteId)
            case .failed(let failed):
                eventNoteId = failed.noteId
                notifications.noteFailed(noteId: failed.noteId)
            case .interrupted(let interrupted):
                eventNoteId = interrupted.noteId
                notifications.notePaused(noteId: interrupted.noteId)
            case .importRejected:
                continue
            }
            let isCompleted: Bool
            switch onEnum(of: event) {
            case .completed:
                isCompleted = true
            case .failed, .interrupted, .importRejected:
                isCompleted = false
            }
            guard eventNoteId == activeNoteId else {
                settledNotes = [eventNoteId: isCompleted]
                continue
            }
            endActivity(isCompleted: isCompleted)
            activeNoteId = nil
        }
    }

    private func observeProgress() async {
        for await progress in sessionManager.noteProgress {
            guard let noteId = activeNoteId,
                  sessionManager.session.value.phase != .recording,
                  let percent = progress[KotlinLong(value: noteId)] else { continue }
            activityController.processingProgressed(percent: percent.intValue)
        }
    }

    // Idle after downloading means the run ended; the LLM downloads last, so its
    // file on disk tells success from a drop the next foreground will resume.
    private func observeAiCoreDownload() async {
        var wasDownloading = false
        for await state in aiCoreDownloadStatus.state {
            switch onEnum(of: state) {
            case .downloading(let downloading):
                wasDownloading = true
                downloadActivityController.progressed(percent: Int(downloading.progressPercent))
            case .idle, .waitingForMobileData:
                guard wasDownloading else { continue }
                wasDownloading = false
                let isReady = (try? await SharedGraph.shared.modelManager().isModelDownloaded())?.boolValue ?? false
                downloadActivityController.finished(isReady: isReady)
            }
        }
    }

    private var proImportBinding: Binding<Bool> {
        Binding(
            get: { phase == .ready && sharedImportState.isProDialogShown },
            set: { isShown in if !isShown { sharedAudioImport.onImportDeclined() } }
        )
    }

    private var sharedImportNoticeBinding: Binding<Bool> {
        Binding(
            get: { phase == .ready && sharedImportState.notice != nil },
            set: { isShown in if !isShown { sharedAudioImport.onNoticeDismissed() } }
        )
    }

    private var sharedImportNoticeTitle: String {
        ImportNoticeText.title(sharedImportState.notice)
    }

    private func handleBackgrounded() {
        let session = sessionManager.session.value
        let hasPendingWork = !sessionManager.processingNoteIds.value.isEmpty || session.phase == .draining
        if hasPendingWork {
            // With a system task carrying the work there is nothing to come
            // back for; if that task is cut short, its own notice says so.
            if !NoteFinishCoordinator.isContinuingInBackground { notifications.remindToKeepOpen() }
            finishCoordinator.appEnteredBackgroundWhileProcessing()
            finishCoordinator.appEnteredBackgroundWithPendingWork()
        } else {
            releaseModelIfIdle()
        }
        // Re-lock on the way out, except mid-recording: swapping in the lock
        // screen tears down the record sheet and strands the live capture.
        if session.phase != .recording {
            viewModel.lockOnBackground()
        }
    }

    // The engine holds gigabytes; give it back rather than risk the OS killing us.
    private func releaseModelIfIdle() {
        guard sessionManager.processingNoteIds.value.isEmpty,
              sessionManager.session.value.phase == .idle else { return }
        SharedGraph.shared.modelManager().release()
    }
}

struct MainTabView: View {
    @Binding var selectedTab: BottomBarDestination

    var body: some View {
        ZStack {
            NotesListView { bottomBar }
                .shownAsTab(selectedTab == .notes)
            SettingsView { bottomBar }
                .shownAsTab(selectedTab == .settings)
        }
        .background(OffhandColor.ground.ignoresSafeArea())
    }

    private var bottomBar: some View {
        BottomBar(
            selected: selectedTab,
            labels: BottomBarLabels(
                notes: String(localized: "Notes"),
                settings: String(localized: "Settings"),
                record: String(localized: "Record"),
                recordAccessibilityLabel: String(localized: "Record a note")
            ),
            onNotes: { selectedTab = .notes },
            onRecord: startRecording,
            onSettings: { selectedTab = .settings }
        )
    }

    private func startRecording() {
        Haptics.confirm()
        selectedTab = .notes
        ShortcutRequests.shared.recordSheetRequestId += 1
    }
}

private extension ShortcutRequests.Origin {
    var analyticsOrigin: RecordingOrigin {
        switch self {
        case .shortcut: return .shortcut
        case .widget: return .widget
        case .onboarding: return .onboarding
        }
    }
}

private extension View {
    func shownAsTab(_ isShown: Bool) -> some View {
        opacity(isShown ? 1 : 0)
            .allowsHitTesting(isShown)
            .accessibilityHidden(!isShown)
    }
}

private struct TrialNoticeItem: Identifiable {
    let notice: ProTrialNotice

    var id: String { notice.name }
}

private struct TrialRemovedSheet: View {
    let onUpgrade: () -> Void
    let onDismiss: () -> Void

    var body: some View {
        ProNoticeSheet(
            title: String(localized: "Your Pro trial was removed"),
            message: String(localized: "The date on this iPhone was set back past the start of your trial, so the trial has ended. You can keep using the free version or upgrade to Offhand Pro."),
            primaryLabel: String(localized: "See Offhand Pro"),
            onPrimary: onUpgrade,
            secondaryLabel: String(localized: "Continue with the free version"),
            onSecondary: onDismiss
        )
    }
}

private struct EarlyAdopterThanksSheet: View {
    let onShowBenefits: () -> Void
    let onDismiss: () -> Void

    var body: some View {
        ProNoticeSheet(
            title: String(localized: "Offhand Pro is yours"),
            message: String(localized: "You were here before 1.5, while Offhand was still rough. Thank you for testing it with me. As a thank-you, everything in Offhand Pro is unlocked on this iPhone, for good. No purchase, no account.\n\nOne thing to know: Pro lives on this iPhone only. If you delete or reinstall Offhand, or move to a new iPhone, it will not come back."),
            primaryLabel: String(localized: "Show Pro benefits"),
            onPrimary: onShowBenefits,
            secondaryLabel: String(localized: "Got it"),
            onSecondary: onDismiss
        )
    }
}

private struct ProNoticeSheet: View {
    let title: String
    let message: String
    let primaryLabel: String
    let onPrimary: () -> Void
    let secondaryLabel: String
    let onSecondary: () -> Void

    var body: some View {
        OffhandSheet {
            VStack(alignment: .leading, spacing: 0) {
                ProCrown(size: 40)
                    .padding(.top, 8)
                SheetHeading(title)
                    .padding(.top, 20)
                Text(message)
                    .offhandText(.dialogBody)
                    .foregroundStyle(OffhandColor.ink2)
                    .fixedSize(horizontal: false, vertical: true)
                    .padding(.top, 10)
                PrimaryButton(primaryLabel, fillsWidth: true, action: onPrimary)
                    .padding(.top, 24)
                PlainTextButton(secondaryLabel, tone: .muted, fillsWidth: true, action: onSecondary)
                    .padding(.top, 4)
            }
        }
    }
}
