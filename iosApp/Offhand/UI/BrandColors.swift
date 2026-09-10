import SwiftUI
import UIKit

enum Brand {
    static let primary = dynamic(light: 0x0B57D0, dark: 0xA8C7FA)
    static let onPrimary = dynamic(light: 0xFFFFFF, dark: 0x062E6F)
    static let onPrimaryContainer = dynamic(light: 0x041E49, dark: 0xD3E3FD)
    static let primaryContainer = dynamic(light: 0xD3E3FD, dark: 0x0842A0)
    static let secondaryContainer = dynamic(light: 0xDBE2F9, dark: 0x3F4759)
    static let onSecondaryContainer = dynamic(light: 0x141B2C, dark: 0xDBE2F9)
    static let teal = dynamic(light: 0x006A60, dark: 0x82D5C8)
    static let warningContainer = dynamic(light: 0xFFDF9E, dark: 0x5C4300)
    static let onWarningContainer = dynamic(light: 0x261A00, dark: 0xFFDF9E)
    static let searchHighlight = dynamic(light: 0xFFDF9E, dark: 0x5C4300)
    static let tertiaryContainer = dynamic(light: 0x9EF2E2, dark: 0x005048)
    static let onTertiaryContainer = dynamic(light: 0x00201C, dark: 0x9EF2E2)
    static let tealContainer = dynamic(light: 0x9EF2E2, dark: 0x003731)
    static let surface = dynamic(light: 0xF6F8FC, dark: 0x101418)
    static let surfaceContainer = dynamic(light: 0xEFF2F9, dark: 0x1C2024)
    static let onSurface = dynamic(light: 0x111418, dark: 0xECEEF3)
    static let onSurfaceVariant = dynamic(light: 0x34383F, dark: 0xCED2DB)

    private static func dynamic(light: UInt32, dark: UInt32) -> Color {
        Color(uiColor: UIColor { traits in
            traits.userInterfaceStyle == .dark ? UIColor(hex: dark) : UIColor(hex: light)
        })
    }
}

private extension UIColor {
    convenience init(hex: UInt32) {
        self.init(
            red: CGFloat((hex >> 16) & 0xFF) / 255,
            green: CGFloat((hex >> 8) & 0xFF) / 255,
            blue: CGFloat(hex & 0xFF) / 255,
            alpha: 1
        )
    }
}
