import SwiftUI

struct TabItem: Hashable {
    let label: String
    var dotDescription: String? = nil
}

struct UnderlineTabs<Trailing: View>: View {
    let tabs: [TabItem]
    let selectedIndex: Int
    var spacing: CGFloat = 24
    var contentPadding: CGFloat = 0
    let onSelect: (Int) -> Void
    @ViewBuilder let trailing: () -> Trailing

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: spacing) {
                ForEach(Array(tabs.enumerated()), id: \.offset) { index, tab in
                    UnderlineTab(tab: tab, isSelected: index == selectedIndex) { onSelect(index) }
                }
                trailing()
            }
            .padding(.horizontal, contentPadding)
        }
        .scrollBounceBehavior(.basedOnSize, axes: .horizontal)
        .background(alignment: .bottom) { Hairline() }
    }
}

extension UnderlineTabs where Trailing == EmptyView {
    init(
        tabs: [TabItem],
        selectedIndex: Int,
        spacing: CGFloat = 24,
        contentPadding: CGFloat = 0,
        onSelect: @escaping (Int) -> Void
    ) {
        self.init(
            tabs: tabs,
            selectedIndex: selectedIndex,
            spacing: spacing,
            contentPadding: contentPadding,
            onSelect: onSelect,
            trailing: { EmptyView() }
        )
    }
}

struct UnderlineTab: View {
    let tab: TabItem
    let isSelected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 6) {
                Text(tab.label)
                    .offhandText(isSelected ? .tabSelected : .tab)
                    .foregroundStyle(isSelected ? OffhandColor.ink : OffhandColor.ink2)
                    .lineLimit(1)
                if tab.dotDescription != nil {
                    Color.clear.frame(width: 6, height: 6)
                }
            }
            .frame(minHeight: OffhandSpacing.touchTarget)
            .overlay(alignment: .bottom) {
                if isSelected { Rectangle().fill(OffhandColor.accent).frame(height: 2) }
            }
            .contentShape(Rectangle())
        }
        .buttonStyle(.offhandPress)
        .accessibilityAddTraits(isSelected ? .isSelected : [])
        .overlay(alignment: .trailing) { dot }
        .animation(OffhandMotion.state, value: isSelected)
    }

    @ViewBuilder private var dot: some View {
        if let description = tab.dotDescription {
            Circle()
                .fill(OffhandColor.accent)
                .frame(width: 6, height: 6)
                .allowsHitTesting(false)
                .accessibilityElement()
                .accessibilityLabel(description)
        }
    }
}

struct SegmentedControl: View {
    let options: [String]
    let selectedIndex: Int
    var isCompact = false
    let onSelect: (Int) -> Void

    var body: some View {
        HStack(spacing: 4) {
            ForEach(options.indices, id: \.self) { index in
                segment(index)
            }
        }
        .padding(4)
        .background(OffhandColor.well, in: RoundedRectangle(cornerRadius: isCompact ? 14 : 16, style: .continuous))
        .animation(OffhandMotion.state, value: selectedIndex)
    }

    private func segment(_ index: Int) -> some View {
        let isSelected = index == selectedIndex
        let shape = RoundedRectangle(cornerRadius: isCompact ? 10 : 12, style: .continuous)
        return Button { onSelect(index) } label: {
            Text(options[index])
                .offhandText(isCompact ? .bodySmall : .tab, face: isSelected ? .interSemibold : .interMedium)
                .foregroundStyle(isSelected ? OffhandColor.ink : OffhandColor.ink2)
                .lineLimit(1)
                .frame(maxWidth: .infinity, minHeight: isCompact ? 40 : OffhandSpacing.touchTarget)
                .background {
                    if isSelected {
                        shape.fill(OffhandColor.wellSelected)
                            .shadow(color: OffhandColor.ink.opacity(0.1), radius: 1, x: 0, y: 1)
                    }
                }
                .contentShape(shape)
        }
        .buttonStyle(.offhandPress)
        .accessibilityAddTraits(isSelected ? .isSelected : [])
    }
}

struct SectionLabel: View {
    private let text: String
    private let isEmphasised: Bool

    init(_ text: String, isEmphasised: Bool = false) {
        self.text = text
        self.isEmphasised = isEmphasised
    }

    var body: some View {
        Text(text)
            .offhandText(.label)
            .foregroundStyle(isEmphasised ? OffhandColor.ink : OffhandColor.ink3)
            .accessibilityAddTraits(.isHeader)
    }
}

struct MetaText: View {
    private let text: String

    init(_ text: String) {
        self.text = text
    }

    var body: some View {
        Text(text)
            .offhandText(.meta)
            .foregroundStyle(OffhandColor.ink3)
            .lineLimit(1)
    }
}

struct Hairline: View {
    var body: some View {
        Rectangle()
            .fill(OffhandColor.hairline)
            .frame(height: 1)
            .frame(maxWidth: .infinity)
            .accessibilityHidden(true)
    }
}

struct ProgressLine: View {
    let progress: Double?
    var thickness: CGFloat = 2

    private static let indeterminateFraction = 0.08

    var body: some View {
        Capsule()
            .fill(OffhandColor.hairline)
            .frame(height: thickness)
            .overlay(alignment: .leading) {
                GeometryReader { geometry in
                    Rectangle()
                        .fill(OffhandColor.accent)
                        .frame(width: geometry.size.width * fraction)
                }
            }
            .clipShape(Capsule())
            .animation(OffhandMotion.state, value: fraction)
            .accessibilityHidden(true)
    }

    private var fraction: CGFloat {
        CGFloat(min(max(progress ?? Self.indeterminateFraction, 0), 1))
    }
}

extension Font {
    static let offhandMonoSmall = Font.offhand(.monoRegular, size: 12, relativeTo: .caption)
}
