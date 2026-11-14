import OffhandShared
import SwiftUI

// The same list as the restyle sheet: picking a row sets the default for new
// recordings, custom rows open the editor, where deleting lives.
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
            footer: String(localized: "The selected style is used for new recordings. You can change the style of any note from the note itself."),
            onSelect: { viewModel.onStyleSelected(style: $0) },
            onCreateStyle: { openEditor(styleId: 0) },
            onEdit: { openEditor(styleId: $0) }
        )
        .navigationTitle(String(localized: "Summary styles"))
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
