import AppIntents
import Foundation

// Siri and the Shortcuts app can start or stop a recording. Both actions open
// the app: a recording needs the microphone session and the record sheet, and
// the lock screen, when it is on, comes first; RootView runs the request once
// the app is READY.
@MainActor
final class ShortcutRequests: ObservableObject {
    static let shared = ShortcutRequests()

    enum Request {
        case startRecording
        case stopRecording
    }

    @Published var pending: Request?
    // Bumped for every start request, so the notes list opens the record
    // sheet even when the previous request was the same.
    @Published var recordSheetRequestId = 0
}

struct StartRecordingIntent: AppIntent {
    static let title: LocalizedStringResource = "Start recording"
    static let description = IntentDescription("Opens Offhand and starts a new recording.")
    static let openAppWhenRun = true

    @MainActor
    func perform() async throws -> some IntentResult {
        ShortcutRequests.shared.pending = .startRecording
        return .result()
    }
}

struct StopRecordingIntent: AppIntent {
    static let title: LocalizedStringResource = "Stop recording"
    static let description = IntentDescription("Opens Offhand and stops the recording in progress.")
    static let openAppWhenRun = true

    @MainActor
    func perform() async throws -> some IntentResult {
        ShortcutRequests.shared.pending = .stopRecording
        return .result()
    }
}

struct OffhandShortcuts: AppShortcutsProvider {
    static var appShortcuts: [AppShortcut] {
        AppShortcut(
            intent: StartRecordingIntent(),
            phrases: ["Start recording in \(.applicationName)", "Record a note in \(.applicationName)"],
            shortTitle: "Start recording",
            systemImageName: "mic.fill"
        )
        AppShortcut(
            intent: StopRecordingIntent(),
            phrases: ["Stop recording in \(.applicationName)"],
            shortTitle: "Stop recording",
            systemImageName: "stop.fill"
        )
    }
}
