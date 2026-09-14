import OffhandShared
import SwiftUI

struct NoteStyleEditorView: View {
    let styleId: Int64
    @Environment(\.dismiss) private var dismiss
    @State private var handle: NoteStyleEditorHandle?
    @State private var state = NoteStyleEditorUiState(
        isNew: true,
        name: "",
        noteKind: "",
        language: .recording,
        sections: [],
        errors: NoteStyleErrors(name: nil, noteKind: nil, sections: nil, headings: [:]),
        describe: nil,
        isSaved: false,
        isLocked: false
    )

    // The ViewModel is created on first appearance, not in init: SwiftUI builds
    // NavigationLink destinations eagerly and rebuilds the struct freely, and
    // each build would otherwise start another ViewModel with its own coroutines.
    private var viewModel: NoteStyleEditorViewModel {
        handle?.viewModel ?? openHandle().viewModel
    }

    var body: some View {
        Form {
            if handle != nil {
                editorContent
            }
        }
        .scrollDismissesKeyboard(.immediately)
        .navigationTitle(state.isNew ? String(localized: "New note style") : String(localized: "Edit note style"))
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .confirmationAction) {
                Button(String(localized: "Save")) { viewModel.onSaveRequested() }
            }
        }
        .sheet(isPresented: describeBinding) {
            if let describe = state.describe {
                DescribeStyleSheet(viewModel: viewModel, describe: describe)
            }
        }
        .onChange(of: state.isSaved) {
            if state.isSaved { dismiss() }
        }
        .onDisappear {
            handle?.close()
            handle = nil
        }
        .task {
            let editor = openHandle()
            for await newState in editor.viewModel.uiState {
                state = newState
            }
        }
    }

    private func openHandle() -> NoteStyleEditorHandle {
        if let handle { return handle }
        let opened = SharedGraph.shared.noteStyleEditor(styleId: styleId)
        handle = opened
        return opened
    }

    @ViewBuilder
    private var editorContent: some View {
        Section {
            Button {
                viewModel.onDescribeRequested()
            } label: {
                HStack(spacing: 8) {
                    Image(systemName: "sparkles")
                    Text(String(localized: "Describe it and let the AI fill the form"))
                }
                .font(.headline)
                .foregroundStyle(.white)
                .frame(maxWidth: .infinity, minHeight: 32)
            }
            .buttonStyle(.borderedProminent)
            .listRowInsets(EdgeInsets())
            .listRowBackground(Color.clear)
        }
        Section {
            TextField(String(localized: "Name"), text: Binding(
                get: { state.name },
                set: { viewModel.onNameChanged(name: $0) }
            ))
            errorText(state.errors.name, maxLength: NoteStyleLimits.shared.MAX_NAME_LENGTH)
            TextField(String(localized: "What kind of note is this?"), text: Binding(
                get: { state.noteKind },
                set: { viewModel.onNoteKindChanged(noteKind: $0) }
            ))
            errorText(state.errors.noteKind, maxLength: NoteStyleLimits.shared.MAX_KIND_LENGTH)
        } footer: {
            Text(String(localized: "Optional. Finishes the sentence “The note is …”."))
        }
        Section(String(localized: "Language of the note")) {
            Picker("", selection: Binding(
                get: { state.language },
                set: { viewModel.onLanguageChanged(language: $0) }
            )) {
                Text(String(localized: "Same as the recording")).tag(NoteStyleLanguage.recording)
                Text(String(localized: "Always English")).tag(NoteStyleLanguage.english)
            }
            .pickerStyle(.segmented)
        }
        ForEach(Array(state.sections.enumerated()), id: \.offset) { index, section in
            sectionEditor(index: index, section: section)
        }
        Section {
            if state.sections.isEmpty {
                Text(String(localized: "Without sections the note is one free-form summary."))
                    .font(.footnote)
                    .foregroundStyle(.secondary)
            }
            sectionsErrorText
            Button {
                viewModel.onSectionAdded()
            } label: {
                Label(String(localized: "Add section"), systemImage: "plus")
            }
            .disabled(state.sections.count >= Int(NoteStyleLimits.shared.MAX_SECTIONS))
        }
    }

    private func sectionEditor(index: Int, section: SectionDraftUi) -> some View {
        Section {
            TextField(String(localized: "Heading"), text: Binding(
                get: { section.heading },
                set: { viewModel.onSectionHeadingChanged(index: Int32(index), heading: $0) }
            ))
            errorText(state.errors.headings[KotlinInt(int: Int32(index))], maxLength: NoteStyleLimits.shared.MAX_HEADING_LENGTH)
            TextField(String(localized: "What goes here"), text: Binding(
                get: { section.guidance },
                set: { viewModel.onSectionGuidanceChanged(index: Int32(index), guidance: $0) }
            ), axis: .vertical)
            .lineLimit(2...Int(NoteStyleLimits.shared.GUIDANCE_VISIBLE_LINES))
            if section.guidance.count > Int(NoteStyleLimits.shared.GUIDANCE_WARNING_LENGTH) {
                Text(String(localized: "Long instructions may fail to process. The on-device AI has limited memory, so use them at your own risk."))
                    .font(.footnote)
                    .foregroundStyle(Brand.onWarningContainer)
                    .padding(8)
                    .background(Brand.warningContainer, in: RoundedRectangle(cornerRadius: 8))
            }
            Picker("", selection: Binding(
                get: { section.format },
                set: { viewModel.onSectionFormatChanged(index: Int32(index), format: $0) }
            )) {
                Text(String(localized: "Short")).tag(SectionFormat.sentences)
                Text(String(localized: "Bullet points")).tag(SectionFormat.bullets)
                Text(String(localized: "Free")).tag(SectionFormat.free)
            }
            .pickerStyle(.segmented)
        } header: {
            sectionHeader(index: index)
        }
    }

    private func sectionHeader(index: Int) -> some View {
        HStack {
            Text(String(format: String(localized: "Section %d"), index + 1))
            Spacer()
            Button { viewModel.onSectionMoved(from: Int32(index), to: Int32(index - 1)) } label: {
                Image(systemName: "chevron.up")
            }
            .disabled(index == 0)
            .accessibilityLabel(String(localized: "Move up"))
            Button { viewModel.onSectionMoved(from: Int32(index), to: Int32(index + 1)) } label: {
                Image(systemName: "chevron.down")
            }
            .disabled(index == state.sections.count - 1)
            .accessibilityLabel(String(localized: "Move down"))
            Button { viewModel.onSectionRemoved(index: Int32(index)) } label: {
                Image(systemName: "xmark")
            }
            .accessibilityLabel(String(localized: "Remove section"))
        }
        .buttonStyle(.borderless)
        .textCase(nil)
    }

    @ViewBuilder
    private var sectionsErrorText: some View {
        if state.errors.sections != nil {
            Text(String(format: String(localized: "Use at most %d sections"), Int(NoteStyleLimits.shared.MAX_SECTIONS)))
                .font(.footnote)
                .foregroundStyle(.red)
        }
    }

    @ViewBuilder
    private func errorText(_ error: NoteStyleFieldError?, maxLength: Int32) -> some View {
        if let error {
            Text(errorMessage(error, maxLength: Int(maxLength)))
                .font(.footnote)
                .foregroundStyle(.red)
        }
    }

    private func errorMessage(_ error: NoteStyleFieldError, maxLength: Int) -> String {
        switch error {
        case .blank: return String(localized: "This field is required")
        case .tooLong: return String(format: String(localized: "Use at most %d characters"), maxLength)
        case .duplicate: return String(localized: "This name is already used")
        default: return ""
        }
    }

    private var describeBinding: Binding<Bool> {
        Binding(
            get: { state.describe != nil },
            set: { isShown in if !isShown { viewModel.onDescribeDismissed() } }
        )
    }
}

private struct DescribeStyleSheet: View {
    let viewModel: NoteStyleEditorViewModel
    let describe: DescribeStyleUi

    private var isRunning: Bool { describe.status == .running }

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    ZStack(alignment: .topLeading) {
                        if describe.description_.isEmpty {
                            Text(String(localized: "For example: I coach football teams. After each training I record what we practised, what went well, what each player should work on and what to plan next time."))
                                .foregroundStyle(Color(.placeholderText))
                                .padding(.top, 8)
                                .padding(.leading, 4)
                                .allowsHitTesting(false)
                        }
                        TextEditor(text: Binding(
                            get: { describe.description_ },
                            set: { viewModel.onDescriptionChanged(description: String($0.prefix(Int(NoteStyleLimits.shared.MAX_DESCRIPTION_LENGTH)))) }
                        ))
                        .frame(minHeight: 120)
                        .disabled(isRunning)
                    }
                } footer: {
                    Text(String(localized: "Say what the recording is about and what the finished note should contain. The AI proposes the sections; you can change everything afterwards."))
                }
                statusSection
            }
            .navigationTitle(String(localized: "Describe your note"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(String(localized: "Cancel")) { viewModel.onDescribeDismissed() }.disabled(isRunning)
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button(String(localized: "Build the style")) { viewModel.onDraftRequested() }
                        .disabled(isRunning || describe.description_.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                }
            }
        }
        .interactiveDismissDisabled(isRunning)
    }

    @ViewBuilder
    private var statusSection: some View {
        switch describe.status {
        case .running:
            Section {
                HStack(spacing: 12) {
                    ProgressView()
                    Text(String(localized: "Thinking about your sections…")).foregroundStyle(.secondary)
                }
            }
        case .modelUnavailable:
            Section { Text(String(localized: "Set up the on-device AI first to build a style.")).foregroundStyle(.red) }
        case .failed:
            Section { Text(String(localized: "The AI could not turn that into a style. Try describing it differently.")).foregroundStyle(.red) }
        default:
            EmptyView()
        }
    }
}
