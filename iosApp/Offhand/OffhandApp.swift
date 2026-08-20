import OffhandShared
import SwiftUI

@main
struct OffhandApp: App {
    init() {
        SharedModulesKt.startSharedKoin(
            deps: IosPlatformDeps(
                gemmaEngine: GemmaEngineImpl(),
                whisperEngine: WhisperEngineImpl(),
                audioSource: MicAudioSource(),
                noteTitleTemplate: String(localized: "Note %d"),
                shareLabels: NoteShareLabels(
                    title: String(localized: "Title"),
                    date: String(localized: "Date"),
                    overview: String(localized: "Overview"),
                    transcript: String(localized: "Transcript")
                ),
                shareFallbackTitle: String(localized: "Recording")
            )
        )
        installRecordingControls()
        CrashReporting.shared.start()
    }

    // Lets the Live Activity's buttons drive the same session the in-app
    // transport does, without linking the shared framework into the widget.
    private func installRecordingControls() {
        RecordingControlBridge.handler = { command in
            let sessionManager = SharedGraph.shared.sessionManager()
            switch command {
            case .pause: sessionManager.pause()
            case .resume: sessionManager.resume()
            case .stop: sessionManager.stop()
            }
        }
    }

    var body: some Scene {
        WindowGroup {
            RootView()
        }
    }
}
