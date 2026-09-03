import OffhandShared
import SwiftUI

struct NoteDetailView: View {
    let viewModel: NotesViewModel
    let detail: NoteDetailUi
    let state: NotesUiState
    @State private var shareItems: [URL] = []

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                header
                trustBadges
                if detail.status == .processing {
                    processingCard
                } else {
                    if detail.hasAudio && state.playback.isAvailable {
                        playbackCard
                    }
                    if detail.status == .failed {
                        failedCard
                    } else {
                        CollapsibleSection(
                            title: String(localized: "Overview"),
                            text: detail.body,
                            initiallyExpanded: true
                        )
                        .id(detail.id)
                    }
                    if !detail.transcript.isEmpty {
                        CollapsibleSection(
                            title: String(localized: "Transcript"),
                            text: detail.transcript,
                            initiallyExpanded: detail.status != .ready
                        )
                        .id(detail.id)
                    }
                }
            }
            .padding()
        }
        .background(Color(.systemGroupedBackground))
        .navigationTitle("")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItemGroup(placement: .primaryAction) {
                Button { viewModel.onEditStarted() } label: { Image(systemName: "pencil") }
                Button { viewModel.onShareRequested() } label: { Image(systemName: "square.and.arrow.up") }
                Menu {
                    if !detail.transcript.isEmpty {
                        Button {
                            viewModel.onPresetSheetRequested()
                        } label: {
                            Label(String(localized: "Change note style"), systemImage: "slider.horizontal.3")
                        }
                    }
                    if detail.hasAudio {
                        Button {
                            viewModel.onRetranscribeRequested()
                        } label: {
                            Label(String(localized: "Re-transcribe"), systemImage: "arrow.clockwise")
                        }
                    }
                    Button(role: .destructive) {
                        viewModel.onDeleteRequested(id: detail.id)
                    } label: {
                        Label(String(localized: "Delete"), systemImage: "trash")
                    }
                } label: {
                    Image(systemName: "ellipsis.circle")
                }
            }
        }
        .confirmationDialog(
            String(localized: "Re-transcribe this note?"),
            isPresented: retranscribeBinding,
            titleVisibility: .visible
        ) {
            Button(String(localized: "Re-transcribe")) { viewModel.onRetranscribeConfirmed() }
            Button(String(localized: "Cancel"), role: .cancel) { viewModel.onRetranscribeDismissed() }
        } message: {
            Text(String(localized: "The recording will be transcribed and summarized again, replacing the current title, overview and transcript. The audio recording itself is kept."))
        }
        .confirmationDialog(
            String(localized: "Share note"),
            isPresented: shareBinding,
            titleVisibility: .visible
        ) {
            Button(String(localized: "Note text")) { viewModel.onShareConfirmed(includeNote: true, includeAudio: false) }
            if detail.hasAudio {
                Button(String(localized: "Audio")) { viewModel.onShareConfirmed(includeNote: false, includeAudio: true) }
                Button(String(localized: "Note and audio")) { viewModel.onShareConfirmed(includeNote: true, includeAudio: true) }
            }
            Button(String(localized: "Cancel"), role: .cancel) { viewModel.onShareDismissed() }
        }
        .sheet(isPresented: editorBinding) {
            if let editor = state.editor {
                NoteEditorView(viewModel: viewModel, editor: editor)
            }
        }
        .sheet(isPresented: presetBinding) {
            NoteStyleSheet(viewModel: viewModel, current: detail.preset)
                .presentationDetents([.medium, .large])
        }
        .sheet(isPresented: shareItemsBinding) {
            ActivityShareSheet(items: shareItems, onComplete: dismissShare)
        }
        .onChange(of: state.pendingShare) {
            if let pending = state.pendingShare {
                shareItems = pending.filePaths.map { URL(fileURLWithPath: $0) }
                viewModel.onShareLaunched()
            }
        }
    }

    private var header: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(detail.title)
                .font(.title2.weight(.semibold))
            Text(metadataLine)
                .font(.subheadline)
                .foregroundStyle(.secondary)
            if let metrics = detail.metrics, state.isDeveloperMode {
                Text(
                    String(
                        format: String(localized: "Transcribed in %@ · Structured in %@ · %@"),
                        metrics.transcriptionTime,
                        metrics.structuringTime,
                        metrics.hardwareBackend
                    )
                )
                .font(.caption)
                .foregroundStyle(.secondary)
            }
        }
    }

    private var trustBadges: some View {
        Label(String(localized: "Encrypted"), systemImage: "lock.fill")
            .font(.caption.weight(.medium))
            .padding(.horizontal, 10)
            .padding(.vertical, 4)
            .background(Brand.primaryContainer, in: Capsule())
            .foregroundStyle(Brand.onPrimaryContainer)
    }

    private var metadataLine: String {
        guard detail.wordCount > 0 else { return detail.createdAt }
        let words = String.localizedStringWithFormat(
            String(localized: "%d words"),
            Int(detail.wordCount)
        )
        return "\(detail.createdAt) · \(words)"
    }

    private var processingCard: some View {
        HStack(spacing: 12) {
            ProgressView()
            VStack(alignment: .leading, spacing: 2) {
                Text(processingStage)
                if let percent = state.noteProgress[KotlinLong(value: detail.id)]?.intValue {
                    Text("\(percent)%")
                        .font(.caption)
                        .monospacedDigit()
                        .foregroundStyle(.secondary)
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding()
        .background(Color(.secondarySystemGroupedBackground), in: RoundedRectangle(cornerRadius: 12))
    }

    private var processingStage: String {
        detail.transcript.isEmpty
            ? String(localized: "Transcribing your recording")
            : String(localized: "Writing your overview")
    }

    private var failedCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            Label(
                detail.transcript.isEmpty
                    ? String(localized: "We were unable to create an overview and transcript for this note.")
                    : String(localized: "We were unable to create an overview for this note. The transcript is saved below."),
                systemImage: "exclamationmark.triangle"
            )
            .foregroundStyle(.secondary)
            if detail.hasAudio {
                Button(String(localized: "Re-transcribe")) {
                    viewModel.onRetranscribeRequested()
                }
                .buttonStyle(.bordered)
                .tint(Brand.primary)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding()
        .background(Color(.secondarySystemGroupedBackground), in: RoundedRectangle(cornerRadius: 12))
    }

    private var playbackCard: some View {
        HStack(spacing: 14) {
            Button {
                viewModel.onPlayPauseClicked()
            } label: {
                Image(systemName: state.playback.isPlaying ? "pause.circle.fill" : "play.circle.fill")
                    .font(.system(size: 40))
                    .foregroundStyle(Brand.primary)
            }
            .buttonStyle(.plain)
            VStack(spacing: 4) {
                Slider(
                    value: Binding(
                        get: { Double(state.playback.progress) },
                        set: { viewModel.onSeekRequested(fraction: Float($0)) }
                    ),
                    in: 0...1
                )
                .tint(Brand.primary)
                HStack {
                    Text(state.playback.positionText)
                    Spacer()
                    Text(state.playback.durationText)
                }
                .font(.caption2)
                .monospacedDigit()
                .foregroundStyle(.secondary)
            }
        }
        .padding()
        .background(Color(.secondarySystemGroupedBackground), in: RoundedRectangle(cornerRadius: 12))
    }

    private var shareBinding: Binding<Bool> {
        Binding(
            get: { state.isShareDialogVisible },
            set: { isShown in if !isShown { viewModel.onShareDismissed() } }
        )
    }

    private var retranscribeBinding: Binding<Bool> {
        Binding(
            get: { state.isRetranscribeConfirmationVisible },
            set: { isShown in if !isShown { viewModel.onRetranscribeDismissed() } }
        )
    }

    private var presetBinding: Binding<Bool> {
        Binding(
            get: { state.isPresetSheetVisible },
            set: { isShown in if !isShown { viewModel.onPresetSheetDismissed() } }
        )
    }

    private var editorBinding: Binding<Bool> {
        Binding(
            get: { state.editor != nil },
            set: { isShown in if !isShown { viewModel.onEditCancelled() } }
        )
    }

    private var shareItemsBinding: Binding<Bool> {
        Binding(
            get: { !shareItems.isEmpty },
            set: { isShown in if !isShown { dismissShare() } }
        )
    }

    private func dismissShare() {
        guard !shareItems.isEmpty else { return }
        shareItems = []
        viewModel.onShareCompleted()
    }
}

private struct CollapsibleSection: View {
    let title: String
    let text: String
    @State private var isExpanded: Bool

    init(title: String, text: String, initiallyExpanded: Bool) {
        self.title = title
        self.text = text
        _isExpanded = State(initialValue: initiallyExpanded)
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 7) {
            Button {
                withAnimation(.easeInOut(duration: 0.2)) { isExpanded.toggle() }
            } label: {
                HStack {
                    Text(title)
                        .font(.footnote.weight(.semibold))
                        .foregroundStyle(.secondary)
                        .textCase(.uppercase)
                    Spacer()
                    Image(systemName: "chevron.down")
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(.secondary)
                        .rotationEffect(.degrees(isExpanded ? 0 : -90))
                }
                .padding(.leading, 16)
                .padding(.trailing, 4)
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .accessibilityLabel(title)
            if isExpanded {
                MarkdownBlocks(raw: text)
                    .textSelection(.enabled)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding()
                    .background(
                        Color(.secondarySystemGroupedBackground),
                        in: RoundedRectangle(cornerRadius: 12)
                    )
            }
        }
    }
}

private struct MarkdownBlocks: View {
    let raw: String

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            ForEach(Array(lines.enumerated()), id: \.offset) { _, line in
                blockView(line)
            }
        }
    }

    private var lines: [String] {
        raw.split(separator: "\n", omittingEmptySubsequences: false)
            .map { $0.trimmingCharacters(in: .whitespaces) }
            .filter { !$0.isEmpty }
    }

    @ViewBuilder
    private func blockView(_ line: String) -> some View {
        if let heading = heading(line) {
            Text(inlineMarkdown(heading.text))
                .font(heading.font)
                .padding(.top, 4)
        } else if let item = listItem(line) {
            HStack(alignment: .firstTextBaseline, spacing: 8) {
                Text(item.marker)
                    .font(.body)
                    .foregroundStyle(.secondary)
                Text(inlineMarkdown(item.text))
                    .font(.body)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
        } else {
            Text(inlineMarkdown(line))
                .font(.body)
        }
    }

    private func heading(_ line: String) -> (text: String, font: Font)? {
        if line.hasPrefix("### ") { return (String(line.dropFirst(4)), .headline) }
        if line.hasPrefix("## ") { return (String(line.dropFirst(3)), .title3.weight(.semibold)) }
        if line.hasPrefix("# ") { return (String(line.dropFirst(2)), .title2.weight(.semibold)) }
        return nil
    }

    private func listItem(_ line: String) -> (marker: String, text: String)? {
        if line.hasPrefix("- ") || line.hasPrefix("* ") {
            return ("•", String(line.dropFirst(2)))
        }
        return numberedItem(line)
    }

    // "1. Something" keeps its own number, the way the Android renderer does.
    private func numberedItem(_ line: String) -> (marker: String, text: String)? {
        guard let dot = line.firstIndex(of: ".") else { return nil }
        let digits = line[line.startIndex..<dot]
        guard !digits.isEmpty, digits.allSatisfy(\.isNumber) else { return nil }
        let afterDot = line.index(after: dot)
        guard afterDot < line.endIndex, line[afterDot] == " " else { return nil }
        return ("\(digits).", String(line[line.index(after: afterDot)...]))
    }

    private func inlineMarkdown(_ raw: String) -> AttributedString {
        let options = AttributedString.MarkdownParsingOptions(interpretedSyntax: .inlineOnlyPreservingWhitespace)
        return (try? AttributedString(markdown: raw, options: options)) ?? AttributedString(raw)
    }
}

private struct NoteStyleSheet: View {
    let viewModel: NotesViewModel
    let current: NotePreset

    private struct StyleOption {
        let preset: NotePreset
        let label: String
        let details: String
        let symbol: String
    }

    private var options: [StyleOption] {
        [
            StyleOption(
                preset: .summary,
                label: String(localized: "Summary"),
                details: String(localized: "A clean write-up of what was said, without repetition or filler."),
                symbol: "doc.plaintext"
            ),
            StyleOption(
                preset: .meeting,
                label: String(localized: "Meeting notes"),
                details: String(localized: "Discussion, decisions, action items and open questions."),
                symbol: "person.3"
            ),
            StyleOption(
                preset: .visit,
                label: String(localized: "Visit report"),
                details: String(localized: "Who the visit was about, observations, what was done and follow-ups."),
                symbol: "list.clipboard"
            ),
            StyleOption(
                preset: .legal,
                label: String(localized: "Legal note"),
                details: String(localized: "Matter, facts stated, instructions, advice given and next steps."),
                symbol: "building.columns"
            ),
        ]
    }

    var body: some View {
        NavigationStack {
            List {
                Section {
                    ForEach(options, id: \.symbol) { option in
                        HStack(spacing: 12) {
                            Image(systemName: option.symbol)
                                .foregroundStyle(Brand.primary)
                                .frame(width: 28)
                            VStack(alignment: .leading, spacing: 2) {
                                Text(option.label)
                                    .font(.body)
                                    .foregroundStyle(Color.primary)
                                Text(option.details)
                                    .font(.caption)
                                    .foregroundStyle(Color.secondary)
                            }
                            Spacer()
                            if option.preset == current {
                                Image(systemName: "checkmark")
                                    .fontWeight(.semibold)
                                    .foregroundStyle(Brand.primary)
                            }
                        }
                        .contentShape(Rectangle())
                        .onTapGesture {
                            viewModel.onPresetSelected(preset: option.preset)
                        }
                    }
                } footer: {
                    Text(String(localized: "The recording is kept. The title and overview are written again from the transcript in the style you pick."))
                }
            }
            .navigationTitle(String(localized: "Rewrite this note as"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(String(localized: "Cancel")) { viewModel.onPresetSheetDismissed() }
                }
            }
        }
    }
}

private struct NoteEditorView: View {
    let viewModel: NotesViewModel
    let editor: NoteEditorUi

    var body: some View {
        NavigationStack {
            Form {
                Section(String(localized: "Title")) {
                    TextField(
                        String(localized: "Title"),
                        text: Binding(
                            get: { editor.title },
                            set: { viewModel.onEditorTitleChanged(title: $0) }
                        )
                    )
                }
                Section(String(localized: "Transcript")) {
                    TextEditor(
                        text: Binding(
                            get: { editor.transcript },
                            set: { viewModel.onEditorTranscriptChanged(transcript: $0) }
                        )
                    )
                    .frame(minHeight: 220)
                }
            }
            .navigationTitle(String(localized: "Edit note"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(String(localized: "Cancel")) { viewModel.onEditCancelled() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button(String(localized: "Save")) { viewModel.onEditSaved() }
                }
            }
        }
    }
}

private struct ActivityShareSheet: UIViewControllerRepresentable {
    let items: [URL]
    let onComplete: () -> Void

    func makeUIViewController(context: Context) -> UIActivityViewController {
        let controller = UIActivityViewController(activityItems: items, applicationActivities: nil)
        // Fires once the chosen activity has consumed the files (or on cancel),
        // so the decrypted share copies are safe to delete.
        controller.completionWithItemsHandler = { _, _, _, _ in onComplete() }
        return controller
    }

    func updateUIViewController(_ uiViewController: UIActivityViewController, context: Context) {}
}
