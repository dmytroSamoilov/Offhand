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

    static func symbol(for preset: NotePreset) -> String {
        switch preset {
        case .summary: return "doc.plaintext"
        case .meeting: return "person.3"
        case .visit: return "list.clipboard"
        case .legal: return "building.columns"
        default: return "doc.plaintext"
        }
    }

    static let customSymbol = "square.and.pencil"

    static func label(for style: NoteStyleRef, customStyles: [CustomStyleOptionUi]) -> String {
        if let preset = style.builtInPreset { return label(for: preset) }
        return customStyles.first { $0.id == style.customId }?.name ?? label(for: .summary)
    }
}

struct StyleOptionRow: View {
    let title: String
    let details: String
    let symbol: String
    let isSelected: Bool
    var showProBadge = false
    let action: () -> Void

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: symbol)
                .foregroundStyle(Brand.primary)
                .frame(width: 28)
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
            if isSelected {
                Image(systemName: "checkmark")
                    .fontWeight(.semibold)
                    .foregroundStyle(Brand.primary)
            }
        }
        .contentShape(Rectangle())
        .onTapGesture(perform: action)
    }
}
