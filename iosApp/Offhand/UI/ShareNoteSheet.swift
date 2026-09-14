import OffhandShared
import SwiftUI

// Two tabs, because a note is shared either as a document or as its recording:
// the text tab lists the file formats, the audio tab explains what the
// recording file is.
struct ShareNoteSheet: View {
    let viewModel: NotesViewModel
    let hasAudio: Bool
    let isDocumentExportUnlocked: Bool

    @State private var tab: ShareTab = .text
    @State private var format: NoteExportFormat = .text

    private enum ShareTab: Hashable {
        case text
        case audio
    }

    private static let formats: [NoteExportFormat] = [.text, .pdf, .docx]

    var body: some View {
        NavigationStack {
            List {
                if hasAudio {
                    tabPicker
                }
                if tab == .text {
                    formatSection
                } else {
                    audioSection
                }
            }
            .listSectionSpacing(12)
            .navigationTitle(String(localized: "Share note"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(String(localized: "Cancel")) { viewModel.onShareDismissed() }
                }
            }
            .safeAreaInset(edge: .bottom) { shareButton }
        }
    }

    private var tabPicker: some View {
        Picker(String(localized: "Share note"), selection: $tab) {
            Text(String(localized: "Text file")).tag(ShareTab.text)
            Text(String(localized: "Audio file")).tag(ShareTab.audio)
        }
        .pickerStyle(.segmented)
        .labelsHidden()
        .listRowInsets(EdgeInsets(top: 8, leading: 0, bottom: 8, trailing: 0))
        .listRowBackground(Color.clear)
    }

    private var formatSection: some View {
        Section {
            ForEach(Self.formats, id: \.self) { option in
                formatRow(option)
            }
        } footer: {
            Text(footerText)
        }
    }

    private var audioSection: some View {
        Section {
            optionLabel(
                title: String(localized: "WAV file"),
                fileExtension: ".wav",
                isDimmed: false,
                isPro: false
            )
        } footer: {
            Text(String(localized: "The recording is shared as an uncompressed WAV file, 16 kHz mono. Every player and transcription tool opens it."))
        }
    }

    private var footerText: String {
        String(localized: "Please keep in mind that shared copies are no longer encrypted once they leave your iPhone.")
    }

    private func formatRow(_ option: NoteExportFormat) -> some View {
        let isLocked = self.isLocked(option)
        return Button {
            format = option
        } label: {
            HStack {
                optionLabel(
                    title: title(for: option),
                    fileExtension: fileExtension(for: option),
                    isDimmed: false,
                    isPro: isLocked
                )
                Spacer()
                if format == option {
                    Image(systemName: "checkmark").foregroundStyle(Brand.primary)
                }
            }
        }
        .buttonStyle(.plain)
    }

    private func optionLabel(title: String, fileExtension: String, isDimmed: Bool, isPro: Bool) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            HStack(spacing: 6) {
                Text(title).foregroundStyle(isDimmed ? AnyShapeStyle(.secondary) : AnyShapeStyle(.primary))
                if isPro { ProBadge() }
            }
            Text(fileExtension).font(.footnote).foregroundStyle(.secondary)
        }
    }

    private var shareButton: some View {
        Button {
            Haptics.confirm()
            let isText = tab == .text
            viewModel.onShareConfirmed(noteFormat: isText ? format : nil, includeAudio: !isText)
        } label: {
            Text(String(localized: "Share")).frame(maxWidth: .infinity)
        }
        .buttonStyle(.borderedProminent)
        .padding()
        .background(.bar)
    }

    private func isLocked(_ option: NoteExportFormat) -> Bool {
        option != .text && !isDocumentExportUnlocked
    }

    private func title(for option: NoteExportFormat) -> String {
        switch option {
        case .pdf: return String(localized: "PDF file")
        case .docx: return String(localized: "Word file")
        default: return String(localized: "Text file")
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
