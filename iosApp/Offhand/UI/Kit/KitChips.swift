import SwiftUI

struct Chip<Trailing: View>: View {
    let text: String
    var systemImage: String? = nil
    let action: () -> Void
    @ViewBuilder let trailing: () -> Trailing

    var body: some View {
        Button(action: action) {
            HStack(spacing: 8) {
                if let systemImage {
                    Image(systemName: systemImage)
                        .font(.system(size: 16, weight: .regular))
                        .accessibilityHidden(true)
                }
                Text(text)
                    .offhandText(.bodySmall, face: .interMedium)
                    .lineLimit(1)
                trailing()
            }
            .foregroundStyle(OffhandColor.ink)
            .padding(.horizontal, 18)
            .frame(minHeight: OffhandSpacing.touchTarget)
            .background(OffhandColor.surface, in: Capsule())
            .overlay { Capsule().strokeBorder(OffhandColor.border, lineWidth: 1) }
            .contentShape(Capsule())
        }
        .buttonStyle(.offhandPress)
    }
}

extension Chip where Trailing == EmptyView {
    init(text: String, systemImage: String? = nil, action: @escaping () -> Void) {
        self.init(text: text, systemImage: systemImage, action: action, trailing: { EmptyView() })
    }
}

struct TimeMark: View {
    let text: String
    var isActive = false

    var body: some View {
        Text(text)
            .font(.offhandMonoSmall)
            .foregroundStyle(OffhandColor.ink)
            .padding(.horizontal, 9)
            .padding(.vertical, 4)
            .frame(minHeight: 26)
            .background(isActive ? OffhandColor.surface : OffhandColor.well, in: Capsule())
            .animation(OffhandMotion.state, value: isActive)
    }
}
