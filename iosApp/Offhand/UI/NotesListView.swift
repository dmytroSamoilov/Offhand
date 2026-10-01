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
        moveToFolder: nil,
        folderStylePicker: nil,
        isFolderStylesUnlocked: false,
        customStyles: [],
        importMessage: nil,
        smartSuggestions: nil,
        isDocumentExportUnlocked: false,
        isCustomStylesUnlocked: false,
        pendingCalendarEvent: nil,
        isRetranscribeAvailable: false
    )
    @State private var recordSheetRequest: RecordSheetRequest?
    @State private var recordedNoteId: Int64?
    @State private var searchQuery = ""
    @State private var newStyleFolderId: Int64?
    @State private var folderStyleEditorFolderId: Int64?
    @Environment(\.horizontalSizeClass) private var horizontalSizeClass

    var body: some View {
        layout
        .sheet(item: $recordSheetRequest, onDismiss: openRecordedNote) { request in
            RecordSheetView(autoStart: true, folderId: request.folderId) { recordedNoteId = $0 }
        }
        // Like the note's restyle sheet, the folder picker closes before the
        // editor is pushed and comes back for the same folder on return.
        .sheet(isPresented: folderStyleBinding, onDismiss: openRequestedFolderStyleEditor) {
            if let picker = state.folderStylePicker {
                FolderStyleSheet(
                    viewModel: viewModel,
                    picker: picker,
                    state: state,
                    onCreateStyle: {
                        newStyleFolderId = picker.folderId
                        viewModel.onFolderStyleDismissed()
                    }
                )
                .presentationDetents([.medium, .large])
            }
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
        .alert(String(localized: "Delete this note?"), isPresented: deleteBinding) {
            Button(String(localized: "Delete"), role: .destructive) { viewModel.onDeleteConfirmed() }
            Button(String(localized: "Cancel"), role: .cancel) { viewModel.onDeleteDismissed() }
        } message: {
            Text(String(localized: "The note will be permanently removed from your iPhone. This cannot be undone."))
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
                NavigationStack {
                    notesList
                }
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
            if !state.noteProgress.isEmpty {
                Section {
                    KeepOpenBanner()
                        .listRowInsets(EdgeInsets())
                        .listRowBackground(Color.clear)
                }
            }
            Section {
                FolderChips(
                    folders: state.folders,
                    selectedFolderId: state.selectedFolderId?.int64Value,
                    isFolderStylesUnlocked: state.isFolderStylesUnlocked,
                    onSelect: { viewModel.onFolderSelected(folderId: $0.map { KotlinLong(value: $0) }) },
                    onNew: { viewModel.onNewFolderRequested() },
                    onRename: { viewModel.onRenameFolderRequested(folderId: $0) },
                    onDelete: { viewModel.onDeleteFolderRequested(folderId: $0) },
                    onStyle: { viewModel.onFolderStyleRequested(folderId: $0) },
                    onMove: { viewModel.onFolderMoved(fromIndex: Int32($0), toIndex: Int32($1)) }
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
                        .alignmentGuide(.listRowSeparatorLeading) { $0[.leading] }
                        .swipeActions(edge: .leading, allowsFullSwipe: true) {
                            Button {
                                Haptics.confirm()
                                viewModel.onMoveToFolderRequested(noteId: note.id)
                            } label: {
                                Label(String(localized: "Move to folder"), systemImage: "folder")
                            }
                            .tint(Brand.primary)
                        }
                        .swipeActions(edge: .trailing, allowsFullSwipe: false) {
                            Button(role: .destructive) {
                                Haptics.confirm()
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
        .navigationDestination(isPresented: folderStyleEditorBinding) {
            NoteStyleEditorView(styleId: 0)
        }
        .onChange(of: folderStyleEditorFolderId) { previous, current in
            if let previous, current == nil { viewModel.onFolderStyleRequested(folderId: previous) }
        }
        .navigationTitle(String(localized: "Notes"))
        .overlay(alignment: .center) {
            if state.sections.isEmpty {
                if !searchQuery.isEmpty {
                    ContentUnavailableView.search(text: searchQuery)
                } else if state.selectedFolderId != nil {
                    ContentUnavailableView(
                        String(localized: "No notes in this folder."),
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

    private func openRequestedFolderStyleEditor() {
        guard let folderId = newStyleFolderId else { return }
        newStyleFolderId = nil
        folderStyleEditorFolderId = folderId
    }

    private var folderStyleEditorBinding: Binding<Bool> {
        Binding(
            get: { folderStyleEditorFolderId != nil },
            set: { isShown in if !isShown { folderStyleEditorFolderId = nil } }
        )
    }

    private var folderStyleBinding: Binding<Bool> {
        Binding(
            get: { state.folderStylePicker != nil },
            set: { isShown in if !isShown { viewModel.onFolderStyleDismissed() } }
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

    private func openRecordedNote() {
        guard let noteId = recordedNoteId else { return }
        recordedNoteId = nil
        viewModel.onNoteSelected(id: noteId)
    }

    private var recordButton: some View {
        Button {
            Haptics.confirm()
            recordSheetRequest = RecordSheetRequest(folderId: state.selectedFolderId?.int64Value)
        } label: {
            Image(systemName: "mic.fill")
                .font(.title2)
                .foregroundStyle(Brand.onPrimary)
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

// Chips can be picked up and dropped onto another chip to change the order;
// a long press without moving opens the same menu as a tap on the selected chip.
private struct FolderChips: View {
    let folders: [FolderUi]
    let selectedFolderId: Int64?
    let isFolderStylesUnlocked: Bool
    let onSelect: (Int64?) -> Void
    let onNew: () -> Void
    let onRename: (Int64) -> Void
    let onDelete: (Int64) -> Void
    let onStyle: (Int64) -> Void
    let onMove: (Int, Int) -> Void

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                FolderChip(title: String(localized: "All notes"), isSelected: selectedFolderId == nil) {
                    onSelect(nil)
                }
                ForEach(Array(folders.enumerated()), id: \.element.id) { index, folder in
                    folderChip(folder)
                    .contextMenu { folderMenuItems(folder) }
                    .draggable(String(folder.id))
                    .dropDestination(for: String.self) { dropped, _ in
                        guard let id = dropped.first.flatMap(Int64.init),
                              let from = folders.firstIndex(where: { $0.id == id }),
                              from != index else { return false }
                        onMove(from, index)
                        return true
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
            .padding(.vertical, 4)
        }
    }

    // Like Android, the selected chip carries the three dots and a tap on it
    // opens the folder menu; a tap on any other chip selects it.
    @ViewBuilder
    private func folderChip(_ folder: FolderUi) -> some View {
        if folder.id == selectedFolderId {
            Menu {
                folderMenuItems(folder)
            } label: {
                FolderChipLabel(title: folder.name, isSelected: true, showsOptions: true)
            }
            .buttonStyle(.plain)
        } else {
            FolderChip(title: folder.name, isSelected: false) { onSelect(folder.id) }
        }
    }

    @ViewBuilder
    private func folderMenuItems(_ folder: FolderUi) -> some View {
        Button {
            onStyle(folder.id)
        } label: {
            Label(styleMenuTitle, systemImage: "gearshape")
        }
        Button {
            onRename(folder.id)
        } label: {
            Label(String(localized: "Rename folder"), systemImage: "pencil")
        }
        Divider()
        Button(role: .destructive) {
            onDelete(folder.id)
        } label: {
            Label(String(localized: "Delete folder"), systemImage: "trash")
        }
        .tint(.red)
    }

    private var styleMenuTitle: String {
        isFolderStylesUnlocked
            ? String(localized: "Summary style")
            : String(localized: "Summary style (Pro)")
    }
}

// Picks the style new recordings in a folder get and notes moved into it
// are rewritten in; "Default" follows Settings.
private struct FolderStyleSheet: View {
    let viewModel: NotesViewModel
    let picker: FolderStylePickerUi
    let state: NotesUiState
    let onCreateStyle: () -> Void

    var body: some View {
        NavigationStack {
            NoteStyleList(
                current: picker.style,
                customStyles: state.customStyles.map { NoteStyleChoice(id: $0.id, name: $0.name, details: $0.description_) },
                isCustomStylesUnlocked: state.isFolderStylesUnlocked,
                footer: String(localized: "New recordings in this folder use it, and notes moved here are rewritten in it."),
                onSelect: { viewModel.onFolderStyleSelected(style: $0) },
                onCreateStyle: onCreateStyle,
                onDefaultSelected: { viewModel.onFolderStyleSelected(style: nil) }
            )
            .navigationTitle(String(format: String(localized: "Summary style for %@"), picker.folderName))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(String(localized: "Cancel")) { viewModel.onFolderStyleDismissed() }
                }
            }
        }
    }
}

private struct FolderChip: View {
    let title: String
    let isSelected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            FolderChipLabel(title: title, isSelected: isSelected, showsOptions: false)
        }
        .buttonStyle(.plain)
    }
}

private struct FolderChipLabel: View {
    let title: String
    let isSelected: Bool
    let showsOptions: Bool

    var body: some View {
        HStack(spacing: 6) {
            Text(title)
                .font(.subheadline.weight(.medium))
                .lineLimit(1)
            if showsOptions {
                Image(systemName: "ellipsis")
                    .font(.subheadline.weight(.semibold))
                    .rotationEffect(.degrees(90))
                    .accessibilityLabel(String(localized: "Folder options"))
            }
        }
        .padding(.horizontal, 14)
        .frame(height: 32)
        .background(isSelected ? Brand.primaryContainer : Color(.secondarySystemGroupedBackground), in: Capsule())
        .foregroundStyle(isSelected ? Brand.onPrimaryContainer : Color.primary)
    }
}

private struct ModelPreparationBanner: View {
    let percent: Int

    var body: some View {
        HStack(spacing: 12) {
            ProgressView()
            VStack(alignment: .leading, spacing: 2) {
                Text(String(localized: "Setting up your on-device AI"))
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
            } else if note.status == .interrupted {
                Label(
                    String(localized: "Paused. Open the note to continue."),
                    systemImage: "pause.circle"
                )
                .font(.subheadline)
                .foregroundStyle(.secondary)
            } else if note.status == .failed {
                Label(
                    String(localized: "We were unable to create a summary and transcript for this note."),
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

private struct RecordSheetRequest: Identifiable {
    let id = UUID()
    let folderId: Int64?
}

// iOS stops the on-device AI as soon as the app leaves the foreground, so a
// note in progress is only finished while Offhand stays in front.
struct KeepOpenBanner: View {
    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            ProgressView()
            VStack(alignment: .leading, spacing: 2) {
                Text(String(localized: "Preparing your note"))
                    .font(.subheadline.weight(.semibold))
                Text(String(localized: "Keep Offhand open until your note is ready."))
                    .font(.footnote)
                    .foregroundStyle(.secondary)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(12)
        .background(Brand.primaryContainer, in: RoundedRectangle(cornerRadius: Brand.cardRadius))
        .foregroundStyle(Brand.onPrimaryContainer)
        .accessibilityElement(children: .combine)
    }
}

struct KeepOpenHint: View {
    var body: some View {
        Label(
            String(localized: "Keep Offhand open until your note is ready."),
            systemImage: "iphone"
        )
        .font(.footnote)
        .foregroundStyle(.secondary)
    }
}
