import LocalAuthentication
import SwiftUI

enum DeviceAuthenticator {
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

    @Environment(\.scenePhase) private var scenePhase
    @State private var isAuthenticating = false
    @State private var didFail = false

    var body: some View {
        VStack(spacing: 0) {
            Spacer()
            VStack(spacing: 14) {
                Image(systemName: "lock")
                    .font(.system(size: 30, weight: .regular))
                    .foregroundStyle(OffhandColor.ink)
                    .frame(width: 80, height: 80)
                    .background(OffhandColor.surface, in: Circle())
                    .overlay { Circle().strokeBorder(OffhandColor.border, lineWidth: 1) }
                    .accessibilityHidden(true)
                Text(String(localized: "Offhand is locked"))
                    .offhandText(.title)
                    .padding(.top, 14)
                    .accessibilityAddTraits(.isHeader)
                Text(String(localized: "Your notes stay locked until your iPhone confirms it's you."))
                    .offhandText(.dialogBody)
                    .foregroundStyle(OffhandColor.ink2)
                    .frame(maxWidth: 280)
            }
            .multilineTextAlignment(.center)
            Spacer()
            PrimaryButton(String(localized: "Unlock"), systemImage: "lock.open", fillsWidth: true, action: authenticate)
                .disabled(isAuthenticating)
        }
        .foregroundStyle(OffhandColor.ink)
        .padding(.horizontal, OffhandSpacing.screen)
        .padding(.bottom, 28)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(OffhandColor.ground.ignoresSafeArea())
        .environment(\.colorScheme, .dark)
        .task { if scenePhase == .active { authenticate() } }
        .onChange(of: scenePhase) {
            if scenePhase == .active && !isAuthenticating && !didFail {
                authenticate()
            }
        }
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
