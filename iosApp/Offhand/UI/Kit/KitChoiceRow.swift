import SwiftUI

struct ChoiceRow<Trailing: View>: View {
    let title: String
    var subtitle: String? = nil
    var hint: String? = nil
    let isSelected: Bool
    var showsProBadge = false
    var showsDivider = true
    var minHeight: CGFloat = 60
    let action: () -> Void
    @ViewBuilder let trailing: () -> Trailing

    var body: some View {
        VStack(spacing: 0) {
            Button(action: action) { rowContent }
                .buttonStyle(.offhandRow)
                .accessibilityAddTraits(isSelected ? .isSelected : [])
            if showsDivider { Hairline() }
        }
    }

    private var rowContent: some View {
        HStack(spacing: 14) {
            ChoiceMark(isSelected: isSelected)
            VStack(alignment: .leading, spacing: 1) {
                HStack(spacing: 10) {
                    Text(title)
                        .offhandText(.body, face: isSelected ? .interSemibold : .interMedium)
                        .foregroundStyle(OffhandColor.ink)
                    if showsProBadge { ProBadge() }
                }
                if let subtitle {
                    Text(subtitle)
                        .offhandText(.caption)
                        .foregroundStyle(OffhandColor.ink2)
                }
            }
            .padding(.vertical, 10)
            .frame(maxWidth: .infinity, alignment: .leading)
            if let hint {
                Text(hint)
                    .font(.offhandMonoSmall)
                    .foregroundStyle(OffhandColor.ink3)
            }
            trailing()
        }
        .frame(minHeight: minHeight)
        .multilineTextAlignment(.leading)
    }
}

extension ChoiceRow where Trailing == EmptyView {
    init(
        title: String,
        subtitle: String? = nil,
        hint: String? = nil,
        isSelected: Bool,
        showsProBadge: Bool = false,
        showsDivider: Bool = true,
        minHeight: CGFloat = 60,
        action: @escaping () -> Void
    ) {
        self.init(
            title: title,
            subtitle: subtitle,
            hint: hint,
            isSelected: isSelected,
            showsProBadge: showsProBadge,
            showsDivider: showsDivider,
            minHeight: minHeight,
            action: action,
            trailing: { EmptyView() }
        )
    }
}

struct ChoiceMark: View {
    let isSelected: Bool
    var size: CGFloat = 22
    var selectedColor: Color = OffhandColor.ink

    private static let unselectedRingAlpha = 0.6

    var body: some View {
        ZStack {
            if isSelected {
                Circle().fill(selectedColor)
                Image(systemName: "checkmark")
                    .font(.system(size: size * 0.5, weight: .bold))
                    .foregroundStyle(OffhandColor.onInk)
            } else {
                Circle().strokeBorder(OffhandColor.ink3.opacity(Self.unselectedRingAlpha), lineWidth: 1.5)
            }
        }
        .frame(width: size, height: size)
        .animation(OffhandMotion.state, value: isSelected)
        .accessibilityHidden(true)
    }
}
