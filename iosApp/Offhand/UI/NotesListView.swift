import OffhandShared
import SwiftUI

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
        modelPreparation: nil
    )
    @State private var isRecordSheetVisible = false

    var body: some View {
        NavigationStack {
            List {
                ForEach(state.sections, id: \.self) { section in
                    Section(dayTitle(section.dayLabel)) {
                        ForEach(section.notes, id: \.id) { note in
                            Button {
                                viewModel.onNoteSelected(id: note.id)
                            } label: {
                                NoteCardRow(note: note, progress: state.noteProgress[KotlinLong(value: note.id)]?.intValue)
                            }
                            .buttonStyle(.plain)
                            .swipeActions(edge: .trailing, allowsFullSwipe: false) {
                                Button(role: .destructive) {
                                    viewModel.onDeleteRequested(id: note.id)
                                } label: {
                                    Label(String(localized: "Delete"), systemImage: "trash")
                                }
                            }
                        }
                    }
                }
            }
            .listStyle(.insetGrouped)
            .navigationTitle(String(localized: "Notes"))
            .overlay(alignment: .center) {
                if state.sections.isEmpty {
                    ContentUnavailableView(
                        String(localized: "No notes yet"),
                        systemImage: "mic",
                        description: Text(String(localized: "Tap the microphone to record your first note."))
                    )
                }
            }
            .overlay(alignment: .bottomTrailing) {
                recordButton
            }
            .navigationDestination(isPresented: detailBinding) {
                if let detail = state.selected {
                    NoteDetailView(viewModel: viewModel, detail: detail, state: state)
                }
            }
        }
        .sheet(isPresented: $isRecordSheetVisible) {
            RecordSheetView(autoStart: true)
        }
        .confirmationDialog(
            String(localized: "Delete this note?"),
            isPresented: deleteBinding,
            titleVisibility: .visible
        ) {
            Button(String(localized: "Delete"), role: .destructive) { viewModel.onDeleteConfirmed() }
            Button(String(localized: "Cancel"), role: .cancel) { viewModel.onDeleteDismissed() }
        }
        .task {
            for await newState in viewModel.uiState {
                state = newState
            }
        }
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

private struct NoteCardRow: View {
    let note: NoteCardUi
    let progress: Int?

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(note.title)
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
                Text(note.preview)
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                    .lineLimit(2)
            }
            HStack(spacing: 12) {
                Text(note.time)
                if let duration = note.durationText {
                    Label(duration, systemImage: "waveform")
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
}
