import SwiftUI

private struct PrivacyShield: ViewModifier {
    @Environment(\.scenePhase) private var scenePhase

    func body(content: Content) -> some View {
        content
            .overlay {
                if scenePhase != .active {
                    cover
                }
            }
    }

    private var cover: some View {
        ZStack {
            Rectangle()
                .fill(Color(.systemGroupedBackground))
                .ignoresSafeArea()
            Image(systemName: "lock.shield.fill")
                .font(.system(size: 44))
                .foregroundStyle(Brand.primary)
        }
        .transition(.opacity)
    }
}

extension View {
    func privacyShielded() -> some View {
        modifier(PrivacyShield())
    }
}
