import UIKit

// Nothing is felt while the app is not in front: a loop that outlives the
// foreground, such as the tick of a note being prepared, stays silent.
enum Haptics {
    static func tap() {
        guard isAppActive else { return }
        UIImpactFeedbackGenerator(style: .light).impactOccurred()
    }

    static func confirm() {
        guard isAppActive else { return }
        UIImpactFeedbackGenerator(style: .medium).impactOccurred()
    }

    static func success() {
        guard isAppActive else { return }
        UINotificationFeedbackGenerator().notificationOccurred(.success)
    }

    static func warning() {
        guard isAppActive else { return }
        UINotificationFeedbackGenerator().notificationOccurred(.warning)
    }

    static func selection() {
        guard isAppActive else { return }
        UISelectionFeedbackGenerator().selectionChanged()
    }

    private static var isAppActive: Bool { UIApplication.shared.applicationState == .active }
}
