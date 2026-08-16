import OffhandShared
import SwiftUI

struct RootView: View {
    private let viewModel = SharedGraph.shared.rootViewModel()
    @State private var isOnboardingCompleted: Bool?

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
