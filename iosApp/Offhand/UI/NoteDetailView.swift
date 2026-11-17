import OffhandShared
import SwiftUI
import UIKit

struct NoteDetailView: View {
    let viewModel: NotesViewModel
    let detail: NoteDetailUi
    let state: NotesUiState
    @State private var shareItems: [URL] = []
    @State private var exportItems: [URL] = []
    @State private var isNewStyleRequested = false
    @State private var isNewStyleEditorVisible = false

    var body: some View {
        VStack(spacing: 0) {
            pinnedHeader
            ScrollViewReader { proxy in
                ScrollView {
                    VStack(alignment: .leading, spacing: 0) {
                        tabContent
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(.horizontal, OffhandSpacing.screen)
                    .padding(.top, 14)
                    .padding(.bottom, OffhandSpacing.screen)
                }
                .onChange(of: playingPartIndex) {
                    guard let playingPartIndex, state.shownDetailTab == .transcript else { return }
                    withAnimation(OffhandMotion.sheet) {
                        proxy.scrollTo(TimedTranscript.rowId(playingPartIndex), anchor: .center)
                    }
                }
                .task(id: transcriptFocusIndex) {
                    guard let transcriptFocusIndex else { return }
                    try? await Task.sleep(for: Self.focusDelay)
                    guard !Task.isCancelled else { return }
                    withAnimation(OffhandMotion.sheet) {
                        proxy.scrollTo(TimedTranscript.rowId(transcriptFocusIndex), anchor: .top)
                    }
                    viewModel.onTranscriptFocusHandled()
                }
            }
        }
        .safeAreaInset(edge: .bottom, spacing: 0) {
            NoteDetailFooter(
                copyTarget: copyTarget,
                onCopied: { viewModel.onNoteCopied(section: $0) },
                onShare: { viewModel.onShareRequested() }
            )
        }
        .background(OffhandColor.ground.ignoresSafeArea())
        .toolbar(.visible, for: .navigationBar)
        .navigationTitle("")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .primaryAction) { moreMenu }
        }
        .tint(OffhandColor.ink)
        .task(id: processingHapticsKey) { await runProcessingHaptics() }
        .offhandDialog(isPresented: retranscribeBinding) {
            OffhandDialog(
                title: String(localized: "Re-transcribe this note?"),
                message: String(localized: "The recording will be transcribed and summarized again, replacing the current title, summary and transcript. The audio recording itself is kept."),
                confirmLabel: String(localized: "Re-transcribe"),
                onConfirm: { viewModel.onRetranscribeConfirmed() },
                dismissLabel: String(localized: "Cancel"),
                onDismiss: { viewModel.onRetranscribeDismissed() }
            )
        }
        .sheet(isPresented: shareBinding) {
            ShareNoteSheet(
                viewModel: viewModel,
                hasAudio: detail.hasAudio,
                isDocumentExportUnlocked: state.isDocumentExportUnlocked
            )
        }
        .sheet(isPresented: editorBinding) {
            if let editor = state.editor {
                NoteEditorView(viewModel: viewModel, editor: editor)
            }
        }
        .sheet(item: pendingEventBinding) { pending in
            CalendarEventEditor(suggestion: pending.suggestion) { saved in
                viewModel.onCalendarEventLaunched(isAdded: saved)
            }
            .ignoresSafeArea()
        }
        .sheet(isPresented: presetBinding, onDismiss: openRequestedStyleEditor) {
            NoteStyleSheet(
                viewModel: viewModel,
                current: detail.style,
                customStyles: state.customStyles,
                isProStylesUnlocked: state.isProStylesUnlocked,
                onCreateStyle: {
                    isNewStyleRequested = true
                    viewModel.onPresetSheetDismissed()
                }
            )
            .presentationDetents([.medium, .large])
        }
        .navigationDestination(isPresented: $isNewStyleEditorVisible) {
            NoteStyleEditorView(styleId: 0)
        }
        .onChange(of: isNewStyleEditorVisible) {
            if !isNewStyleEditorVisible { viewModel.onPresetSheetRequested() }
        }
        .sheet(isPresented: shareItemsBinding) {
            ActivityShareSheet(items: shareItems, onComplete: dismissShare)
        }
        .sheet(isPresented: exportItemsBinding) {
            DocumentExporter(items: exportItems, onComplete: dismissExport)
                .ignoresSafeArea()
        }
        .onChange(of: state.pendingShare) {
            guard let pending = state.pendingShare else { return }
            let urls = pending.filePaths.map { URL(fileURLWithPath: $0) }
            if pending.saveToDevice {
                exportItems = urls
            } else {
                shareItems = urls
            }
            viewModel.onShareLaunched()
        }
    }

    private var pinnedHeader: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text(detail.title)
                .offhandText(.title)
                .foregroundStyle(OffhandColor.ink)
                .lineLimit(2)
                .accessibilityAddTraits(.isHeader)
            MetaText(metaLine)
                .padding(.top, 8)
            if let metrics = detail.metrics, state.isDeveloperMode {
                Text(
                    String(
                        format: String(localized: "Transcribed in %@ · Structured in %@ · %@"),
                        metrics.transcriptionTime,
                        metrics.structuringTime,
                        metrics.hardwareBackend
                    )
                )
                .offhandText(.caption)
                .foregroundStyle(OffhandColor.ink3)
                .padding(.top, 4)
            }
            if isPlayerShown {
                player.padding(.top, 22)
            }
            if state.visibleDetailTabs.count > 1 {
                UnderlineTabs(
                    tabs: [
                        TabItem(
                            label: String(localized: "Summary"),
                            dotDescription: state.isSummaryReadyMarked ? String(localized: "Summary is ready") : nil
                        ),
                        TabItem(label: String(localized: "Transcript")),
                    ],
                    selectedIndex: state.shownDetailTab == .transcript ? 1 : 0,
                    onSelect: { index in
                        Haptics.tap()
                        viewModel.onDetailTabSelected(tab: index == 0 ? .summary : .transcript)
                    }
                )
                .padding(.top, 18)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, OffhandSpacing.screen)
        .padding(.top, 8)
    }

    private var isPlayerShown: Bool { detail.hasAudio && state.playback.isAvailable }

    private var player: some View {
        let playback = state.playback
        return Player(
            isPlaying: playback.isPlaying,
            progress: Double(playback.progress),
            elapsedLabel: playback.isPlaying || playback.progress > 0 ? playback.positionText : playback.durationText,
            playLabel: String(localized: "Play recording"),
            pauseLabel: String(localized: "Pause playback"),
            seekLabel: String(localized: "Playback position"),
            waveSeed: detail.id,
            waveform: state.detailWaveform.map { CGFloat($0.floatValue) },
            onToggle: {
                Haptics.confirm()
                viewModel.onPlayPauseClicked()
            },
            onSeek: { viewModel.onSeekRequested(fraction: Float($0)) }
        )
    }

    private var metaLine: String {
        var parts = [detail.createdAt]
        if isPlayerShown {
            parts.append(state.playback.durationText)
        }
        if let folder = detail.folderName {
            parts.append(folder)
        }
        return parts.joined(separator: " · ")
    }

    private var moreMenu: some View {
        Menu {
            Button {
                viewModel.onEditStarted()
            } label: {
                Label(String(localized: "Edit note"), systemImage: "pencil")
            }
            if !detail.transcript.isEmpty && state.isSummaryAvailable {
                Button {
                    viewModel.onPresetSheetRequested()
                } label: {
                    Label(String(localized: "Change writing style"), systemImage: "text.alignleft")
                }
            }
            Button {
                viewModel.onMoveToFolderRequested()
            } label: {
                Label(String(localized: "Move to folder"), systemImage: "folder")
            }
            if detail.hasAudio && state.isRetranscribeAvailable {
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
            Image(systemName: "ellipsis")
                .foregroundStyle(OffhandColor.ink)
        }
        .accessibilityLabel(String(localized: "More actions"))
    }

    @ViewBuilder
    private var tabContent: some View {
        if state.shownDetailTab == .summary {
            summaryTab
        } else if state.shownDetailTab == .transcript {
            transcriptTab
        } else {
            PlainNotice(text: String(localized: "This note is the recording only."))
        }
    }

    @ViewBuilder
    private var summaryTab: some View {
        switch detail.status {
        case .processing:
            WritingState(text: summaryWritingText, percent: percent, isWaiting: state.isWaitingForAi)
        case .interrupted:
            pausedCard
        case .failed:
            failedCard
        default:
            MarkdownBlocks(raw: detail.body)
                .textSelection(.enabled)
        }
        if !state.styleChips.isEmpty {
            StyleChipsRow(chips: state.styleChips) { viewModel.onStyleSelected(style: $0) }
                .padding(.top, 28)
        }
        if let suggestions = state.smartSuggestions {
            SmartSuggestionsSection(viewModel: viewModel, suggestions: suggestions)
                .padding(.top, 28)
        }
    }

    @ViewBuilder
    private var transcriptTab: some View {
        if !detail.transcript.isEmpty {
            if detail.transcriptParts.isEmpty {
                MarkdownBlocks(raw: detail.transcript)
                    .textSelection(.enabled)
            } else {
                TimedTranscript(
                    parts: detail.transcriptParts,
                    playingIndex: playingPartIndex,
                    onTimeSelected: isPlayerShown ? { viewModel.onTranscriptTimeSelected(startMs: $0) } : nil
                )
            }
            if state.isTranscribing {
                WritingState(text: String(localized: "Still transcribing the rest…"), percent: percent, isWaiting: false)
                    .padding(.top, 24)
            }
        } else if detail.status == .processing {
            WritingState(text: transcriptWritingText, percent: percent, isWaiting: state.isWaitingForSpeech)
        } else if detail.status == .interrupted {
            pausedCard
        } else if detail.status == .failed {
            failedCard
        } else {
            PlainNotice(text: String(localized: "This note has no transcript."))
        }
    }

    private static let focusDelay: Duration = .milliseconds(250)

    private var playingPartIndex: Int? {
        state.playingPartIndex?.intValue
    }

    private var transcriptFocusIndex: Int? {
        state.transcriptFocusIndex?.intValue
    }

    private var percent: Int? {
        state.noteProgress[KotlinLong(value: detail.id)]?.intValue
    }

    private var copyTarget: CopyTarget? {
        if state.shownDetailTab == .summary, detail.status == .ready, !detail.body.isEmpty {
            return CopyTarget(text: detail.body, label: String(localized: "Copy summary"), section: .overview)
        }
        if state.shownDetailTab == .transcript, !detail.transcript.isEmpty {
            return CopyTarget(text: detail.transcript, label: String(localized: "Copy transcript"), section: .transcript)
        }
        return nil
    }

    private var processingHapticsKey: String { "\(detail.id)-\(detail.status)" }

    private func runProcessingHaptics() async {
        guard detail.status == .processing else { return }
        while !Task.isCancelled {
            try? await Task.sleep(for: .seconds(2.5))
            guard !Task.isCancelled else { return }
            Haptics.tap()
        }
    }

    private var pausedCard: some View {
        MessageCard(
            title: String(localized: "Your note is paused"),
            message: String(localized: "This note was paused before it was finished. Keep Offhand open and it will continue.")
        ) {
            PrimaryButton(String(localized: "Continue"), fillsWidth: true, height: 48) {
                viewModel.onResumeProcessingRequested()
            }
            .padding(.top, 10)
        }
    }

    private var failedCard: some View {
        MessageCard(title: failedTitle, message: failedMessage) {
            if detail.hasAudio {
                SecondaryButton(String(localized: "Try again"), fillsWidth: true, height: 48) {
                    viewModel.onRetryTranscriptionRequested()
                }
                .padding(.top, 10)
            }
        }
    }

    private var failedTitle: String {
        state.isSummaryAvailable
            ? String(localized: "The summary could not be written")
            : String(localized: "The transcript could not be written")
    }

    private var failedMessage: String {
        if !state.isSummaryAvailable {
            return String(localized: "We were unable to create a transcript for this note.")
        }
        return detail.transcript.isEmpty
            ? String(localized: "We were unable to create a summary and transcript for this note.")
            : String(localized: "We were unable to create a summary for this note. The transcript is saved.")
    }

    private var summaryWritingText: String {
        state.isWaitingForAi
            ? String(localized: "Waiting for your on-device AI to finish downloading")
            : String(localized: "Writing your summary…")
    }

    private var transcriptWritingText: String {
        state.isWaitingForSpeech
            ? String(localized: "Waiting for your on-device AI to finish downloading")
            : String(localized: "Transcribing your recording")
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

    private var pendingEventBinding: Binding<PendingCalendarEvent?> {
        Binding(
            get: { state.pendingCalendarEvent.map { PendingCalendarEvent(suggestion: $0) } },
            set: { pending in if pending == nil { viewModel.onCalendarEventLaunched(isAdded: false) } }
        )
    }

    private func openRequestedStyleEditor() {
        guard isNewStyleRequested else { return }
        isNewStyleRequested = false
        isNewStyleEditorVisible = true
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

    private var exportItemsBinding: Binding<Bool> {
        Binding(
            get: { !exportItems.isEmpty },
            set: { isShown in if !isShown { dismissExport() } }
        )
    }

    private func dismissShare() {
        guard !shareItems.isEmpty else { return }
        shareItems = []
        viewModel.onShareCompleted()
    }

    private func dismissExport() {
        guard !exportItems.isEmpty else { return }
        exportItems = []
        viewModel.onShareCompleted()
    }
}

private struct CopyTarget: Equatable {
    let text: String
    let label: String
    let section: NoteSection
}

private struct NoteDetailFooter: View {
    let copyTarget: CopyTarget?
    let onCopied: (NoteSection) -> Void
    let onShare: () -> Void
    @State private var copiedAt: Date?

    var body: some View {
        VStack(spacing: 0) {
            Hairline()
            HStack(spacing: OffhandSpacing.controls) {
                if let copyTarget {
                    PrimaryButton(
                        copyTarget.label,
                        systemImage: copiedAt == nil ? "doc.on.doc" : "checkmark",
                        fillsWidth: true
                    ) {
                        SensitivePasteboard.copy(copyTarget.text)
                        onCopied(copyTarget.section)
                        copiedAt = .now
                    }
                } else {
                    Spacer()
                }
                RoundIconButton(systemImage: "square.and.arrow.up", label: String(localized: "Share note"), action: onShare)
            }
            .padding(.horizontal, OffhandSpacing.screen)
            .padding(.top, OffhandSpacing.card)
            .padding(.bottom, OffhandSpacing.related)
        }
        .background(OffhandColor.ground.ignoresSafeArea(edges: .bottom))
        .sensoryFeedback(.success, trigger: copiedAt) { _, new in new != nil }
        .task(id: copiedAt) {
            guard copiedAt != nil else { return }
            try? await Task.sleep(for: .seconds(1.5))
            copiedAt = nil
        }
        .onChange(of: copyTarget) { copiedAt = nil }
    }
}

private struct WritingState: View {
    let text: String
    let percent: Int?
    let isWaiting: Bool

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(alignment: .firstTextBaseline, spacing: OffhandSpacing.controls) {
                Text(text)
                    .offhandText(.buttonSmall)
                    .foregroundStyle(isWaiting ? OffhandColor.ink2 : OffhandColor.accent)
                    .frame(maxWidth: .infinity, alignment: .leading)
                if let percent, !isWaiting {
                    Text("\(percent)%")
                        .offhandText(.number)
                        .foregroundStyle(OffhandColor.ink2)
                }
            }
            if !isWaiting {
                ProgressLine(progress: percent.map { Double($0) / 100 })
                Skeleton()
                    .padding(.top, 8)
                Text(String(localized: "Keep Offhand open until your note is ready."))
                    .offhandText(.caption)
                    .foregroundStyle(OffhandColor.ink2)
                    .padding(.top, 8)
            }
        }
    }
}

private struct PlainNotice: View {
    let text: String

    var body: some View {
        Text(text)
            .offhandText(.body)
            .foregroundStyle(OffhandColor.ink2)
            .frame(maxWidth: .infinity, alignment: .leading)
    }
}

private struct TimedTranscript: View {
    let parts: [TranscriptPartUi]
    let playingIndex: Int?
    let onTimeSelected: ((Int64) -> Void)?

    private static let inset: CGFloat = 12

    static func rowId(_ index: Int) -> String { "transcript-part-\(index)" }

    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            ForEach(Array(parts.enumerated()), id: \.element.startMs) { index, part in
                partRow(part, isPlaying: index == playingIndex)
                    .id(Self.rowId(index))
            }
        }
    }

    private func partRow(_ part: TranscriptPartUi, isPlaying: Bool) -> some View {
        let shape = RoundedRectangle(cornerRadius: OffhandRadius.field, style: .continuous)
        return Button {
            onTimeSelected?(part.startMs)
        } label: {
            VStack(alignment: .leading, spacing: 6) {
                TimeMark(text: part.time, isActive: isPlaying)
                Text(part.text)
                    .offhandText(.body)
                    .foregroundStyle(OffhandColor.ink)
                    .multilineTextAlignment(.leading)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
            .padding(.vertical, 10)
            .padding(.horizontal, Self.inset)
            .background(isPlaying ? OffhandColor.accentSoft : .clear, in: shape)
            .contentShape(shape)
            .animation(OffhandMotion.state, value: isPlaying)
        }
        .buttonStyle(.offhandPress)
        .allowsHitTesting(onTimeSelected != nil)
        .padding(.horizontal, -Self.inset)
        .accessibilityAddTraits(isPlaying ? .isSelected : [])
        .accessibilityHint(String(format: String(localized: "Play from %@"), part.time))
    }
}

private struct MarkdownBlocks: View {
    let raw: String

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            ForEach(Array(lines.enumerated()), id: \.offset) { index, line in
                blockView(line, isFirst: index == 0)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private var lines: [String] {
        raw.split(separator: "\n", omittingEmptySubsequences: false)
            .map { $0.trimmingCharacters(in: .whitespaces) }
            .filter { !$0.isEmpty }
    }

    @ViewBuilder
    private func blockView(_ line: String, isFirst: Bool) -> some View {
        if let heading = heading(line) {
            SectionLabel(heading)
                .padding(.top, isFirst ? 0 : 16)
                .padding(.bottom, 2)
        } else if line.hasPrefix("- ") || line.hasPrefix("* ") {
            HStack(alignment: .top, spacing: OffhandSpacing.controls) {
                Circle()
                    .fill(OffhandColor.ink)
                    .frame(width: 5, height: 5)
                    .padding(.top, 10)
                    .accessibilityHidden(true)
                bodyText(String(line.dropFirst(2)))
            }
        } else if let item = numberedItem(line) {
            HStack(alignment: .firstTextBaseline, spacing: OffhandSpacing.related) {
                Text(item.marker)
                    .offhandText(.body)
                    .foregroundStyle(OffhandColor.ink2)
                bodyText(item.text)
            }
        } else {
            bodyText(line)
        }
    }

    private func bodyText(_ text: String) -> some View {
        Text(inlineMarkdown(text))
            .offhandText(.body)
            .foregroundStyle(OffhandColor.ink)
            .frame(maxWidth: .infinity, alignment: .leading)
            .fixedSize(horizontal: false, vertical: true)
    }

    private func heading(_ line: String) -> String? {
        for prefix in ["### ", "## ", "# "] where line.hasPrefix(prefix) {
            return String(line.dropFirst(prefix.count))
        }
        return nil
    }

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
    let current: NoteStyleRef
    let customStyles: [NoteStyleOptionUi]
    let isProStylesUnlocked: Bool
    let onCreateStyle: () -> Void

    var body: some View {
        NavigationStack {
            NoteStyleList(
                current: current,
                customStyles: customStyles.map { NoteStyleChoice(id: $0.id, name: $0.name, details: $0.description_) },
                isProStylesUnlocked: isProStylesUnlocked,
                footer: String(localized: "The recording is kept. The title and summary are written again from the transcript in the style you pick."),
                onSelect: { viewModel.onStyleSelected(style: $0) },
                onCreateStyle: onCreateStyle
            )
            .navigationTitle(String(localized: "Change writing style"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(String(localized: "Cancel")) { viewModel.onPresetSheetDismissed() }
                }
            }
        }
        .tint(OffhandColor.ink)
    }
}

private struct NoteEditorView: View {
    let viewModel: NotesViewModel
    @State private var title: String
    @State private var transcript: String

    init(viewModel: NotesViewModel, editor: NoteEditorUi) {
        self.viewModel = viewModel
        _title = State(initialValue: editor.title)
        _transcript = State(initialValue: editor.transcript)
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: OffhandSpacing.section) {
                    OffhandTextField(text: $title, label: String(localized: "Title"))
                    OffhandTextField(
                        text: $transcript,
                        label: String(localized: "Transcript"),
                        isMultiline: true,
                        minLines: 10
                    )
                }
                .padding(OffhandSpacing.screen)
            }
            .scrollDismissesKeyboard(.interactively)
            .background(OffhandColor.ground.ignoresSafeArea())
            .navigationTitle(String(localized: "Edit note"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(String(localized: "Cancel")) { viewModel.onEditCancelled() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button(String(localized: "Save")) { viewModel.onEditSaved() }
                        .foregroundStyle(OffhandColor.accent)
                }
            }
        }
        .tint(OffhandColor.ink)
        .presentationBackground(OffhandColor.ground)
        .onChange(of: title) { viewModel.onEditorTitleChanged(title: title) }
        .onChange(of: transcript) { viewModel.onEditorTranscriptChanged(transcript: transcript) }
    }
}

private struct ActivityShareSheet: UIViewControllerRepresentable {
    let items: [URL]
    let onComplete: () -> Void

    func makeUIViewController(context: Context) -> UIActivityViewController {
        let controller = UIActivityViewController(activityItems: items, applicationActivities: nil)
        controller.completionWithItemsHandler = { _, _, _, _ in onComplete() }
        return controller
    }

    func updateUIViewController(_ uiViewController: UIActivityViewController, context: Context) {}
}

private struct DocumentExporter: UIViewControllerRepresentable {
    let items: [URL]
    let onComplete: () -> Void

    func makeCoordinator() -> Coordinator { Coordinator(onComplete: onComplete) }

    func makeUIViewController(context: Context) -> UIDocumentPickerViewController {
        let controller = UIDocumentPickerViewController(forExporting: items, asCopy: true)
        controller.delegate = context.coordinator
        return controller
    }

    func updateUIViewController(_ uiViewController: UIDocumentPickerViewController, context: Context) {}

    final class Coordinator: NSObject, UIDocumentPickerDelegate {
        private let onComplete: () -> Void

        init(onComplete: @escaping () -> Void) {
            self.onComplete = onComplete
        }

        func documentPicker(_ controller: UIDocumentPickerViewController, didPickDocumentsAt urls: [URL]) {
            onComplete()
        }

        func documentPickerWasCancelled(_ controller: UIDocumentPickerViewController) {
            onComplete()
        }
    }
}

private struct StyleChipsRow: View {
    let chips: [StyleChipUi]
    let onSelect: (NoteStyleRef) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            SectionLabel(String(localized: "Writing style"))
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(chips, id: \.self) { chip in
                        Chip(text: label(for: chip), isSelected: chip.isCurrent) {
                            if !chip.isCurrent { onSelect(chip.style) }
                        }
                    }
                }
                .padding(.horizontal, OffhandSpacing.screen)
            }
            .padding(.horizontal, -OffhandSpacing.screen)
        }
    }

    private func label(for chip: StyleChipUi) -> String {
        if let name = chip.customName { return name }
        return chip.style.builtInPreset.map { NoteStyleLabels.label(for: $0) } ?? ""
    }
}
