import OffhandShared
import SwiftUI

struct NotesListView: View {
    private let viewModel = SharedGraph.shared.notesViewModel()
    @State private var state = NotesUiState(
        sections: [],
        selected: nil,
        editor: nil,
        playback: AudioPlaybackUi(isAvailable: false, isPlaying: false, progress: 0, positionText: "0:00", durationText: "0:00"),
        isDeleteConfirmationVisible: false,
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
                        }
                    }
                }
            }
            .listStyle(.insetGrouped)
            .scrollContentBackground(.hidden)
            .background(Brand.surface)
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
            .safeAreaInset(edge: .bottom) {
                recordButton
            }
            .navigationDestination(isPresented: detailBinding) {
                if let detail = state.selected {
                    NoteDetailView(viewModel: viewModel, detail: detail, state: state)
                }
            }
        }
        .sheet(isPresented: $isRecordSheetVisible) {
            RecordSheetView()
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

    private var recordButton: some View {
        Button {
            isRecordSheetVisible = true
        } label: {
            Image(systemName: "mic.fill")
                .font(.title2)
                .foregroundStyle(.white)
                .frame(width: 64, height: 64)
                .background(Brand.primary, in: RoundedRectangle(cornerRadius: 22))
        }
        .frame(maxWidth: .infinity, alignment: .trailing)
        .padding(.trailing, 24)
        .padding(.bottom, 8)
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
        HStack(alignment: .top, spacing: 12) {
            Image(systemName: "mic")
                .foregroundStyle(Brand.teal)
                .frame(width: 40, height: 40)
                .background(Brand.tealContainer, in: Circle())
            VStack(alignment: .leading, spacing: 4) {
                Text(note.title)
                    .font(.headline)
                    .lineLimit(1)
                Text(note.time)
                    .font(.caption)
                    .foregroundStyle(Brand.onSurfaceVariant)
                if note.status == .processing {
                    HStack(spacing: 8) {
                        ProgressView()
                        Text(progressText)
                            .font(.caption)
                            .foregroundStyle(Brand.onSurfaceVariant)
                    }
                } else {
                    Text(note.preview)
                        .font(.subheadline)
                        .foregroundStyle(Brand.onSurfaceVariant)
                        .lineLimit(3)
                }
                if let duration = note.durationText {
                    Label(duration, systemImage: "mic")
                        .font(.caption2)
                        .foregroundStyle(Brand.onSurfaceVariant)
                }
            }
        }
        .padding(.vertical, 4)
    }

    private var progressText: String {
        if let progress { return "\(progress)%" }
        return String(localized: "Preparing your note")
    }
}
