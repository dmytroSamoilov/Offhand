import OffhandShared
import SwiftUI

@main
struct OffhandApp: App {
    #if UI_TEST
    private static let useSmokeFakes = true
    #else
    private static let useSmokeFakes = false
    #endif
    // The shared framework is always a release binary, so the app decides
    // what counts as a debug build (developer options, the Pro override).
    #if DEBUG
    private static let isDebugBuild = true
    #else
    private static let isDebugBuild = false
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
                    transcript: String(localized: "Transcript"),
                    recorded: String(localized: "Recorded"),
                    duration: String(localized: "Duration"),
                    createdWith: String(localized: "Created with Offhand"),
                    exported: String(localized: "Exported")
                ),
                shareFallbackTitle: String(localized: "Recording"),
                appVersion: Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "",
                backupCrypto: BackupCryptoImpl(),
                audioDecoder: AudioDecoderImpl(),
                noteDocuments: NoteDocumentBridgeImpl(),
                proStore: StoreKitProStore(),
                isDebugBuild: Self.isDebugBuild
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
