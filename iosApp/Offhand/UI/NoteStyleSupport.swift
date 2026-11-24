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
        case .email: return String(localized: "Email")
        default: return String(localized: "Summary")
        }
    }

    static func details(for preset: NotePreset) -> String {
        switch preset {
        case .summary: return String(localized: "One to three sentences on what the recording is about.")
        case .meeting: return String(localized: "Discussion, decisions, action items and open questions.")
        case .visit: return String(localized: "Who the visit was about, observations, what was done and follow-ups.")
        case .legal: return String(localized: "Matter, facts stated, instructions, advice given and next steps.")
        case .email: return String(localized: "A subject line and an email that is ready to send.")
        default: return ""
        }
    }

    static func label(for style: NoteStyleRef, customStyles: [CustomStyleOptionUi]) -> String {
        if let preset = style.builtInPreset { return label(for: preset) }
        return customStyles.first { $0.id == style.customId }?.name ?? label(for: .summary)
    }
}

struct NoteStyleChoice: Identifiable {
    let id: Int64
    let name: String
    let details: String
}

struct NoteStyleList: View {
    let current: NoteStyleRef?
    let customStyles: [NoteStyleChoice]
    let isProStylesUnlocked: Bool
    var footer: String?
    let onSelect: (NoteStyleRef) -> Void
    var onCreateStyle: (() -> Void)?
    var onDefaultSelected: (() -> Void)?
    var onEdit: ((Int64) -> Void)?
    var title: String?
    var subtitle: String?

    private static let presets: [NotePreset] = [.summary, .meeting, .visit, .legal, .email]

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                heading
                if let onDefaultSelected {
                    ChoiceRow(
                        title: String(localized: "Default"),
                        subtitle: String(localized: "The style chosen in Settings"),
                        isSelected: current == nil,
                        showsDivider: false,
                        minHeight: 66,
                        action: onDefaultSelected
                    )
                }
                if !customStyles.isEmpty {
                    groupLabel(String(localized: "Your styles"))
                    ForEach(Array(customStyles.enumerated()), id: \.element.id) { index, style in
                        customRow(style, showsDivider: index < customStyles.count - 1)
                    }
                }
                groupLabel(String(localized: "Built in"))
                ForEach(Self.presets, id: \.self) { preset in
                    ChoiceRow(
                        title: NoteStyleLabels.label(for: preset),
                        subtitle: NoteStyleLabels.details(for: preset),
                        isSelected: current?.builtInPreset == preset,
                        showsProBadge: !isProStylesUnlocked && preset != .summary,
                        showsDivider: preset != Self.presets.last,
                        minHeight: 66
                    ) {
                        onSelect(NoteStyleRefBuiltIn(preset: preset))
                    }
                }
                if let footer {
                    Text(footer)
                        .offhandText(.caption)
                        .foregroundStyle(OffhandColor.ink2)
                        .padding(.top, OffhandSpacing.card)
                }
            }
            .padding(.horizontal, OffhandSpacing.screen)
            .padding(.bottom, OffhandSpacing.screen)
        }
        .background(OffhandColor.ground.ignoresSafeArea())
        .toolbar {
            if let onCreateStyle {
                ToolbarItem(placement: .primaryAction) {
                    Button(String(localized: "New"), action: onCreateStyle)
                        .font(.offhand(.buttonSmall))
                        .tint(OffhandColor.accent)
                }
            }
        }
    }

    @ViewBuilder
    private var heading: some View {
        if let title {
            Text(title)
                .offhandText(.title)
                .foregroundStyle(OffhandColor.ink)
                .padding(.top, 8)
                .accessibilityAddTraits(.isHeader)
        }
        if let subtitle {
            Text(subtitle)
                .offhandText(.dialogBody)
                .foregroundStyle(OffhandColor.ink2)
                .padding(.top, 8)
        }
    }

    private func groupLabel(_ text: String) -> some View {
        SectionLabel(text)
            .padding(.top, 22)
            .padding(.bottom, 2)
    }

    private func customRow(_ style: NoteStyleChoice, showsDivider: Bool) -> some View {
        VStack(spacing: 0) {
            HStack(spacing: 0) {
                ChoiceRow(
                    title: style.name,
                    subtitle: style.details,
                    isSelected: current?.customId == style.id,
                    showsProBadge: !isProStylesUnlocked,
                    showsDivider: false,
                    minHeight: 66
                ) {
                    onSelect(NoteStyleRefCustom(id: style.id))
                }
                if let onEdit {
                    PlainTextButton(String(localized: "Edit")) { onEdit(style.id) }
                }
            }
            if showsDivider { Hairline() }
        }
    }
}
