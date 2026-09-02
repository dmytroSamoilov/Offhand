import LocalAuthentication
import SwiftUI

// Housed here rather than its own file because the generated Xcode project
// only picks up new files on the next `xcodegen generate`.
enum DeviceAuthenticator {
    // A device that can never authenticate (no passcode, no usable biometry)
    // reports success: failing a check that cannot pass would strand the user,
    // and a lock on such a device is unenforceable anyway.
    static func confirmOwner(reason: String, completion: @escaping @MainActor (Bool) -> Void) {
        let context = LAContext()
        context.localizedFallbackTitle = ""
        context.evaluatePolicy(.deviceOwnerAuthentication, localizedReason: reason) { success, error in
            Task { @MainActor in
                completion(success || isUnenforceable(error))
            }
        }
    }

    private static func isUnenforceable(_ error: Error?) -> Bool {
        switch (error as? LAError)?.code {
        case .passcodeNotSet, .biometryNotAvailable, .biometryNotEnrolled:
            return true
        default:
            return false
        }
    }
}

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
        DeviceAuthenticator.confirmOwner(
            reason: String(localized: "Unlock Offhand to open your notes.")
        ) { confirmed in
            isAuthenticating = false
            if confirmed {
                onAuthenticated()
            } else {
                didFail = true
            }
        }
    }
}
