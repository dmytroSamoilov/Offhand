import SwiftUI

struct SettingsGroup<Content: View>: View {
    var label: String? = nil
    @ViewBuilder let content: () -> Content

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            if let label {
                SectionLabel(label)
                    .padding(.leading, 8)
                    .padding(.top, 28)
                    .padding(.bottom, 10)
            }
            SurfaceCard(content: content)
        }
    }
}

struct SurfaceCard<Content: View>: View {
    @ViewBuilder let content: () -> Content

    var body: some View {
        let shape = RoundedRectangle(cornerRadius: OffhandRadius.card, style: .continuous)
        VStack(alignment: .leading, spacing: 0) {
            content()
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(OffhandColor.surface, in: shape)
        .clipShape(shape)
        .overlay { shape.strokeBorder(OffhandColor.hairline, lineWidth: 1) }
    }
}

struct LinkRow<Trailing: View>: View {
    let title: String
    var subtitle: String? = nil
    var value: String? = nil
    var showsProBadge = false
    var showsDivider = true
    let action: () -> Void
    @ViewBuilder let trailing: () -> Trailing

    var body: some View {
        Button(action: action) {
            SettingsRowLayout(
                title: title,
                subtitle: subtitle,
                showsProBadge: showsProBadge,
                showsDivider: showsDivider
            ) {
                if let value {
                    Text(value)
                        .offhandText(.bodySmall)
                        .foregroundStyle(OffhandColor.ink2)
                }
                trailing()
            }
        }
        .buttonStyle(.offhandRow)
    }
}

extension LinkRow where Trailing == RowChevron {
    init(
        title: String,
        subtitle: String? = nil,
        value: String? = nil,
        showsProBadge: Bool = false,
        showsDivider: Bool = true,
        action: @escaping () -> Void
    ) {
        self.init(
            title: title,
            subtitle: subtitle,
            value: value,
            showsProBadge: showsProBadge,
            showsDivider: showsDivider,
            action: action,
            trailing: { RowChevron() }
        )
    }
}

struct SwitchRow: View {
    let title: String
    var subtitle: String? = nil
    let isOn: Bool
    var showsProBadge = false
    var showsDivider = true
    let onChange: (Bool) -> Void

    var body: some View {
        Button(action: toggle) {
            SettingsRowLayout(
                title: title,
                subtitle: subtitle,
                showsProBadge: showsProBadge,
                showsDivider: showsDivider
            ) {
                OffhandSwitch(isOn: isOn)
            }
        }
        .buttonStyle(.offhandRow)
        .accessibilityRepresentation {
            Toggle(title, isOn: Binding(get: { isOn }, set: { _ in toggle() }))
        }
        .accessibilityHint(subtitle ?? "")
    }

    private func toggle() {
        Haptics.selection()
        onChange(!isOn)
    }
}

struct SettingsRowLayout<Trailing: View>: View {
    let title: String
    let subtitle: String?
    let showsProBadge: Bool
    let showsDivider: Bool
    @ViewBuilder let trailing: () -> Trailing

    var body: some View {
        VStack(spacing: 0) {
            HStack(spacing: OffhandSpacing.controls) {
                VStack(alignment: .leading, spacing: 1) {
                    HStack(spacing: 8) {
                        Text(title)
                            .offhandText(.settingTitle)
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
                trailing()
            }
            .padding(.horizontal, OffhandSpacing.card)
            .frame(minHeight: 62)
            if showsDivider { Hairline() }
        }
        .multilineTextAlignment(.leading)
    }
}

struct RowChevron: View {
    var body: some View {
        Image(systemName: "chevron.right")
            .font(.system(size: 15, weight: .regular))
            .foregroundStyle(OffhandColor.ink3)
            .frame(width: 16, height: 16)
            .accessibilityHidden(true)
    }
}

struct OffhandSwitch: View {
    let isOn: Bool
    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        Capsule()
            .fill(isOn ? OffhandColor.ink : OffhandColor.border)
            .frame(width: 48, height: 28)
            .overlay(alignment: .leading) {
                Circle()
                    .fill(knobColor)
                    .frame(width: 22, height: 22)
                    .shadow(color: OffhandColor.ink.opacity(0.25), radius: 1, x: 0, y: 1)
                    .offset(x: isOn ? 23 : 3)
            }
            .animation(OffhandMotion.state, value: isOn)
            .accessibilityHidden(true)
    }

    private var knobColor: Color {
        switch (colorScheme, isOn) {
        case (.dark, true): OffhandColor.onInk
        case (.dark, false): OffhandColor.ink2
        default: OffhandColor.surface
        }
    }
}
