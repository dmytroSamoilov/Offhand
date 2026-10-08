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
    @State private var phase: IosRootPhase = .loading
    @State private var selectedTab = 0
    @State private var isPaywallPresented = false
    @State private var isEarlyAdopterThanksShown = false
    @State private var showsBenefitsAfterThanks = false
    @ObservedObject private var notifications = NoteNotifications.shared
    @ObservedObject private var shortcuts = ShortcutRequests.shared
    @Environment(\.scenePhase) private var scenePhase

    var body: some View {
        Group {
            switch phase {
            case .loading:
                ProgressView()
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
            .presentationDetents([.fraction(0.66), .large])
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
            guard url.isFileURL else { return }
            let staged = AudioFileStaging.stage(url)
            sharedAudioImport.onSharedAudioReceived(
                sources: [staged].compactMap { $0 },
                unreadableCount: staged == nil ? 1 : 0
            )
        }
        .alert(String(localized: "Import with Offhand Pro"), isPresented: proImportBinding) {
            Button(String(localized: "Upgrade")) { sharedAudioImport.onUpgradeClicked() }
            Button(String(localized: "Cancel"), role: .cancel) { sharedAudioImport.onImportDeclined() }
        } message: {
            Text(String(localized: "Turning recordings from other apps into notes is part of Offhand Pro. Upgrade to import the shared audio."))
        }
        .alert(sharedImportNoticeTitle, isPresented: sharedImportNoticeBinding) {
            Button(String(localized: "OK")) { sharedAudioImport.onNoticeDismissed() }
        } message: {
            if let notice = sharedImportState.notice {
                Text(ImportNoticeText.message(notice))
            }
        }
        .onAppear {
            NoteFinishCoordinator.onExpired = { [activityController] in
                activityController.suspendedWithPendingWork()
            }
            notifications.register()
        }
        .onChange(of: shortcuts.pending) { runPendingShortcut() }
        .onChange(of: phase) { runPendingShortcut() }
        .onChange(of: notifications.noteIdToOpen) {
            guard let noteId = notifications.noteIdToOpen else { return }
            notifications.noteIdToOpen = nil
            selectedTab = 0
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

    // A shortcut fired while the app was locked or still loading waits here
    // until the notes list can act on it.
    private func runPendingShortcut() {
        guard phase == .ready, let request = shortcuts.pending else { return }
        shortcuts.pending = nil
        switch request {
        case .startRecording:
            selectedTab = 0
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

    private var earlyAdopterThanksBinding: Binding<Bool> {
        Binding(
            get: { isEarlyAdopterThanksShown },
            set: { isShown in if !isShown { viewModel.onEarlyAdopterThanksDismissed() } }
        )
    }

    private func observeSession() async {
        var previousPhase = SessionPhase.idle
        var previousPaused = false
        for await session in sessionManager.session {
            let phase = session.phase
            if phase == .recording {
                if previousPhase != .recording {
                    activityController.recordingStarted()
                } else if session.isPaused != previousPaused {
                    activityController.recordingPaused(session.isPaused)
                }
                if let noteId = session.noteId {
                    activeNoteId = noteId.int64Value
                }
            } else if previousPhase == .recording {
                if phase == .failed {
                    activityController.cancelled()
                    activeNoteId = nil
                } else if let noteId = activeNoteId {
                    finishCoordinator.processingStarted(noteId: noteId)
                    if finishCoordinator.isManaging(noteId: noteId) {
                        activityController.cancelled()
                    } else {
                        activityController.processingProgressed(percent: nil)
                    }
                }
            }
            previousPhase = phase
            previousPaused = session.isPaused
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
            guard eventNoteId == activeNoteId else { continue }
            switch onEnum(of: event) {
            case .completed:
                activityController.finished()
            case .failed, .interrupted, .importRejected:
                activityController.cancelled()
            }
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
            notifications.remindToKeepOpen()
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
    @Binding var selectedTab: Int

    var body: some View {
        TabView(selection: $selectedTab) {
            NotesListView()
                .tabItem { Label(String(localized: "Notes"), systemImage: "list.bullet") }
                .tag(0)
            SettingsView()
                .tabItem { Label(String(localized: "Settings"), systemImage: "gearshape") }
                .tag(1)
        }
        .tint(Brand.primary)
    }
}

// The early-adopter grant: the crown and a primary button keep it from being
// tapped past, and the body says plainly that Pro stays on this iPhone only.
private struct EarlyAdopterThanksSheet: View {
    let onShowBenefits: () -> Void
    let onDismiss: () -> Void

    var body: some View {
        VStack(spacing: 20) {
            ProCrown(size: 56)
                .padding(.top, 8)
            Text(String(localized: "Offhand Pro is yours"))
                .font(.title2.bold())
                .multilineTextAlignment(.center)
            Text(String(localized: "You were here before 1.5, while Offhand was still rough. Thank you for testing it with me. As a thank-you, everything in Offhand Pro is unlocked on this iPhone, for good. No purchase, no account.\n\nOne thing to know: Pro lives on this iPhone only. If you delete or reinstall Offhand, or move to a new iPhone, it will not come back."))
                .font(.body)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
                .fixedSize(horizontal: false, vertical: true)
            Spacer(minLength: 0)
            Button(String(localized: "Show Pro benefits"), action: onShowBenefits)
                .buttonStyle(.borderedProminent)
                .tint(Brand.primary)
                .controlSize(.large)
                .frame(maxWidth: .infinity)
            Button(String(localized: "Got it"), action: onDismiss)
                .buttonStyle(.plain)
                .foregroundStyle(Brand.primary)
        }
        .padding(24)
    }
}
