import OffhandShared
import SwiftUI
import UserNotifications

struct RootView: View {
    private let viewModel = AppViewModels.root
    private let sessionManager = SharedGraph.shared.sessionManager()
    @State private var activityController = NoteActivityController()
    @State private var finishCoordinator = NoteFinishCoordinator()
    @State private var activeNoteId: Int64?
    @State private var phase: IosRootPhase = .loading
    @Environment(\.scenePhase) private var scenePhase

    var body: some View {
        Group {
            switch phase {
            case .loading:
                ProgressView()
            case .onboarding:
                OnboardingView { viewModel.onReady() }
            case .locked:
                LockScreenView(skipsAuthentication: DevFlags.skipsAppLock) {
                    viewModel.onUnlockAuthenticated()
                }
            case .ready:
                MainTabView()
                    .onAppear { viewModel.onReady() }
            }
        }
        .privacyShielded()
        .task {
            for await newPhase in viewModel.phase {
                phase = newPhase
            }
        }
        .task { await observeSession() }
        .task { await observeProcessingIds() }
        .task { await observeProgress() }
        .task { await observeProcessingEvents() }
        .onAppear {
            finishCoordinator.onLegacyExpired = { [activityController] in
                activityController.suspendedWithPendingWork()
            }
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
                finishCoordinator.appBecameActive()
                UNUserNotificationCenter.current().removeAllPendingNotificationRequests()
                viewModel.onReady()
            default:
                break
            }
        }
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
            let current = Set(ids.map { $0.int64Value })
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
            case .failed(let failed):
                eventNoteId = failed.noteId
            }
            guard eventNoteId == activeNoteId else { continue }
            switch onEnum(of: event) {
            case .completed:
                activityController.finished()
            case .failed:
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

    private func handleBackgrounded() {
        let session = sessionManager.session.value
        let hasPendingWork = !sessionManager.processingNoteIds.value.isEmpty || session.phase == .draining
        if hasPendingWork {
            finishCoordinator.appEnteredBackgroundWhileProcessing()
        } else {
            releaseModelIfIdle()
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
    var body: some View {
        TabView {
            NotesListView()
                .tabItem { Label(String(localized: "Notes"), systemImage: "list.bullet") }
            SettingsView()
                .tabItem { Label(String(localized: "Settings"), systemImage: "gearshape") }
        }
        .tint(Brand.primary)
    }
}
