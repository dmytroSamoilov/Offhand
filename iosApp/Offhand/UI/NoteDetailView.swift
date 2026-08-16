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
                    contentCard(title: String(localized: "Overview"), text: detail.body, tint: Brand.primaryContainer)
                    contentCard(title: String(localized: "Transcript"), text: detail.transcript, tint: Brand.tealContainer)
                }
            }
            .padding()
        }
        .background(Brand.surface)
        .navigationTitle("")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItemGroup(placement: .primaryAction) {
                Button { viewModel.onEditStarted() } label: { Image(systemName: "pencil") }
                Button { viewModel.onShareRequested() } label: { Image(systemName: "square.and.arrow.up") }
                Button(role: .destructive) {
                    viewModel.onDeleteRequested()
                } label: {
                    Image(systemName: "trash")
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
                .foregroundStyle(Brand.onSurfaceVariant)
        }
    }

    private var processingCard: some View {
        HStack(spacing: 12) {
            ProgressView()
            Text(String(localized: "Preparing your note"))
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding()
        .background(Brand.surfaceContainer, in: RoundedRectangle(cornerRadius: 20))
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
                    .foregroundStyle(Brand.onSurfaceVariant)
            }
        }
        .padding()
        .background(Brand.surfaceContainer, in: RoundedRectangle(cornerRadius: 20))
    }

    private func contentCard(title: String, text: String, tint: Color) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(title)
                .font(.caption.weight(.semibold))
                .padding(.horizontal, 10)
                .padding(.vertical, 5)
                .background(tint, in: Capsule())
            Text(markdownText(text))
                .font(.body)
                .textSelection(.enabled)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding()
        .background(Brand.surfaceContainer, in: RoundedRectangle(cornerRadius: 20))
    }

    private func markdownText(_ raw: String) -> AttributedString {
        let options = AttributedString.MarkdownParsingOptions(interpretedSyntax: .inlineOnlyPreservingWhitespace)
        return (try? AttributedString(markdown: raw, options: options)) ?? AttributedString(raw)
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
