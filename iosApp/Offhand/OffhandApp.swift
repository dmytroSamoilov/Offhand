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
    }

    var body: some Scene {
        WindowGroup {
            RootView()
        }
    }
}
