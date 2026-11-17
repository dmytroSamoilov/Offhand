import SwiftUI

enum BottomBarDestination {
    case notes, settings
}

struct BottomBarLabels {
    let notes: String
    let settings: String
    let record: String
    let recordAccessibilityLabel: String
}

struct BottomBar: View {
    let selected: BottomBarDestination
    let labels: BottomBarLabels
    let onNotes: () -> Void
    let onRecord: () -> Void
    let onSettings: () -> Void

    private static let barHeight: CGFloat = 84

    var body: some View {
        VStack(spacing: 0) {
            Hairline()
            HStack {
                BottomBarItem(
                    label: labels.notes,
                    systemImage: "text.alignleft",
                    identifier: "tab_notes",
                    isSelected: selected == .notes,
                    action: onNotes
                )
                Spacer(minLength: OffhandSpacing.controls)
                RecordPill(labels.record, accessibilityLabel: labels.recordAccessibilityLabel, action: onRecord)
                Spacer(minLength: OffhandSpacing.controls)
                BottomBarItem(
                    label: labels.settings,
                    systemImage: "slider.horizontal.3",
                    identifier: "tab_settings",
                    isSelected: selected == .settings,
                    action: onSettings
                )
            }
            .padding(.horizontal, 28)
            .frame(height: Self.barHeight)
        }
        .background(OffhandColor.ground.ignoresSafeArea(edges: .bottom))
    }
}

private struct BottomBarItem: View {
    let label: String
    let systemImage: String
    let identifier: String
    let isSelected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            VStack(spacing: 4) {
                Image(systemName: systemImage)
                    .font(.system(size: 22, weight: .regular))
                    .frame(height: 22)
                    .accessibilityLabel(label)
                Text(label)
                    .font(.offhand(isSelected ? .interSemibold : .interMedium, size: 11, relativeTo: .caption2))
                    .lineLimit(1)
            }
            .foregroundStyle(isSelected ? OffhandColor.ink : OffhandColor.ink2)
            .frame(width: 64, height: 56)
            .contentShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
        }
        .buttonStyle(.offhandPress)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(label)
        .accessibilityAddTraits(.isButton)
        .accessibilityIdentifier(identifier)
        .accessibilityAddTraits(isSelected ? .isSelected : [])
    }
}
