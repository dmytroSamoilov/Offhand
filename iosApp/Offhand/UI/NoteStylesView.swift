import OffhandShared
import SwiftUI

struct NoteStylesView: View {
    private let viewModel = AppViewModels.noteStyles
    @State private var state = NoteStylesUiState(customStyles: [], isUnlocked: false, pendingDeleteId: nil)

    var body: some View {
        List {
            Section(String(localized: "Built in")) {
                ForEach([NotePreset.summary, .meeting, .visit, .legal], id: \.self) { preset in
                    Label {
                        VStack(alignment: .leading, spacing: 2) {
                            Text(NoteStyleLabels.label(for: preset))
                            Text(NoteStyleLabels.details(for: preset))
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        }
                    } icon: {
                        Image(systemName: NoteStyleLabels.symbol(for: preset)).foregroundStyle(Brand.primary)
                    }
                }
            }
            Section(String(localized: "Your styles")) {
                if state.customStyles.isEmpty {
                    Text(state.isUnlocked
                        ? String(localized: "No custom styles yet. Create one to give the AI your own headings.")
                        : String(localized: "Custom styles are part of Offhand Pro."))
                        .foregroundStyle(.secondary)
                }
                ForEach(state.customStyles, id: \.id) { style in
                    NavigationLink {
                        NoteStyleEditorView(styleId: style.id)
                    } label: {
                        Label {
                            VStack(alignment: .leading, spacing: 2) {
                                Text(style.name)
                                Text(style.description_)
                                    .font(.caption)
                                    .foregroundStyle(.secondary)
                            }
                        } icon: {
                            Image(systemName: NoteStyleLabels.customSymbol).foregroundStyle(Brand.primary)
                        }
                    }
                    .swipeActions(edge: .trailing, allowsFullSwipe: false) {
                        Button(role: .destructive) {
                            Haptics.confirm()
                            viewModel.onDeleteRequested(id: style.id)
                        } label: {
                            Label(String(localized: "Delete style"), systemImage: "trash")
                        }
                    }
                }
            }
        }
        .navigationTitle(String(localized: "Note styles"))
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            if state.isUnlocked {
                ToolbarItem(placement: .primaryAction) {
                    NavigationLink {
                        NoteStyleEditorView(styleId: 0)
                    } label: {
                        Label(String(localized: "New style"), systemImage: "plus")
                    }
                }
            }
        }
        .alert(String(localized: "Delete this style?"), isPresented: deleteBinding) {
            Button(String(localized: "Delete"), role: .destructive) { viewModel.onDeleteConfirmed() }
            Button(String(localized: "Cancel"), role: .cancel) { viewModel.onDeleteDismissed() }
        } message: {
            Text(String(localized: "Notes written with it are kept. They switch to the Summary style the next time they are rewritten."))
        }
        .task {
            for await newState in viewModel.uiState {
                state = newState
            }
        }
    }

    private var deleteBinding: Binding<Bool> {
        Binding(
            get: { state.pendingDeleteId != nil },
            set: { isShown in if !isShown { viewModel.onDeleteDismissed() } }
        )
    }
}
