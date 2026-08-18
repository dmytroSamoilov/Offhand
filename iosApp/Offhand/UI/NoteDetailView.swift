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
                if detail.status == .processing {
                    processingCard
                } else {
                    if detail.hasAudio {
                        playbackCard
                    }
                    if detail.status == .failed {
                        failedCard
                    } else {
                        contentCard(title: String(localized: "Overview"), text: detail.body, tint: Brand.primaryContainer)
                    }
                    if !detail.transcript.isEmpty {
                        contentCard(title: String(localized: "Transcript"), text: detail.transcript, tint: Brand.tealContainer)
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
                        viewModel.onDeleteRequested()
                    } label: {
                        Label(String(localized: "Delete"), systemImage: "trash")
                    }
                } label: {
                    Image(systemName: "ellipsis.circle")
                }
            }
        }
        .confirmationDialog(
            String(localized: "Delete this note?"),
            isPresented: deleteBinding,
            titleVisibility: .visible
        ) {
            Button(String(localized: "Delete"), role: .destructive) { viewModel.onDeleteConfirmed() }
            Button(String(localized: "Cancel"), role: .cancel) { viewModel.onDeleteDismissed() }
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
            ActivityShareSheet(items: shareItems)
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
            Text(detail.createdAt)
                .font(.subheadline)
                .foregroundStyle(.secondary)
        }
    }

    private var processingCard: some View {
        HStack(spacing: 12) {
            ProgressView()
            Text(String(localized: "Preparing your note"))
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding()
        .background(Color(.secondarySystemGroupedBackground), in: RoundedRectangle(cornerRadius: 12))
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
        HStack(spacing: 12) {
            Button {
                viewModel.onPlayPauseClicked()
            } label: {
                Image(systemName: state.playback.isPlaying ? "pause.fill" : "play.fill")
                    .frame(width: 44, height: 44)
                    .background(Brand.primaryContainer, in: Circle())
            }
            .buttonStyle(.plain)
            VStack(alignment: .leading, spacing: 6) {
                ProgressView(value: state.playback.progress)
                    .tint(Brand.primary)
                Text("\(state.playback.positionText) / \(state.playback.durationText)")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
        }
        .padding()
        .background(Color(.secondarySystemGroupedBackground), in: RoundedRectangle(cornerRadius: 12))
    }

    private func contentCard(title: String, text: String, tint: Color) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(title)
                .font(.caption.weight(.semibold))
                .padding(.horizontal, 10)
                .padding(.vertical, 5)
                .background(tint, in: Capsule())
            MarkdownBlocks(raw: text)
                .textSelection(.enabled)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding()
        .background(Color(.secondarySystemGroupedBackground), in: RoundedRectangle(cornerRadius: 12))
    }

    private var deleteBinding: Binding<Bool> {
        Binding(
            get: { state.isDeleteConfirmationVisible },
            set: { isShown in if !isShown { viewModel.onDeleteDismissed() } }
        )
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
            set: { isShown in if !isShown { shareItems = [] } }
        )
    }
}

private struct MarkdownBlocks: View {
    let raw: String

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            ForEach(Array(raw.split(separator: "\n", omittingEmptySubsequences: false).enumerated()), id: \.offset) { _, line in
                blockView(String(line))
            }
        }
    }

    @ViewBuilder
    private func blockView(_ line: String) -> some View {
        let trimmed = line.trimmingCharacters(in: .whitespaces)
        if trimmed.isEmpty {
            EmptyView()
        } else if trimmed.hasPrefix("#") {
            Text(trimmed.drop(while: { $0 == "#" }).trimmingCharacters(in: .whitespaces))
                .font(.headline)
                .padding(.top, 4)
        } else {
            Text(inlineMarkdown(trimmed))
                .font(.body)
        }
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
                        Button {
                            viewModel.onPresetSelected(preset: option.preset)
                        } label: {
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
                                        .foregroundStyle(Brand.primary)
                                }
                            }
                        }
                        .buttonStyle(.plain)
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

    func makeUIViewController(context: Context) -> UIActivityViewController {
        UIActivityViewController(activityItems: items, applicationActivities: nil)
    }

    func updateUIViewController(_ uiViewController: UIActivityViewController, context: Context) {}
}
