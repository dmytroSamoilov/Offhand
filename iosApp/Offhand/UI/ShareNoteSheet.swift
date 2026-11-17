import OffhandShared
import SwiftUI

struct ShareNoteSheet: View {
    let viewModel: NotesViewModel
    let hasAudio: Bool
    let isDocumentExportUnlocked: Bool

    @State private var tab: ShareTab = .text
    @State private var format: NoteExportFormat = .text

    private enum ShareTab: Int {
        case text
        case audio
    }

    private static let formats: [NoteExportFormat] = [.text, .pdf, .docx]

    var body: some View {
        OffhandSheet(title: String(localized: "Share note")) {
            if hasAudio {
                SegmentedControl(
                    options: [String(localized: "Text file"), String(localized: "Audio file")],
                    selectedIndex: tab.rawValue,
                    onSelect: { tab = ShareTab(rawValue: $0) ?? .text }
                )
                .padding(.bottom, OffhandSpacing.controls)
            }
            if tab == .text {
                formatRows
            } else {
                audioExplanation
            }
            Text(String(localized: "The file is made on your iPhone and goes only where you send it."))
                .offhandText(.caption)
                .foregroundStyle(OffhandColor.ink3)
                .padding(.top, 14)
            HStack(spacing: OffhandSpacing.controls) {
                PrimaryButton(String(localized: "Share"), systemImage: "square.and.arrow.up", fillsWidth: true) {
                    confirm(viewModel.onShareConfirmed(noteFormat:includeAudio:))
                }
                SecondaryButton(String(localized: "Download"), systemImage: "arrow.down.to.line", fillsWidth: true) {
                    confirm(viewModel.onSaveToDeviceConfirmed(noteFormat:includeAudio:))
                }
            }
            .padding(.top, 18)
        }
    }

    private var formatRows: some View {
        VStack(spacing: 0) {
            ForEach(Self.formats, id: \.self) { option in
                ChoiceRow(
                    title: title(for: option),
                    hint: fileExtension(for: option),
                    isSelected: format == option,
                    showsProBadge: isLocked(option),
                    showsDivider: option != Self.formats.last
                ) {
                    format = option
                }
            }
        }
    }

    private var audioExplanation: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack(spacing: OffhandSpacing.controls) {
                Text(String(localized: "WAV file"))
                    .offhandText(.body, face: .interSemibold)
                    .foregroundStyle(OffhandColor.ink)
                    .frame(maxWidth: .infinity, alignment: .leading)
                Text(verbatim: ".wav")
                    .font(.offhandMonoSmall)
                    .foregroundStyle(OffhandColor.ink3)
            }
            Text(String(localized: "The recording is shared as an uncompressed WAV file, 16 kHz mono. Every player and transcription tool opens it."))
                .offhandText(.bodySmall)
                .foregroundStyle(OffhandColor.ink2)
        }
        .padding(.vertical, OffhandSpacing.controls)
    }

    private func confirm(_ action: (NoteExportFormat?, Bool) -> Void) {
        Haptics.confirm()
        let isText = tab == .text
        action(isText ? format : nil, !isText)
    }

    private func isLocked(_ option: NoteExportFormat) -> Bool {
        option != .text && !isDocumentExportUnlocked
    }

    private func title(for option: NoteExportFormat) -> String {
        switch option {
        case .pdf: return String(localized: "PDF")
        case .docx: return String(localized: "Word")
        default: return String(localized: "Plain text")
        }
    }

    private func fileExtension(for option: NoteExportFormat) -> String {
        switch option {
        case .pdf: return ".pdf"
        case .docx: return ".docx"
        default: return ".txt"
        }
    }
}
