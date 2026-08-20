import OffhandShared

// Koin registers the shared ViewModels as factories, so every SharedGraph call
// builds a new one. SwiftUI re-runs a View's stored-property initializers each
// time the struct is rebuilt, which otherwise leaves the UI observing an
// abandoned instance while taps drive a newer one — and leaks the coroutines
// each abandoned instance started. These hold one instance per screen.
enum AppViewModels {
    static let root = SharedGraph.shared.rootViewModel()
    static let notes = SharedGraph.shared.notesViewModel()
    static let recording = SharedGraph.shared.recordingViewModel()
    static let onboarding = SharedGraph.shared.onboardingViewModel()
    static let settings = SharedGraph.shared.settingsViewModel()
}
