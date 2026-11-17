import SwiftUI
import UIKit

enum OffhandColor {
    static let ground = dynamic(light: 0xF7F8FA, dark: 0x0B0E12)
    static let surface = dynamic(light: 0xFFFFFF, dark: 0x1A1F26)
    static let ink = dynamic(light: 0x0E1116, dark: 0xF2F4F7)
    static let ink2 = dynamic(light: 0x4A515C, dark: 0xA9B0BB)
    static let ink3 = dynamic(light: 0x5F6672, dark: 0x8A919C)
    static let hairline = dynamic(light: 0xE3E6EB, dark: 0x232830)
    static let border = dynamic(light: 0xCDD2DA, dark: 0x2A303A)
    static let wave = dynamic(light: 0xB9BFC9, dark: 0x3A414C)
    static let well = dynamic(light: 0xEEF0F3, dark: 0x14181D)
    static let wellSelected = dynamic(light: 0xFFFFFF, dark: 0x2A303A)
    static let accent = dynamic(light: 0x0B57D0, dark: 0xA8C7FA)
    static let onAccent = dynamic(light: 0xFFFFFF, dark: 0x062E6F)
    static let accentSoft = dynamic(light: 0xD3E3FD, dark: 0x1E3A66)
    static let danger = dynamic(light: 0xC22B20, dark: 0xFF8A80)
    static let onInk = dynamic(light: 0xFFFFFF, dark: 0x0B0E12)
    static let record = Color(hex: 0xFF5A4D)
    static let gold = Color(hex: 0xE9B44C)
    static let onGold = Color(hex: 0x1A1200)
    static let scrim = dynamic(light: 0x0E1116, dark: 0x05070A, lightAlpha: 0.48, darkAlpha: 0.72)

    private static func dynamic(light: UInt32, dark: UInt32, lightAlpha: CGFloat = 1, darkAlpha: CGFloat = 1) -> Color {
        Color(uiColor: UIColor { traits in
            traits.userInterfaceStyle == .dark
                ? UIColor(hex: dark, alpha: darkAlpha)
                : UIColor(hex: light, alpha: lightAlpha)
        })
    }
}

extension Color {
    init(hex: UInt32) {
        self.init(uiColor: UIColor(hex: hex, alpha: 1))
    }
}

extension UIColor {
    convenience init(hex: UInt32, alpha: CGFloat) {
        self.init(
            red: CGFloat((hex >> 16) & 0xFF) / 255,
            green: CGFloat((hex >> 8) & 0xFF) / 255,
            blue: CGFloat(hex & 0xFF) / 255,
            alpha: alpha
        )
    }
}

enum OffhandRadius {
    static let control: CGFloat = 12
    static let field: CGFloat = 14
    static let card: CGFloat = 18
    static let sheet: CGFloat = 28
}

enum OffhandSpacing {
    static let inRow: CGFloat = 4
    static let related: CGFloat = 8
    static let controls: CGFloat = 12
    static let card: CGFloat = 16
    static let screen: CGFloat = 24
    static let section: CGFloat = 24
    static let touchTarget: CGFloat = 44
    static let control: CGFloat = 56
    static let bottomBar: CGFloat = 104
}

enum OffhandMotion {
    static let state = Animation.easeOut(duration: 0.2)
    static let sheet = Animation.easeOut(duration: 0.3)
    static let skeletonSweep: Double = 1.4
}
