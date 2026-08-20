import SwiftUI
import UIKit

// iOS has no FLAG_SECURE equivalent — a screenshot cannot be blocked by any public
// API. What is available is covering the app-switcher snapshot and reacting to an
// active screen recording or mirror, which is what this does.
private struct PrivacyShield: ViewModifier {
    @Environment(\.scenePhase) private var scenePhase
    @State private var isCaptured = UIScreen.main.isCaptured

    func body(content: Content) -> some View {
        content
            .overlay {
                if isObscured {
                    cover
                }
            }
            .onReceive(
                NotificationCenter.default.publisher(for: UIScreen.capturedDidChangeNotification)
            ) { _ in
                isCaptured = UIScreen.main.isCaptured
            }
    }

    private var isObscured: Bool {
        scenePhase != .active || isCaptured
    }

    private var cover: some View {
        ZStack {
            Rectangle()
                .fill(Color(.systemGroupedBackground))
                .ignoresSafeArea()
            VStack(spacing: 12) {
                Image(systemName: "lock.shield.fill")
                    .font(.system(size: 44))
                    .foregroundStyle(Brand.primary)
                if isCaptured {
                    Text(String(localized: "Hidden while the screen is being recorded."))
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                        .multilineTextAlignment(.center)
                }
            }
            .padding(24)
        }
        .transition(.opacity)
    }
}

extension View {
    func privacyShielded() -> some View {
        modifier(PrivacyShield())
    }
}
