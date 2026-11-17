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
            .font(.offhand(.interBold, size: 10, relativeTo: .caption2))
            .tracking(0.8)
            .foregroundStyle(OffhandColor.onGold)
            .padding(.horizontal, 7)
            .padding(.vertical, 2)
            .background(OffhandColor.gold, in: RoundedRectangle(cornerRadius: 6, style: .continuous))
            .accessibilityLabel(String(localized: "Offhand Pro"))
    }
}
