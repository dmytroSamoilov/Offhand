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
    // what counts as a developer build (developer options, the Pro override):
    // any debug build, or the dev flavor, whose TestFlight builds are release.
    #if DEBUG
    private static let isDeveloperBuild = true
    #else
    private static let isDeveloperBuild = Bundle.main.bundleIdentifier?.hasSuffix(".dev") == true
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
                isDeveloperBuild: Self.isDeveloperBuild
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
