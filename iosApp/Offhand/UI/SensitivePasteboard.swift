import UIKit
import UniformTypeIdentifiers

enum SensitivePasteboard {
    private static let lifetime: TimeInterval = 60 * 60

    static func copy(_ text: String) {
        UIPasteboard.general.setItems(
            [[UTType.utf8PlainText.identifier: text]],
            options: [.localOnly: true, .expirationDate: Date().addingTimeInterval(lifetime)]
        )
    }
}
