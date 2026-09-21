import SwiftUI

// The gold crown that marks a Pro-only action for a free user; the same art
// ships as ic_pro_crown on Android. Gold is a brand mark, not a tinted glyph.
struct ProCrown: View {
    var size: CGFloat = 18

    var body: some View {
        Image("ProCrown")
            .renderingMode(.original)
            .resizable()
            .scaledToFit()
            .frame(width: size, height: size)
            .accessibilityLabel(String(localized: "Offhand Pro"))
    }
}

// The gating marker: a "PRO" pill placed right after a feature's title. The
// crown stays for the tier itself (paywall, Settings card).
struct ProBadge: View {
    var body: some View {
        Text(verbatim: "PRO")
            .font(.caption2.weight(.bold))
            .kerning(0.6)
            .foregroundStyle(Brand.proGold)
            .padding(.horizontal, 6)
            .padding(.vertical, 2)
            .background(Brand.proGold.opacity(0.18), in: RoundedRectangle(cornerRadius: 6))
            .accessibilityLabel(String(localized: "Offhand Pro"))
    }
}
