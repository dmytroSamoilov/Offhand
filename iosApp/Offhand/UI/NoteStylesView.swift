import OffhandShared
import SwiftUI

struct NoteStylesView: View {
    private let viewModel = AppViewModels.noteStyles
    @State private var state = NoteStylesUiState(
        customStyles: [],
        isUnlocked: false,
        selected: NoteStyleRefBuiltIn(preset: .summary)
    )
    @State private var editorStyleId: Int64 = 0
    @State private var isEditorVisible = false

    var body: some View {
        NoteStyleList(
            current: state.selected,
            customStyles: state.customStyles.map { NoteStyleChoice(id: $0.id, name: $0.name, details: $0.description_) },
            isProStylesUnlocked: state.isUnlocked,
            onSelect: { viewModel.onStyleSelected(style: $0) },
            onCreateStyle: { openEditor(styleId: 0) },
            onEdit: { openEditor(styleId: $0) },
            title: String(localized: "Writing styles"),
            subtitle: String(localized: "A style decides what Offhand writes from your recording. Tap one to use it for new notes.")
        )
        .navigationBarTitleDisplayMode(.inline)
        .navigationDestination(isPresented: $isEditorVisible) {
            NoteStyleEditorView(styleId: editorStyleId)
        }
        .task {
            for await newState in viewModel.uiState {
                state = newState
            }
        }
    }

    private func openEditor(styleId: Int64) {
        editorStyleId = styleId
        isEditorVisible = true
    }
}
