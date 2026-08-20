import LocalAuthentication
import SwiftUI

struct LockScreenView: View {
    let onAuthenticated: () -> Void

    @State private var isAuthenticating = false
    @State private var didFail = false

    var body: some View {
        VStack(spacing: 16) {
            Spacer()
            Image(systemName: "lock.shield.fill")
                .font(.system(size: 56))
                .foregroundStyle(Brand.primary)
                .padding(.bottom, 8)
            Text(String(localized: "Offhand is locked"))
                .font(.title.bold())
                .multilineTextAlignment(.center)
            Text(String(localized: "Your notes stay locked until this device confirms it's you."))
                .font(.body)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
            if didFail {
                Button(String(localized: "Try again")) { authenticate() }
                    .buttonStyle(.borderedProminent)
                    .controlSize(.large)
                    .tint(Brand.primary)
                    .padding(.top, 8)
            }
            Spacer()
        }
        .padding(.horizontal, 24)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color(.systemGroupedBackground))
        .task { authenticate() }
    }

    private func authenticate() {
        guard !isAuthenticating else { return }
        isAuthenticating = true
        didFail = false
        let context = LAContext()
        context.localizedFallbackTitle = ""
        context.evaluatePolicy(
            .deviceOwnerAuthentication,
            localizedReason: String(localized: "Unlock Offhand to open your notes.")
        ) { success, _ in
            Task { @MainActor in
                isAuthenticating = false
                if success {
                    onAuthenticated()
                } else {
                    didFail = true
                }
            }
        }
    }
}
