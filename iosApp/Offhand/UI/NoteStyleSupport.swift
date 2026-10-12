import OffhandShared
import SwiftUI

extension NoteStyleRef {
    var builtInPreset: NotePreset? { (self as? NoteStyleRefBuiltIn)?.preset }

    var customId: Int64? { (self as? NoteStyleRefCustom)?.id }
}

enum NoteStyleLabels {
    static func label(for preset: NotePreset) -> String {
        switch preset {
        case .summary: return String(localized: "Summary")
        case .meeting: return String(localized: "Meeting notes")
        case .visit: return String(localized: "Visit report")
        case .legal: return String(localized: "Legal note")
        default: return String(localized: "Summary")
        }
    }

    static func details(for preset: NotePreset) -> String {
        switch preset {
        case .summary: return String(localized: "Main topics, key decisions, action items and a short overview.")
        case .meeting: return String(localized: "Discussion, decisions, action items and open questions.")
        case .visit: return String(localized: "Who the visit was about, observations, what was done and follow-ups.")
        case .legal: return String(localized: "Matter, facts stated, instructions, advice given and next steps.")
        default: return ""
        }
    }

    static func label(for style: NoteStyleRef, customStyles: [CustomStyleOptionUi]) -> String {
        if let preset = style.builtInPreset { return label(for: preset) }
        return customStyles.first { $0.id == style.customId }?.name ?? label(for: .summary)
    }
}

struct StyleOptionRow: View {
    let title: String
    let details: String
    let isSelected: Bool
    var showProBadge = false
    let action: () -> Void

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: isSelected ? "checkmark.circle.fill" : "circle")
                .font(.title3)
                .foregroundStyle(isSelected ? Brand.primary : Color.secondary)
            VStack(alignment: .leading, spacing: 2) {
                HStack(spacing: 6) {
                    Text(title)
                        .font(.body)
                        .foregroundStyle(Color.primary)
                    if showProBadge { ProBadge() }
                }
                Text(details)
                    .font(.caption)
                    .foregroundStyle(Color.secondary)
            }
            Spacer()
        }
        .contentShape(Rectangle())
        .onTapGesture(perform: action)
    }
}

struct NoteStyleChoice: Identifiable {
    let id: Int64
    let name: String
    let details: String
}

// One list for every place a style is picked: the user's own styles, then the
// built-in ones, with "New" as a toolbar item in the host's top-right corner.
// The Summary styles screen adds an Edit button to the custom rows; the
// restyle sheet shows the plain list.
// The selected row looks the same as on Android: tinted, with a filled check.
struct NoteStyleList: View {
    let current: NoteStyleRef?
    let customStyles: [NoteStyleChoice]
    let isProStylesUnlocked: Bool
    let footer: String
    let onSelect: (NoteStyleRef) -> Void
    var onCreateStyle: (() -> Void)?
    var onDefaultSelected: (() -> Void)?
    var onEdit: ((Int64) -> Void)?

    var body: some View {
        List {
            if let onDefaultSelected {
                Section {
                    StyleOptionRow(
                        title: String(localized: "Default"),
                        details: String(localized: "The style chosen in Settings"),
                        isSelected: current == nil,
                        action: onDefaultSelected
                    )
                    .listRowBackground(rowBackground(isSelected: current == nil))
                }
            }
            if !customStyles.isEmpty {
                Section(String(localized: "Your styles")) {
                    ForEach(customStyles) { style in
                        customRow(style)
                    }
                }
            }
            Section {
                ForEach([NotePreset.summary, .meeting, .visit, .legal], id: \.self) { preset in
                    StyleOptionRow(
                        title: NoteStyleLabels.label(for: preset),
                        details: NoteStyleLabels.details(for: preset),
                        isSelected: current?.builtInPreset == preset,
                        showProBadge: !isProStylesUnlocked && preset != .summary
                    ) {
                        onSelect(NoteStyleRefBuiltIn(preset: preset))
                    }
                    .listRowBackground(rowBackground(isSelected: current?.builtInPreset == preset))
                }
            } header: {
                Text(String(localized: "Built in"))
            } footer: {
                Text(footer)
            }
        }
        .toolbar {
            if let onCreateStyle {
                ToolbarItem(placement: .primaryAction) {
                    Button(String(localized: "New"), action: onCreateStyle)
                }
            }
        }
    }

    @ViewBuilder
    private func customRow(_ style: NoteStyleChoice) -> some View {
        let isSelected = current?.customId == style.id
        HStack(spacing: 12) {
            StyleOptionRow(
                title: style.name,
                details: style.details,
                isSelected: isSelected,
                showProBadge: !isProStylesUnlocked
            ) {
                onSelect(NoteStyleRefCustom(id: style.id))
            }
            if let onEdit {
                Button(String(localized: "Edit")) {
                    onEdit(style.id)
                }
                .buttonStyle(.borderless)
                .foregroundStyle(Brand.primary)
            }
        }
        .listRowBackground(rowBackground(isSelected: isSelected))
    }

    private func rowBackground(isSelected: Bool) -> Color {
        isSelected ? Brand.secondaryContainer : Color(.secondarySystemGroupedBackground)
    }
}
