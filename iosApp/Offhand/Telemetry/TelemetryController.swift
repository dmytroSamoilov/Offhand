import FirebaseAnalytics
import FirebaseCore
import FirebaseCrashlytics
import Foundation
import OffhandShared
import os

// Follows the stored telemetry consent, like the Android TelemetryController.
//
// It goes one step further than Android: Firebase is not configured at all until
// consent is granted. Merely calling FirebaseApp.configure() makes Firebase reach
// firebase-settings.crashlytics.com and firebaselogging-pa.googleapis.com, which
// is not something a "nothing leaves your device" app should do before the user
// has said yes. The trade is a slightly later start, so a crash in the first
// moments of launch may go unreported.
final class TelemetryController {

    static let shared = TelemetryController()

    private static let logger = Logger(
        subsystem: Bundle.main.bundleIdentifier ?? "com.dmytrosamoilov.offhand",
        category: "Telemetry"
    )

    private var task: Task<Void, Never>?

    func start() {
        guard task == nil else { return }
        task = Task { @MainActor in
            for await granted in SharedGraph.shared.observeTelemetryConsent().invoke() {
                apply(isGranted: granted.boolValue)
            }
        }
    }

    @MainActor
    private func apply(isGranted: Bool) {
        guard isGranted else {
            // If Firebase was never configured there is nothing to turn off, and
            // nothing has been sent.
            if FirebaseApp.app() != nil {
                Crashlytics.crashlytics().setCrashlyticsCollectionEnabled(false)
                Crashlytics.crashlytics().deleteUnsentReports()
                Analytics.setAnalyticsCollectionEnabled(false)
            }
            return
        }
        guard configureIfNeeded() else { return }
        Crashlytics.crashlytics().setCrashlyticsCollectionEnabled(true)
        Analytics.setAnalyticsCollectionEnabled(true)
    }

    // A build without GoogleService-Info.plist is the normal state of a fresh
    // clone, so treat it as "telemetry unavailable" rather than crashing.
    private func configureIfNeeded() -> Bool {
        if FirebaseApp.app() != nil { return true }
        guard Bundle.main.path(forResource: "GoogleService-Info", ofType: "plist") != nil else {
            Self.logger.notice("No Firebase config in this build; telemetry stays off.")
            return false
        }
        FirebaseApp.configure()
        return FirebaseApp.app() != nil
    }
}
