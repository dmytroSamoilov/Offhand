import SwiftUI

struct EmptyState: View {
    let title: String
    var message: String? = nil
    var linkLabel: String? = nil
    var onLink: () -> Void = {}

    private static let waveSeed: Int64 = 7

    var body: some View {
        VStack(spacing: 12) {
            WaveGlyph(
                seed: Self.waveSeed,
                size: CGSize(width: 120, height: 56),
                barCount: 10,
                lineWidth: 4
            )
            .padding(.bottom, 12)
            Text(title)
                .offhandText(.emptyTitle)
                .foregroundStyle(OffhandColor.ink)
                .accessibilityAddTraits(.isHeader)
            if let message {
                Text(message)
                    .offhandText(.body)
                    .foregroundStyle(OffhandColor.ink2)
            }
            if let linkLabel {
                PlainTextButton(linkLabel, action: onLink)
                    .padding(.top, 8)
            }
        }
        .multilineTextAlignment(.center)
        .padding(.horizontal, 40)
    }
}
