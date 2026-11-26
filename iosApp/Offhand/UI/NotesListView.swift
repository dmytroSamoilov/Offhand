import OffhandShared
import StoreKit
import SwiftUI
import UIKit

struct NotesListView<BottomBarContent: View>: View {
    private let viewModel = AppViewModels.notes
    private let bottomBar: BottomBarContent
    @State private var state = NotesUiState(
        sections: [],
        selected: nil,
        editor: nil,
        playback: AudioPlaybackUi(isAvailable: false, isPlaying: false, progress: 0, positionText: "0:00", durationText: "0:00", positionMs: 0),
        pendingDeleteNoteId: nil,
        isRetranscribeConfirmationVisible: false,
        isShareDialogVisible: false,
        isPresetSheetVisible: false,
        pendingShare: nil,
        isDeveloperMode: false,
        noteProgress: [:],
        modelPreparation: nil,
        isWaitingForAi: false,
        isWaitingForSpeech: false,
        searchQuery: "",
        folders: [],
        selectedFolderId: nil,
        folderEditor: nil,
        pendingDeleteFolderId: nil,
        moveToFolder: nil,
        folderStylePicker: nil,
        isFolderStylesUnlocked: false,
        isFolderLimitReached: false,
        customStyles: [],
        importMessage: nil,
        smartSuggestions: nil,
        isDocumentExportUnlocked: false,
        isProStylesUnlocked: false,
        pendingCalendarEvent: nil,
        isRetranscribeAvailable: false,
        detailTab: .summary,
        detailWaveform: [],
        isSummaryReadyMarked: false,
        isNotesLoaded: false,
        isUnsortedShown: false,
        isTranscribing: false,
        selectedNoteIds: [],
        bulkAction: nil,
        isTranscriptionEnabled: true,
        isSummaryEnabled: true,
        isSummaryAvailable: true,
        transcriptFocusIndex: nil,
        usageSurvey: nil
    )
    @State private var longPressedNoteId: Int64?
    @State private var recordSheetRequest: RecordSheetRequest?
    @ObservedObject private var shortcuts = ShortcutRequests.shared
    @State private var recordedNoteId: Int64?
    @State private var searchQuery = ""
    @State private var isSearching = false
    @State private var newStyleFolderId: Int64?
    @State private var folderStyleEditorFolderId: Int64?
    @State private var isNewFolderRequestedAfterMove = false
    @Environment(\.horizontalSizeClass) private var horizontalSizeClass

    init(@ViewBuilder bottomBar: () -> BottomBarContent) {
        self.bottomBar = bottomBar()
    }

    var body: some View {
        layout
        .onChange(of: shortcuts.recordSheetRequestId) {
            guard recordSheetRequest == nil else { return }
            recordSheetRequest = RecordSheetRequest(
                folderId: shortcuts.takeRequestedFolder(fallback: state.selectedFolderId?.int64Value)
            )
        }
        .sheet(item: $recordSheetRequest, onDismiss: openRecordedNote) { request in
            RecordSheetView(autoStart: true, folderId: request.folderId) { recordedNoteId = $0 }
        }
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
        .sheet(isPresented: usageSurveySheetBinding) {
            UsageSurveySheet(
                selection: state.usageSurvey?.selection ?? [],
                onToggled: { viewModel.onUsageSurveyAnswerToggled(answer: $0) },
                onClosed: { viewModel.onUsageSurveySheetClosed() }
            )
        }
        .sheet(isPresented: moveToFolderBinding, onDismiss: requestNewFolderAfterMove) {
            MoveToFolderSheet(
                title: String(localized: "Move to folder"),
                folders: state.folders,
                initialChoice: FolderChoice(folderId: state.moveToFolder?.currentFolderId?.int64Value),
                isNewFolderGated: state.isFolderLimitReached,
                styleLabel: folderStyleLabel,
                rewriteNotice: { rewriteNotice(folderId: $0, noteCount: 1) },
                onMove: { viewModel.onMoveToFolder(folderId: $0.map { KotlinLong(value: $0) }) },
                onNewFolder: {
                    isNewFolderRequestedAfterMove = true
                    viewModel.onMoveToFolderDismissed()
                },
                onCancel: { viewModel.onMoveToFolderDismissed() }
            )
        }
        .sheet(isPresented: bulkBinding(.move), onDismiss: requestNewFolderAfterMove) {
            MoveToFolderSheet(
                title: String.localizedStringWithFormat(String(localized: "Move %lld notes"), state.selectedNoteIds.count),
                folders: state.folders,
                initialChoice: nil,
                isNewFolderGated: state.isFolderLimitReached,
                styleLabel: folderStyleLabel,
                rewriteNotice: { rewriteNotice(folderId: $0, noteCount: state.selectedNoteIds.count) },
                onMove: { viewModel.onBulkMoveConfirmed(folderId: $0.map { KotlinLong(value: $0) }) },
                onNewFolder: {
                    isNewFolderRequestedAfterMove = true
                    viewModel.onBulkActionDismissed()
                },
                onCancel: { viewModel.onBulkActionDismissed() }
            )
        }
        .offhandDialog(isPresented: bulkBinding(.delete)) {
            OffhandDialog(
                title: String(localized: "Delete the selected notes?"),
                message: String(localized: "They will be permanently removed from your iPhone. This cannot be undone."),
                confirmLabel: String(localized: "Cancel"),
                onConfirm: { viewModel.onBulkActionDismissed() },
                destructiveLabel: String(localized: "Delete"),
                onDestructive: { viewModel.onBulkDeleteConfirmed() },
                onDismiss: { viewModel.onBulkActionDismissed() }
            )
        }
        .offhandDialog(isPresented: deleteBinding) {
            OffhandDialog(
                title: String(localized: "Delete this note?"),
                message: String(localized: "The note will be permanently removed from your iPhone. This cannot be undone."),
                confirmLabel: String(localized: "Cancel"),
                onConfirm: { viewModel.onDeleteDismissed() },
                destructiveLabel: String(localized: "Delete"),
                onDestructive: { viewModel.onDeleteConfirmed() },
                onDismiss: { viewModel.onDeleteDismissed() }
            )
        }
        .offhandDialog(isPresented: deleteFolderBinding) {
            OffhandDialog(
                title: String(localized: "Delete this folder?"),
                message: String(localized: "Its notes are kept, just without a folder."),
                confirmLabel: String(localized: "Cancel"),
                onConfirm: { viewModel.onDeleteFolderDismissed() },
                destructiveLabel: String(localized: "Delete"),
                onDestructive: { viewModel.onDeleteFolderConfirmed() },
                onDismiss: { viewModel.onDeleteFolderDismissed() }
            )
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
        .tint(OffhandColor.ink)
        .task {
            for await newState in viewModel.uiState {
                state = newState
            }
        }
        .task {
            for await _ in viewModel.reviewRequests {
                if requestAppStoreReview() { viewModel.onReviewAttemptSucceeded() }
            }
        }
    }

    @ViewBuilder
    private var layout: some View {
        if isRegularWidth {
            NavigationSplitView {
                NavigationStack {
                    notesRoot
                }
                .navigationSplitViewColumnWidth(min: 360, ideal: 400, max: 520)
            } detail: {
                NavigationStack {
                    if let detail = state.selected {
                        NoteDetailView(viewModel: viewModel, detail: detail, state: state)
                    } else {
                        EmptyState(
                            title: String(localized: "No note selected"),
                            message: String(localized: "Pick a note from the list to read it.")
                        )
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                        .background(OffhandColor.ground)
                    }
                }
            }
        } else {
            NavigationStack {
                notesRoot
                    .navigationDestination(isPresented: detailBinding) {
                        if let detail = state.selected {
                            NoteDetailView(viewModel: viewModel, detail: detail, state: state)
                        }
                    }
            }
        }
    }

    private var isRegularWidth: Bool { horizontalSizeClass == .regular }

    private var notesRoot: some View {
        VStack(spacing: 0) {
            header
            if !state.isSelectingNotes && !isSearching {
                folderTabs.padding(.top, 12)
            }
            notesContent
        }
        .background(OffhandColor.ground.ignoresSafeArea())
        .toolbar(.hidden, for: .navigationBar)
        .safeAreaInset(edge: .bottom, spacing: 0) { footer }
        .onChange(of: searchQuery) { viewModel.onSearchQueryChanged(query: searchQuery) }
        .navigationDestination(isPresented: folderStyleEditorBinding) {
            NoteStyleEditorView(styleId: 0)
                .toolbar(.visible, for: .navigationBar)
        }
        .onChange(of: folderStyleEditorFolderId) { previous, current in
            if let previous, current == nil { viewModel.onFolderStyleRequested(folderId: previous) }
        }
    }

    @ViewBuilder
    private var header: some View {
        if state.isSelectingNotes {
            HStack(spacing: OffhandSpacing.controls) {
                Text(String.localizedStringWithFormat(String(localized: "%lld selected"), state.selectedNoteIds.count))
                    .offhandText(.title)
                    .foregroundStyle(OffhandColor.ink)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .accessibilityAddTraits(.isHeader)
                PlainTextButton(String(localized: "Cancel"), tone: .ink) { viewModel.onNoteSelectionCleared() }
                    .accessibilityLabel(String(localized: "Cancel selection"))
            }
            .padding(.leading, OffhandSpacing.screen)
            .padding(.trailing, OffhandSpacing.controls)
            .padding(.top, 8)
            .padding(.bottom, 12)
            .overlay(alignment: .bottom) { Hairline() }
        } else if isSearching {
            HStack(spacing: OffhandSpacing.inRow) {
                SearchField(
                    query: $searchQuery,
                    placeholder: String(localized: "Search notes"),
                    clearLabel: String(localized: "Clear search"),
                    focusesOnAppear: true
                )
                PlainTextButton(String(localized: "Cancel"), tone: .ink, action: closeSearch)
            }
            .padding(.leading, OffhandSpacing.screen)
            .padding(.trailing, OffhandSpacing.controls)
            .padding(.top, 8)
            .padding(.bottom, 12)
            .overlay(alignment: .bottom) { Hairline() }
        } else {
            HStack {
                Text(String(localized: "Notes"))
                    .offhandText(.display)
                    .foregroundStyle(OffhandColor.ink)
                    .accessibilityAddTraits(.isHeader)
                Spacer()
                BarIconButton(systemImage: "magnifyingglass", label: String(localized: "Search notes")) {
                    isSearching = true
                }
            }
            .padding(.leading, OffhandSpacing.screen)
            .padding(.trailing, 14)
            .padding(.top, 8)
        }
    }

    private var folderTabs: some View {
        FolderTabs(
            folders: state.folders,
            selectedFolderId: state.selectedFolderId?.int64Value,
            isUnsortedShown: state.isUnsortedShown,
            isFolderStylesUnlocked: state.isFolderStylesUnlocked,
            isFolderStyleShown: state.isSummaryAvailable,
            isFolderLimitReached: state.isFolderLimitReached,
            onSelect: { viewModel.onFolderSelected(folderId: $0.map { KotlinLong(value: $0) }) },
            onNew: { viewModel.onNewFolderRequested() },
            onRename: { viewModel.onRenameFolderRequested(folderId: $0) },
            onDelete: { viewModel.onDeleteFolderRequested(folderId: $0) },
            onStyle: { viewModel.onFolderStyleRequested(folderId: $0) },
            onMove: { viewModel.onFolderMoved(fromIndex: Int32($0), toIndex: Int32($1)) }
        )
    }

    @ViewBuilder
    private var footer: some View {
        if state.isSelectingNotes {
            SelectionFooter(
                onMove: { viewModel.onBulkActionRequested(action: .move) },
                onDelete: { viewModel.onBulkActionRequested(action: .delete) }
            )
        } else if !isSearching {
            bottomBar
        }
    }

    private var notesContent: some View {
        List {
            if let preparation = state.modelPreparation {
                ModelPreparationCard(
                    preparation: preparation,
                    onDownloadOnMobileData: { viewModel.onDownloadOnMobileDataClicked() }
                )
                .notesListRow(top: 20)
            }
            if let survey = state.shownUsageSurvey, !isSearching {
                UsageSurveyCard(
                    stage: survey.stage,
                    onAnswer: { viewModel.onUsageSurveyOpened() },
                    onNotNow: { viewModel.onUsageSurveySkipped() },
                    onThanksClosed: { viewModel.onUsageSurveyThanksClosed() }
                )
                .notesListRow(top: 16)
            }
            if !state.noteProgress.isEmpty {
                StatusCard(
                    title: String(localized: "Preparing your note"),
                    message: String(localized: "Keep Offhand open until your note is ready.")
                )
                .accessibilityElement(children: .combine)
                .notesListRow(top: 12)
            }
            if isShowingSearchResults {
                SectionLabel(String.localizedStringWithFormat(String(localized: "%lld notes"), noteCount))
                    .notesListRow(top: 20, bottom: 4)
            }
            ForEach(state.sections, id: \.self) { section in
                if !isSearching {
                    SectionLabel(dayTitle(section.dayLabel))
                        .notesListRow(top: 22, bottom: 4)
                }
                ForEach(section.notes, id: \.id) { note in
                    noteRow(note, day: isSearching ? dayTitle(section.dayLabel) : nil)
                }
            }
            if isShowingSearchResults {
                Text(String(localized: "Search looks through titles, summaries and transcripts."))
                    .offhandText(.caption)
                    .foregroundStyle(OffhandColor.ink3)
                    .notesListRow(top: 20)
            }
        }
        .listStyle(.plain)
        .scrollContentBackground(.hidden)
        .environment(\.defaultMinListRowHeight, 0)
        .overlay { emptyOrLoading }
    }

    @ViewBuilder
    private var emptyOrLoading: some View {
        if !state.isNotesLoaded {
            NotesLoadingState()
        } else if state.sections.isEmpty {
            emptyState
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .allowsHitTesting(false)
        }
    }

    @ViewBuilder
    private var emptyState: some View {
        if !searchQuery.isEmpty {
            EmptyState(
                title: String(format: String(localized: "No notes match “%@”"), searchQuery),
                message: String(localized: "Search looks through titles, summaries and transcripts.")
            )
        } else if state.selectedFolderId != nil {
            EmptyState(title: String(localized: "No notes in this folder yet"))
        } else if state.isUnsortedShown {
            EmptyState(
                title: String(localized: "No unsorted notes"),
                message: String(localized: "Notes outside your folders appear here.")
            )
        } else {
            EmptyState(
                title: String(localized: "No notes yet"),
                message: state.isSummaryAvailable
                    ? String(localized: "Tap Record and speak. Offhand writes the transcript and a summary on your iPhone.")
                    : String(localized: "Tap Record and speak. Offhand writes the transcript on your iPhone.")
            )
        }
    }

    private var isShowingSearchResults: Bool {
        isSearching && !searchQuery.isEmpty && !state.sections.isEmpty
    }

    private var noteCount: Int {
        state.sections.reduce(0) { $0 + $1.notes.count }
    }

    private func noteRow(_ note: NoteCardUi, day: String?) -> some View {
        let isSelected = isNoteSelected(note.id)
        let isOpen = isRegularWidth && state.selected?.id == note.id
        return Button {
            onRowTapped(note)
        } label: {
            NoteRow(
                title: highlighted(note.title, note.titleHighlights),
                state: rowState(note),
                meta: metaLine(note, day: day),
                waveSeed: note.id,
                waveform: note.waveform.map { CGFloat($0.floatValue) },
                previewLines: isSearching ? 2 : 1
            ) {
                if state.isSelectingNotes {
                    ChoiceMark(isSelected: isSelected, size: 24)
                }
            }
            .padding(.horizontal, isOpen ? OffhandSpacing.controls : OffhandSpacing.screen)
            .background {
                if isOpen {
                    RoundedRectangle(cornerRadius: OffhandRadius.field, style: .continuous)
                        .fill(OffhandColor.surface)
                        .shadow(color: OffhandColor.ink.opacity(0.06), radius: 1, x: 0, y: 1)
                } else if isSelected {
                    OffhandColor.accentSoft
                }
            }
            .padding(.horizontal, isOpen ? OffhandSpacing.controls : 0)
            .overlay(alignment: .bottom) {
                if !isOpen {
                    Hairline().padding(.horizontal, state.isSelectingNotes ? 0 : OffhandSpacing.screen)
                }
            }
        }
        .buttonStyle(.offhandRow)
        .simultaneousGesture(LongPressGesture(minimumDuration: 0.4).onEnded { _ in onRowHeld(note.id) })
        .accessibilityAddTraits(isSelected ? .isSelected : [])
        .listRowInsets(EdgeInsets())
        .listRowSeparator(.hidden)
        .listRowBackground(Color.clear)
        .swipeActions(edge: .leading, allowsFullSwipe: true) {
            if !state.isSelectingNotes {
                Button {
                    Haptics.confirm()
                    viewModel.onMoveToFolderRequested(noteId: note.id)
                } label: {
                    Label(String(localized: "Move to folder"), systemImage: "folder")
                }
                .tint(OffhandColor.accent)
            }
        }
        .swipeActions(edge: .trailing, allowsFullSwipe: false) {
            if !state.isSelectingNotes {
                Button(role: .destructive) {
                    Haptics.confirm()
                    viewModel.onDeleteRequested(id: note.id)
                } label: {
                    Label(String(localized: "Delete"), systemImage: "trash")
                }
                .tint(OffhandColor.danger)
            }
        }
    }

    private func rowState(_ note: NoteCardUi) -> NoteRowState {
        switch note.status {
        case .processing:
            let progress = state.noteProgress[KotlinLong(value: note.id)]?.intValue
            if state.isWaitingForAi {
                return .processing(text: String(localized: "Waiting for your on-device AI to finish downloading"), progress: nil)
            }
            return .processing(text: processingText, progress: progress.map { Double($0) / 100 })
        case .interrupted:
            return .attention(text: String(localized: "Paused. Open the note to continue."))
        case .failed:
            return .attention(text: state.isSummaryAvailable
                ? String(localized: "We were unable to create a summary and transcript for this note.")
                : String(localized: "We were unable to create a transcript for this note."))
        default:
            return .ready(preview: note.preview.isEmpty ? nil : highlighted(note.preview, note.previewHighlights))
        }
    }

    private var processingText: String {
        state.isSummaryAvailable ? String(localized: "Preparing your note") : String(localized: "Transcribing your recording")
    }

    private func metaLine(_ note: NoteCardUi, day: String?) -> String {
        let time = [day, note.time].compactMap { $0 }.joined(separator: " ")
        if isSearching, let match = matchText(note) {
            return [time, match].joined(separator: " · ")
        }
        return [time, note.durationText, note.folderName].compactMap { $0 }.joined(separator: " · ")
    }

    private func matchText(_ note: NoteCardUi) -> String? {
        switch note.matchLocation {
        case .title: return String(localized: "In title")
        case .summary: return String(localized: "In summary")
        case .transcript:
            guard let time = note.matchTime else { return String(localized: "In transcript") }
            return String(format: String(localized: "In transcript at %@"), time)
        default: return nil
        }
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
            attributed[start..<end].backgroundColor = OffhandColor.accentSoft
            attributed[start..<end].foregroundColor = OffhandColor.ink
        }
        return attributed
    }

    private func styleLabel(_ style: NoteStyleRef) -> String {
        if let preset = style.builtInPreset { return NoteStyleLabels.label(for: preset) }
        return state.customStyles.first { $0.id == style.customId }?.name ?? NoteStyleLabels.label(for: .summary)
    }

    private func folderStyleLabel(_ style: NoteStyleRef) -> String? {
        state.isSummaryAvailable ? styleLabel(style) : nil
    }

    private func rewriteNotice(folderId: Int64?, noteCount: Int) -> String? {
        guard state.rewritesNotesMovedTo(folderId: folderId.map { KotlinLong(value: $0) }),
              let folder = state.folders.first(where: { $0.id == folderId }),
              let style = folder.style else { return nil }
        let format = noteCount == 1
            ? String(localized: "%1$@ writes notes in the %2$@ style, so this note will be rewritten. Your transcript does not change.")
            : String(localized: "%1$@ writes notes in the %2$@ style, so these notes will be rewritten. Your transcripts do not change.")
        return String(format: format, folder.name, styleLabel(style))
    }

    private func closeSearch() {
        searchQuery = ""
        isSearching = false
    }

    private func requestAppStoreReview() -> Bool {
        guard let scene = UIApplication.shared.connectedScenes
            .first(where: { $0.activationState == .foregroundActive }) as? UIWindowScene else { return false }
        AppStore.requestReview(in: scene)
        return true
    }

    private var detailBinding: Binding<Bool> {
        Binding(
            get: { state.selected != nil },
            set: { isShown in if !isShown { viewModel.onDetailClosed() } }
        )
    }

    private func bulkBinding(_ action: BulkNoteActionUi) -> Binding<Bool> {
        Binding(
            get: { state.bulkAction == action },
            set: { if !$0 && state.bulkAction == action { viewModel.onBulkActionDismissed() } }
        )
    }

    private func isNoteSelected(_ id: Int64) -> Bool {
        state.selectedNoteIds.contains(KotlinLong(value: id))
    }

    private func onRowTapped(_ note: NoteCardUi) {
        if longPressedNoteId == note.id {
            longPressedNoteId = nil
        } else if state.isSelectingNotes {
            viewModel.onNoteSelectionToggled(id: note.id)
        } else if isSearching && note.matchLocation != nil {
            viewModel.onSearchResultSelected(id: note.id)
        } else {
            viewModel.onNoteSelected(id: note.id)
        }
    }

    private func onRowHeld(_ id: Int64) {
        Haptics.confirm()
        longPressedNoteId = id
        viewModel.onNoteSelectionToggled(id: id)
    }

    private var deleteBinding: Binding<Bool> {
        Binding(
            get: { state.pendingDeleteNoteId != nil },
            set: { isShown in if !isShown { viewModel.onDeleteDismissed() } }
        )
    }

    private var usageSurveySheetBinding: Binding<Bool> {
        Binding(
            get: { state.usageSurvey?.isSheetVisible == true },
            set: { isShown in if !isShown { viewModel.onUsageSurveySheetClosed() } }
        )
    }

    private var moveToFolderBinding: Binding<Bool> {
        Binding(
            get: { state.moveToFolder != nil },
            set: { isShown in if !isShown { viewModel.onMoveToFolderDismissed() } }
        )
    }

    private func requestNewFolderAfterMove() {
        guard isNewFolderRequestedAfterMove else { return }
        isNewFolderRequestedAfterMove = false
        viewModel.onNewFolderRequested()
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

    private func dayTitle(_ label: NoteDayLabelUi) -> String {
        switch onEnum(of: label) {
        case .today: return String(localized: "Today")
        case .yesterday: return String(localized: "Yesterday")
        case .date(let date): return date.text
        }
    }
}

private extension View {
    func notesListRow(top: CGFloat, bottom: CGFloat = 0) -> some View {
        listRowInsets(EdgeInsets(top: top, leading: OffhandSpacing.screen, bottom: bottom, trailing: OffhandSpacing.screen))
            .listRowSeparator(.hidden)
            .listRowBackground(Color.clear)
    }
}

private struct FolderTabs: View {
    let folders: [FolderUi]
    let selectedFolderId: Int64?
    let isUnsortedShown: Bool
    let isFolderStylesUnlocked: Bool
    let isFolderStyleShown: Bool
    let isFolderLimitReached: Bool
    let onSelect: (Int64?) -> Void
    let onNew: () -> Void
    let onRename: (Int64) -> Void
    let onDelete: (Int64) -> Void
    let onStyle: (Int64) -> Void
    let onMove: (Int, Int) -> Void

    private static let spacing: CGFloat = 22

    var body: some View {
        UnderlineTabs(
            tabs: [TabItem(label: isUnsortedShown ? String(localized: "Unsorted") : String(localized: "All notes"))],
            selectedIndex: selectedFolderId == nil ? 0 : -1,
            spacing: Self.spacing,
            contentPadding: OffhandSpacing.screen,
            onSelect: { _ in onSelect(nil) }
        ) {
            ForEach(Array(folders.enumerated()), id: \.element.id) { index, folder in
                folderTab(folder)
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
            newFolderButton
        }
    }

    @ViewBuilder
    private func folderTab(_ folder: FolderUi) -> some View {
        if folder.id == selectedFolderId {
            Menu {
                folderMenuItems(folder)
            } label: {
                UnderlineTab(tab: TabItem(label: folder.name), isSelected: true) {}
                    .allowsHitTesting(false)
                    .contentShape(Rectangle())
            }
            .accessibilityHint(String(localized: "Folder options"))
        } else {
            UnderlineTab(tab: TabItem(label: folder.name), isSelected: false) { onSelect(folder.id) }
        }
    }

    private var newFolderButton: some View {
        Button(action: onNew) {
            HStack(spacing: 6) {
                Image(systemName: "plus")
                    .font(.system(size: 18, weight: .regular))
                    .foregroundStyle(OffhandColor.ink2)
                if isFolderLimitReached { ProBadge() }
            }
            .frame(minWidth: 28, minHeight: OffhandSpacing.touchTarget)
            .contentShape(Rectangle())
        }
        .buttonStyle(.offhandPress)
        .accessibilityElement(children: .combine)
        .accessibilityLabel(String(localized: "New folder"))
    }

    @ViewBuilder
    private func folderMenuItems(_ folder: FolderUi) -> some View {
        if isFolderStyleShown {
            Button {
                onStyle(folder.id)
            } label: {
                Label(styleMenuTitle, systemImage: "text.alignleft")
            }
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
    }

    private var styleMenuTitle: String {
        isFolderStylesUnlocked
            ? String(localized: "Writing style")
            : String(localized: "Writing style (Pro)")
    }
}

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
                isProStylesUnlocked: state.isFolderStylesUnlocked,
                footer: String(localized: "New recordings in this folder use it, and notes moved here are rewritten in it."),
                onSelect: { viewModel.onFolderStyleSelected(style: $0) },
                onCreateStyle: onCreateStyle,
                onDefaultSelected: { viewModel.onFolderStyleSelected(style: nil) }
            )
            .navigationTitle(String(format: String(localized: "Writing style for %@"), picker.folderName))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(String(localized: "Cancel")) { viewModel.onFolderStyleDismissed() }
                }
            }
        }
        .tint(OffhandColor.ink)
    }
}

private enum FolderChoice: Equatable {
    case unsorted
    case folder(Int64)

    init(folderId: Int64?) {
        self = folderId.map { .folder($0) } ?? .unsorted
    }

    var folderId: Int64? {
        if case .folder(let id) = self { return id }
        return nil
    }
}

private struct MoveToFolderSheet: View {
    let title: String
    let folders: [FolderUi]
    let initialChoice: FolderChoice?
    let isNewFolderGated: Bool
    let styleLabel: (NoteStyleRef) -> String?
    let rewriteNotice: (Int64?) -> String?
    let onMove: (Int64?) -> Void
    let onNewFolder: () -> Void
    let onCancel: () -> Void
    @State private var choice: FolderChoice?

    private static let rowHeight: CGFloat = 60
    private static let maxVisibleRows = 6

    var body: some View {
        OffhandSheet(title: title) {
            if folders.count + 2 > Self.maxVisibleRows {
                ScrollView { rows }
                    .frame(height: Self.rowHeight * CGFloat(Self.maxVisibleRows))
            } else {
                rows
            }
            if let notice = choice.flatMap(notice(for:)) {
                Text(notice)
                    .offhandText(.caption)
                    .foregroundStyle(OffhandColor.ink3)
                    .fixedSize(horizontal: false, vertical: true)
                    .padding(.top, 10)
            }
            HStack(spacing: OffhandSpacing.controls) {
                SecondaryButton(String(localized: "Cancel"), fillsWidth: true, action: onCancel)
                PrimaryButton(String(localized: "Move"), fillsWidth: true) {
                    guard let choice else { return }
                    onMove(choice.folderId)
                }
                .disabled(choice == nil)
            }
            .padding(.top, 18)
        }
        .onAppear { choice = initialChoice }
    }

    private var rows: some View {
        VStack(spacing: 0) {
            ChoiceRow(title: String(localized: "No folder"), isSelected: choice == .unsorted) {
                choice = .unsorted
            }
            ForEach(folders, id: \.id) { folder in
                ChoiceRow(
                    title: folder.name,
                    hint: hint(for: folder),
                    isSelected: choice == .folder(folder.id)
                ) {
                    choice = .folder(folder.id)
                }
            }
            Button(action: onNewFolder) {
                HStack(spacing: 14) {
                    Image(systemName: "plus")
                        .font(.system(size: 18, weight: .regular))
                        .frame(width: 22, height: 22)
                    Text(String(localized: "New folder"))
                        .offhandText(.body, face: .interMedium)
                    if isNewFolderGated { ProBadge() }
                }
                .foregroundStyle(OffhandColor.ink)
                .frame(maxWidth: .infinity, minHeight: Self.rowHeight, alignment: .leading)
            }
            .buttonStyle(.offhandRow)
        }
    }

    private func notice(for choice: FolderChoice) -> String? {
        guard choice != initialChoice else { return nil }
        return rewriteNotice(choice.folderId)
    }

    private func hint(for folder: FolderUi) -> String {
        let count = String(folder.noteCount)
        guard let label = folder.style.flatMap(styleLabel) else { return count }
        return "\(label.localizedUppercase) · \(count)"
    }
}

private struct SelectionFooter: View {
    let onMove: () -> Void
    let onDelete: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            Hairline()
            HStack(spacing: OffhandSpacing.controls) {
                PrimaryButton(String(localized: "Move to folder"), systemImage: "folder", fillsWidth: true, action: onMove)
                PlainTextButton(String(localized: "Delete"), tone: .danger, action: onDelete)
            }
            .padding(.horizontal, OffhandSpacing.screen)
            .padding(.top, OffhandSpacing.card)
            .padding(.bottom, OffhandSpacing.controls)
        }
        .background(OffhandColor.ground.ignoresSafeArea(edges: .bottom))
    }
}

private struct ModelPreparationCard: View {
    let preparation: ModelPreparationUi
    let onDownloadOnMobileData: () -> Void
    @State private var isInfoVisible = false

    var body: some View {
        card
            .offhandDialog(isPresented: $isInfoVisible) {
                OffhandDialog(
                    title: String(localized: "Your on-device AI"),
                    message: String(localized: "Offhand turns recordings into notes with on-device AI that runs entirely on your iPhone. It is a one-time download; after that, your notes never leave your iPhone."),
                    confirmLabel: String(localized: "Got it"),
                    onConfirm: { isInfoVisible = false },
                    onDismiss: { isInfoVisible = false }
                )
            }
    }

    @ViewBuilder
    private var card: some View {
        switch onEnum(of: preparation) {
        case .downloading(let downloading):
            StatusCard(
                title: String(localized: "Setting up your on-device AI"),
                number: "\(Int(downloading.progressPercent))%",
                progress: Double(downloading.progressPercent) / 100,
                showsProgress: true,
                message: String(localized: "New notes start processing once this finishes."),
                titleTrailing: { infoButton },
                actions: { EmptyView() }
            )
        case .waitingForMobileData(let waiting):
            StatusCard(
                title: String(localized: "Your on-device AI is ready to download"),
                message: String(format: String(localized: "About %@ GB · waits for Wi‑Fi"), waiting.sizeGb),
                titleTrailing: { infoButton },
                actions: {
                    PlainTextButton(String(localized: "Download on mobile data"), action: onDownloadOnMobileData)
                        .padding(.horizontal, -OffhandSpacing.controls)
                }
            )
        }
    }

    private var infoButton: some View {
        Button {
            isInfoVisible = true
        } label: {
            Image(systemName: "info.circle")
                .font(.system(size: 16, weight: .regular))
                .foregroundStyle(OffhandColor.ink3)
                .frame(width: 24, height: 24)
                .contentShape(Rectangle())
        }
        .buttonStyle(.offhandPress)
        .accessibilityLabel(String(localized: "About your on-device AI"))
    }
}

private struct RecordSheetRequest: Identifiable {
    let id = UUID()
    let folderId: Int64?
}

private struct NotesLoadingState: View {
    private static let lineWidths: [CGFloat] = [0.55, 0.9, 0.3]

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            ForEach(0..<4, id: \.self) { _ in
                Skeleton(lineWidths: Self.lineWidths)
                    .padding(.vertical, 18)
                    .overlay(alignment: .bottom) { Hairline() }
            }
            Spacer()
        }
        .padding(.horizontal, OffhandSpacing.screen)
        .padding(.top, 12)
        .background(OffhandColor.ground)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(String(localized: "Loading your notes"))
    }
}
