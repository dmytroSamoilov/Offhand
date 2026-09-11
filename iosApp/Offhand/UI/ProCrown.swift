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
