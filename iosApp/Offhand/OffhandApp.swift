import OffhandShared
import SwiftUI

@main
struct OffhandApp: App {
    #if UI_TEST
    private static let useSmokeFakes = true
    #else
    private static let useSmokeFakes = false
    #endif

    init() {
        SharedModulesKt.startSharedKoin(
            deps: IosPlatformDeps(
                gemmaEngine: GemmaEngineImpl(),
                whisperEngine: WhisperEngineImpl(),
                audioSource: MicAudioSource(),
                noteTitleTemplate: String(localized: "Note %d"),
                untitledNoteTitle: String(localized: "Voice note"),
                shareLabels: NoteShareLabels(
                    title: String(localized: "Title"),
                    date: String(localized: "Date"),
                    overview: String(localized: "Overview"),
                    transcript: String(localized: "Transcript")
                ),
                shareFallbackTitle: String(localized: "Recording"),
                appVersion: Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "",
                backupCrypto: BackupCryptoImpl()
            ),
            useSmokeFakes: Self.useSmokeFakes
        )
        TelemetryController.shared.start()
    }

    var body: some Scene {
        WindowGroup {
            RootView()
        }
    }
}
