import SwiftUI

struct Chip<Trailing: View>: View {
    let text: String
    var systemImage: String? = nil
    var isSelected = false
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
                    .offhandText(.bodySmall, face: isSelected ? .interSemibold : .interMedium)
                    .lineLimit(1)
                trailing()
            }
            .foregroundStyle(isSelected ? OffhandColor.onInk : OffhandColor.ink)
            .padding(.horizontal, 18)
            .frame(minHeight: OffhandSpacing.touchTarget)
            .background(isSelected ? OffhandColor.ink : OffhandColor.surface, in: Capsule())
            .overlay { Capsule().strokeBorder(isSelected ? .clear : OffhandColor.border, lineWidth: 1) }
            .contentShape(Capsule())
        }
        .buttonStyle(.offhandPress)
        .accessibilityAddTraits(isSelected ? .isSelected : [])
    }
}

extension Chip where Trailing == EmptyView {
    init(text: String, systemImage: String? = nil, isSelected: Bool = false, action: @escaping () -> Void) {
        self.init(text: text, systemImage: systemImage, isSelected: isSelected, action: action, trailing: { EmptyView() })
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
