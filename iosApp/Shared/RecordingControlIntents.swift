import AppIntents

enum RecordingCommand {
    case pause
    case resume
    case stop
}

// A LiveActivityIntent runs in the owning app's process, not the widget's. The
// app installs this handler at launch; the widget target compiles the types but
// never executes them, which keeps the extension free of the shared framework.
enum RecordingControlBridge {
    nonisolated(unsafe) static var handler: ((RecordingCommand) -> Void)?

    @MainActor
    static func send(_ command: RecordingCommand) {
        handler?(command)
    }
}

struct PauseRecordingIntent: LiveActivityIntent {
    static var title: LocalizedStringResource = "Pause recording"

    func perform() async throws -> some IntentResult {
        await RecordingControlBridge.send(.pause)
        return .result()
    }
}

struct ResumeRecordingIntent: LiveActivityIntent {
    static var title: LocalizedStringResource = "Resume recording"

    func perform() async throws -> some IntentResult {
        await RecordingControlBridge.send(.resume)
        return .result()
    }
}

struct StopRecordingIntent: LiveActivityIntent {
    static var title: LocalizedStringResource = "Stop recording"

    func perform() async throws -> some IntentResult {
        await RecordingControlBridge.send(.stop)
        return .result()
    }
}
