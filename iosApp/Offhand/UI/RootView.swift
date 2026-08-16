import OffhandShared
import SwiftUI
import UserNotifications

struct RootView: View {
    private let viewModel = SharedGraph.shared.rootViewModel()
    @State private var isOnboardingCompleted: Bool?
    @Environment(\.scenePhase) private var scenePhase

    var body: some View {
        Group {
            switch isOnboardingCompleted {
            case .none:
                ProgressView()
            case .some(false):
                OnboardingView {
                    isOnboardingCompleted = true
                    viewModel.onReady()
                }
            case .some(true):
                MainTabView()
                    .onAppear { viewModel.onReady() }
            }
        }
        .task {
            for await completed in viewModel.isOnboardingCompleted {
                if let completed {
                    isOnboardingCompleted = completed.boolValue
                }
            }
        }
        .onChange(of: scenePhase) {
            switch scenePhase {
            case .background:
                handleBackgrounded()
            case .active:
                UNUserNotificationCenter.current().removeAllPendingNotificationRequests()
                viewModel.onReady()
            default:
                break
            }
        }
    }

    private func handleBackgrounded() {
        let sessionManager = SharedGraph.shared.sessionManager()
        let hasPendingWork = !sessionManager.processingNoteIds.value.isEmpty
        if hasPendingWork {
            scheduleReopenReminder()
        } else if sessionManager.session.value.phase == .idle {
            SharedGraph.shared.modelManager().release()
        }
    }

    private func scheduleReopenReminder() {
        let center = UNUserNotificationCenter.current()
        center.requestAuthorization(options: [.alert, .sound]) { granted, _ in
            guard granted else { return }
            let content = UNMutableNotificationContent()
            content.title = String(localized: "Your note isn't finished")
            content.body = String(localized: "Open Offhand to finish preparing your note.")
            let trigger = UNTimeIntervalNotificationTrigger(timeInterval: 2, repeats: false)
            center.add(UNNotificationRequest(identifier: "note-pending", content: content, trigger: trigger))
        }
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
