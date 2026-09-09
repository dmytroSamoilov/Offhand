import OffhandShared
import StoreKit
import SwiftUI
import UIKit

struct NotesListView: View {
    private let viewModel = AppViewModels.notes
    @State private var state = NotesUiState(
        sections: [],
        selected: nil,
        editor: nil,
        playback: AudioPlaybackUi(isAvailable: false, isPlaying: false, progress: 0, positionText: "0:00", durationText: "0:00"),
        pendingDeleteNoteId: nil,
        isRetranscribeConfirmationVisible: false,
        isShareDialogVisible: false,
        isPresetSheetVisible: false,
        pendingShare: nil,
        isDeveloperMode: false,
        noteProgress: [:],
        modelPreparation: nil,
        searchQuery: "",
        folders: [],
        selectedFolderId: nil,
        folderEditor: nil,
        pendingDeleteFolderId: nil,
        moveToFolder: nil
    )
    @State private var isRecordSheetVisible = false
    @State private var searchQuery = ""
    @Environment(\.horizontalSizeClass) private var horizontalSizeClass

    var body: some View {
        layout
        .sheet(isPresented: $isRecordSheetVisible) {
            RecordSheetView(autoStart: true)
        }
        .sheet(isPresented: moveToFolderBinding) {
            MoveToFolderSheet(
                folders: state.folders,
                currentFolderId: state.moveToFolder?.currentFolderId?.int64Value,
                onMove: { viewModel.onMoveToFolder(folderId: $0.map { KotlinLong(value: $0) }) },
                onCancel: { viewModel.onMoveToFolderDismissed() }
            )
            .presentationDetents([.medium, .large])
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
            String(localized: "Delete this folder?"),
            isPresented: deleteFolderBinding,
            titleVisibility: .visible
        ) {
            Button(String(localized: "Delete"), role: .destructive) { viewModel.onDeleteFolderConfirmed() }
            Button(String(localized: "Cancel"), role: .cancel) { viewModel.onDeleteFolderDismissed() }
        } message: {
            Text(String(localized: "Its notes are kept and go back to All notes."))
        }
        .alert(
            state.folderEditor?.folderId == nil ? String(localized: "New folder") : String(localized: "Rename folder"),
            isPresented: folderEditorBinding
        ) {
            TextField(String(localized: "Folder name"), text: folderNameBinding)
                .textInputAutocapitalization(.sentences)
            Button(String(localized: "Save")) { viewModel.onFolderEditorConfirmed() }
            Button(String(localized: "Cancel"), role: .cancel) { viewModel.onFolderEditorDismissed() }
        } message: {
            if let error = state.folderEditor?.error {
                Text(folderErrorMessage(error))
            }
        }
        .task {
            for await newState in viewModel.uiState {
                state = newState
            }
        }
        .task {
            for await _ in viewModel.reviewRequests {
                requestAppStoreReview()
                viewModel.onReviewAttemptSucceeded()
            }
        }
    }

    // The app ships for iPad, so give a regular width the two-pane layout Android
    // gets from its list-detail scaffold instead of a phone-shaped push stack.
    @ViewBuilder
    private var layout: some View {
        if horizontalSizeClass == .regular {
            NavigationSplitView {
                notesList
            } detail: {
                NavigationStack {
                    if let detail = state.selected {
                        NoteDetailView(viewModel: viewModel, detail: detail, state: state)
                    } else {
                        ContentUnavailableView(
                            String(localized: "No note selected"),
                            systemImage: "doc.text",
                            description: Text(String(localized: "Pick a note from the list to read it."))
                        )
                    }
                }
            }
        } else {
            NavigationStack {
                notesList
                    .navigationDestination(isPresented: detailBinding) {
                        if let detail = state.selected {
                            NoteDetailView(viewModel: viewModel, detail: detail, state: state)
                        }
                    }
            }
        }
    }

    private var notesList: some View {
        List {
            Section {
                FolderChips(
                    folders: state.folders,
                    selectedFolderId: state.selectedFolderId?.int64Value,
                    onSelect: { viewModel.onFolderSelected(folderId: $0.map { KotlinLong(value: $0) }) },
                    onNew: { viewModel.onNewFolderRequested() },
                    onRename: { viewModel.onRenameFolderRequested(folderId: $0) },
                    onDelete: { viewModel.onDeleteFolderRequested(folderId: $0) }
                )
                .listRowInsets(EdgeInsets())
                .listRowBackground(Color.clear)
            }
            ForEach(state.sections, id: \.self) { section in
                Section(dayTitle(section.dayLabel)) {
                    ForEach(section.notes, id: \.id) { note in
                        Button {
                            viewModel.onNoteSelected(id: note.id)
                        } label: {
                            NoteCardRow(note: note, progress: state.noteProgress[KotlinLong(value: note.id)]?.intValue)
                        }
                        .buttonStyle(.plain)
                        .swipeActions(edge: .leading, allowsFullSwipe: true) {
                            Button {
                                viewModel.onMoveToFolderRequested(noteId: note.id)
                            } label: {
                                Label(String(localized: "Move to folder"), systemImage: "folder")
                            }
                            .tint(Brand.primary)
                        }
                        .swipeActions(edge: .trailing, allowsFullSwipe: false) {
                            Button(role: .destructive) {
                                viewModel.onDeleteRequested(id: note.id)
                            } label: {
                                Label(String(localized: "Delete"), systemImage: "trash")
                            }
                            // The tab bar's brand tint would otherwise repaint a
                            // destructive action in blue.
                            .tint(.red)
                        }
                    }
                }
            }
        }
        .listStyle(.insetGrouped)
        .searchable(text: $searchQuery, prompt: Text(String(localized: "Search notes")))
        .onChange(of: searchQuery) { viewModel.onSearchQueryChanged(query: searchQuery) }
        .safeAreaInset(edge: .top) {
            if let preparation = state.modelPreparation {
                ModelPreparationBanner(percent: Int(preparation.progressPercent))
            }
        }
        .navigationTitle(String(localized: "Notes"))
        .overlay(alignment: .center) {
            if state.sections.isEmpty {
                if !searchQuery.isEmpty {
                    ContentUnavailableView.search(text: searchQuery)
                } else if state.selectedFolderId != nil {
                    ContentUnavailableView(
                        String(localized: "No notes in this folder yet."),
                        systemImage: "folder"
                    )
                } else {
                    ContentUnavailableView(
                        String(localized: "No notes yet"),
                        systemImage: "mic",
                        description: Text(String(localized: "Tap the microphone to record your first note."))
                    )
                }
            }
        }
        .overlay(alignment: .bottomTrailing) {
            recordButton
        }
    }

    private func requestAppStoreReview() {
        guard let scene = UIApplication.shared.connectedScenes
            .first(where: { $0.activationState == .foregroundActive }) as? UIWindowScene else { return }
        AppStore.requestReview(in: scene)
    }

    private var detailBinding: Binding<Bool> {
        Binding(
            get: { state.selected != nil },
            set: { isShown in if !isShown { viewModel.onDetailClosed() } }
        )
    }

    private var deleteBinding: Binding<Bool> {
        Binding(
            get: { state.pendingDeleteNoteId != nil },
            set: { isShown in if !isShown { viewModel.onDeleteDismissed() } }
        )
    }

    private var moveToFolderBinding: Binding<Bool> {
        Binding(
            get: { state.moveToFolder != nil },
            set: { isShown in if !isShown { viewModel.onMoveToFolderDismissed() } }
        )
    }

    private var deleteFolderBinding: Binding<Bool> {
        Binding(
            get: { state.pendingDeleteFolderId != nil },
            set: { isShown in if !isShown { viewModel.onDeleteFolderDismissed() } }
        )
    }

    private var folderEditorBinding: Binding<Bool> {
        Binding(
            get: { state.folderEditor != nil },
            set: { isShown in if !isShown { viewModel.onFolderEditorDismissed() } }
        )
    }

    private var folderNameBinding: Binding<String> {
        Binding(
            get: { state.folderEditor?.name ?? "" },
            set: { viewModel.onFolderNameChanged(name: $0) }
        )
    }

    private func folderErrorMessage(_ error: FolderNameErrorUi) -> String {
        switch error {
        case .blank: return String(localized: "Enter a folder name.")
        case .tooLong:
            return String(format: String(localized: "Use at most %d characters."), Int(FolderNameValidator.shared.MAX_LENGTH))
        case .duplicate: return String(localized: "A folder with this name already exists.")
        default: return ""
        }
    }

    private var recordButton: some View {
        Button {
            isRecordSheetVisible = true
        } label: {
            Image(systemName: "mic.fill")
                .font(.title2)
                .foregroundStyle(.white)
                .frame(width: 60, height: 60)
                .background(Brand.primary, in: Circle())
                .shadow(color: .black.opacity(0.25), radius: 10, y: 4)
        }
        .accessibilityLabel(String(localized: "Record a note"))
        .padding(.trailing, 20)
        .padding(.bottom, 16)
    }

    private func dayTitle(_ label: NoteDayLabelUi) -> String {
        switch onEnum(of: label) {
        case .today: return String(localized: "Today")
        case .yesterday: return String(localized: "Yesterday")
        case .date(let date): return date.text
        }
    }
}

private struct FolderChips: View {
    let folders: [FolderUi]
    let selectedFolderId: Int64?
    let onSelect: (Int64?) -> Void
    let onNew: () -> Void
    let onRename: (Int64) -> Void
    let onDelete: (Int64) -> Void

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                FolderChip(title: String(localized: "All notes"), isSelected: selectedFolderId == nil) {
                    onSelect(nil)
                }
                ForEach(folders, id: \.id) { folder in
                    FolderChip(title: folder.name, isSelected: folder.id == selectedFolderId) {
                        onSelect(folder.id)
                    }
                    .contextMenu {
                        Button {
                            onRename(folder.id)
                        } label: {
                            Label(String(localized: "Rename folder"), systemImage: "pencil")
                        }
                        Button(role: .destructive) {
                            onDelete(folder.id)
                        } label: {
                            Label(String(localized: "Delete folder"), systemImage: "trash")
                        }
                    }
                }
                Button(action: onNew) {
                    Label(String(localized: "New folder"), systemImage: "plus")
                        .font(.subheadline.weight(.medium))
                        .padding(.horizontal, 14)
                        .frame(height: 32)
                        .background(Color(.secondarySystemGroupedBackground), in: Capsule())
                        .foregroundStyle(Brand.primary)
                }
                .buttonStyle(.plain)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 4)
        }
    }
}

private struct FolderChip: View {
    let title: String
    let isSelected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(title)
                .font(.subheadline.weight(.medium))
                .lineLimit(1)
                .padding(.horizontal, 14)
                .frame(height: 32)
                .background(isSelected ? Brand.primaryContainer : Color(.secondarySystemGroupedBackground), in: Capsule())
                .foregroundStyle(isSelected ? Brand.onPrimaryContainer : Color.primary)
        }
        .buttonStyle(.plain)
    }
}

private struct ModelPreparationBanner: View {
    let percent: Int

    var body: some View {
        HStack(spacing: 12) {
            ProgressView()
            VStack(alignment: .leading, spacing: 2) {
                Text(String(localized: "Preparing on-device AI"))
                    .font(.subheadline.weight(.semibold))
                Text(String(localized: "New notes start processing once this finishes."))
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            Spacer()
            Text("\(percent)%")
                .font(.subheadline.weight(.medium))
                .monospacedDigit()
                .foregroundStyle(.secondary)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .background(.bar)
    }
}

private struct NoteCardRow: View {
    let note: NoteCardUi
    let progress: Int?

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(highlighted(note.title, note.titleHighlights))
                .font(.headline)
                .lineLimit(1)
            if note.status == .processing {
                HStack(spacing: 8) {
                    ProgressView()
                    Text(progressText)
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                }
            } else if note.status == .failed {
                Label(
                    String(localized: "We were unable to create an overview and transcript for this note."),
                    systemImage: "exclamationmark.triangle"
                )
                .font(.subheadline)
                .foregroundStyle(.secondary)
            } else if !note.preview.isEmpty {
                Text(highlighted(note.preview, note.previewHighlights))
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                    .lineLimit(2)
            }
            HStack(spacing: 12) {
                Text(note.time)
                if let duration = note.durationText {
                    Label(duration, systemImage: "waveform")
                }
                if let folder = note.folderName {
                    Label(folder, systemImage: "folder")
                        .lineLimit(1)
                }
            }
            .font(.caption)
            .foregroundStyle(.secondary)
        }
        .padding(.vertical, 2)
    }

    private var progressText: String {
        if let progress { return "\(progress)%" }
        return String(localized: "Preparing your note")
    }

    private func highlighted(_ text: String, _ ranges: [TextRangeUi]) -> AttributedString {
        var attributed = AttributedString(text)
        let utf16 = text.utf16
        for range in ranges {
            guard let lower = utf16.index(utf16.startIndex, offsetBy: Int(range.start), limitedBy: utf16.endIndex),
                  let upper = utf16.index(utf16.startIndex, offsetBy: Int(range.end), limitedBy: utf16.endIndex),
                  lower < upper,
                  let start = AttributedString.Index(lower, within: attributed),
                  let end = AttributedString.Index(upper, within: attributed) else { continue }
            attributed[start..<end].backgroundColor = Brand.searchHighlight
        }
        return attributed
    }
}
