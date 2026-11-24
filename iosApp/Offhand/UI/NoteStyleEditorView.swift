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
        isLocked: false,
        isDeleteRequested: false,
        isDeleted: false
    )

    private var viewModel: NoteStyleEditorViewModel {
        handle?.viewModel ?? openHandle().viewModel
    }

    var body: some View {
        ScrollView {
            if handle != nil {
                editorContent
                    .padding(.horizontal, OffhandSpacing.screen)
                    .padding(.top, 12)
                    .padding(.bottom, OffhandSpacing.screen)
            }
        }
        .scrollDismissesKeyboard(.immediately)
        .background(OffhandColor.ground.ignoresSafeArea())
        .navigationTitle(state.isNew ? String(localized: "New writing style") : String(localized: "Edit writing style"))
        .navigationBarTitleDisplayMode(.inline)
        .navigationBarBackButtonHidden(true)
        .toolbar {
            ToolbarItem(placement: .cancellationAction) {
                Button(String(localized: "Cancel")) { dismiss() }
                    .tint(OffhandColor.ink)
            }
            ToolbarItem(placement: .confirmationAction) {
                Button(String(localized: "Save")) { viewModel.onSaveRequested() }
                    .tint(OffhandColor.accent)
            }
        }
        .onChange(of: state.isSaved) {
            if state.isSaved { dismiss() }
        }
        .onChange(of: state.isDeleted) {
            if state.isDeleted { dismiss() }
        }
        .offhandDialog(isPresented: deleteBinding) {
            OffhandDialog(
                title: String(localized: "Delete this style?"),
                message: String(localized: "Notes written with it are kept. They switch to the Summary style the next time they are rewritten."),
                confirmLabel: String(localized: "Cancel"),
                onConfirm: { viewModel.onDeleteDismissed() },
                destructiveLabel: String(localized: "Delete"),
                onDestructive: { viewModel.onDeleteConfirmed() },
                onDismiss: { viewModel.onDeleteDismissed() }
            )
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

    private var deleteBinding: Binding<Bool> {
        Binding(
            get: { state.isDeleteRequested },
            set: { isShown in if !isShown { viewModel.onDeleteDismissed() } }
        )
    }

    private func openHandle() -> NoteStyleEditorHandle {
        if let handle { return handle }
        let opened = SharedGraph.shared.noteStyleEditor(styleId: styleId)
        handle = opened
        return opened
    }

    private var editorContent: some View {
        VStack(alignment: .leading, spacing: 0) {
            DescribeStyleCard(viewModel: viewModel, describe: state.describe)
            fieldLabel(String(localized: "Name")).padding(.top, 20)
            OffhandTextField(
                text: Binding(get: { state.name }, set: { viewModel.onNameChanged(name: $0) }),
                placeholder: String(localized: "Name"),
                error: errorMessage(state.errors.name, maxLength: NoteStyleLimits.shared.MAX_NAME_LENGTH)
            )
            .padding(.top, 6)
            OffhandTextField(
                text: Binding(get: { state.noteKind }, set: { viewModel.onNoteKindChanged(noteKind: $0) }),
                placeholder: String(localized: "What kind of note is this?"),
                error: errorMessage(state.errors.noteKind, maxLength: NoteStyleLimits.shared.MAX_KIND_LENGTH)
            )
            .padding(.top, 12)
            hint(String(localized: "Optional. Finishes the sentence “The note is …”."))
            fieldLabel(String(localized: "Language of the note")).padding(.top, 20)
            SegmentedControl(
                options: [String(localized: "Same as the recording"), String(localized: "Always English")],
                selectedIndex: state.language == .english ? 1 : 0,
                isCompact: true
            ) { viewModel.onLanguageChanged(language: $0 == 1 ? .english : .recording) }
            .padding(.top, 6)
            SectionLabel(String(localized: "Sections"))
                .padding(.top, 22)
                .padding(.bottom, 10)
            VStack(spacing: 10) {
                ForEach(Array(state.sections.enumerated()), id: \.offset) { index, section in
                    sectionCard(index: index, section: section)
                }
            }
            if state.sections.isEmpty {
                hint(String(localized: "Without sections the note is one free-form summary."))
            }
            if state.errors.sections != nil {
                Text(String(format: String(localized: "Use at most %d sections"), Int(NoteStyleLimits.shared.MAX_SECTIONS)))
                    .offhandText(.caption)
                    .foregroundStyle(OffhandColor.danger)
                    .padding(.top, 8)
            }
            PlainTextButton(String(localized: "Add section"), systemImage: "plus") { viewModel.onSectionAdded() }
                .disabled(state.sections.count >= Int(NoteStyleLimits.shared.MAX_SECTIONS))
                .padding(.leading, -OffhandSpacing.controls)
                .padding(.top, 4)
            if !state.isNew {
                PlainTextButton(String(localized: "Delete style"), tone: .danger) { viewModel.onDeleteRequested() }
                    .padding(.leading, -OffhandSpacing.controls)
                    .padding(.top, OffhandSpacing.screen)
            }
        }
    }

    private func sectionCard(index: Int, section: SectionDraftUi) -> some View {
        SurfaceCard {
            VStack(alignment: .leading, spacing: 12) {
                sectionHeader(index: index)
                OffhandTextField(
                    text: Binding(
                        get: { section.heading },
                        set: { viewModel.onSectionHeadingChanged(index: Int32(index), heading: $0) }
                    ),
                    placeholder: String(localized: "Heading"),
                    error: errorMessage(
                        state.errors.headings[KotlinInt(int: Int32(index))],
                        maxLength: NoteStyleLimits.shared.MAX_HEADING_LENGTH
                    )
                )
                SegmentedControl(
                    options: SectionFormatChoice.all.map(\.label),
                    selectedIndex: SectionFormatChoice.all.firstIndex { $0.format == section.format } ?? 0,
                    isCompact: true
                ) { viewModel.onSectionFormatChanged(index: Int32(index), format: SectionFormatChoice.all[$0].format) }
                OffhandTextField(
                    text: Binding(
                        get: { section.guidance },
                        set: { viewModel.onSectionGuidanceChanged(index: Int32(index), guidance: $0) }
                    ),
                    placeholder: String(localized: "What goes here"),
                    isMultiline: true,
                    minLines: 2
                )
                .lineLimit(2...Int(NoteStyleLimits.shared.GUIDANCE_VISIBLE_LINES))
                if section.guidance.count > Int(NoteStyleLimits.shared.GUIDANCE_WARNING_LENGTH) {
                    Text(String(localized: "Long instructions may fail to process. The on-device AI has limited memory, so use them at your own risk."))
                        .offhandText(.caption)
                        .foregroundStyle(OffhandColor.ink2)
                }
            }
            .padding(OffhandSpacing.card)
        }
    }

    private func sectionHeader(index: Int) -> some View {
        HStack(spacing: 0) {
            SectionLabel(String(format: String(localized: "Section %d"), index + 1))
                .frame(maxWidth: .infinity, alignment: .leading)
            BarIconButton(systemImage: "chevron.up", label: String(localized: "Move up"), tint: OffhandColor.ink2) {
                viewModel.onSectionMoved(from: Int32(index), to: Int32(index - 1))
            }
            .disabled(index == 0)
            BarIconButton(systemImage: "chevron.down", label: String(localized: "Move down"), tint: OffhandColor.ink2) {
                viewModel.onSectionMoved(from: Int32(index), to: Int32(index + 1))
            }
            .disabled(index == state.sections.count - 1)
            BarIconButton(systemImage: "xmark", label: String(localized: "Remove section"), tint: OffhandColor.ink2) {
                viewModel.onSectionRemoved(index: Int32(index))
            }
        }
        .padding(.trailing, -10)
        .padding(.vertical, -6)
    }

    private func fieldLabel(_ text: String) -> some View {
        Text(text)
            .offhandText(.caption, face: .interSemibold)
            .foregroundStyle(OffhandColor.ink)
            .accessibilityHidden(true)
    }

    private func hint(_ text: String) -> some View {
        Text(text)
            .offhandText(.caption)
            .foregroundStyle(OffhandColor.ink2)
            .padding(.top, 8)
    }

    private func errorMessage(_ error: NoteStyleFieldError?, maxLength: Int32) -> String? {
        guard let error else { return nil }
        switch error {
        case .blank: return String(localized: "This field is required")
        case .tooLong: return String(format: String(localized: "Use at most %d characters"), Int(maxLength))
        case .duplicate: return String(localized: "This name is already used")
        default: return nil
        }
    }

}

private struct SectionFormatChoice {
    let label: String
    let format: SectionFormat

    static var all: [SectionFormatChoice] {
        [
            SectionFormatChoice(label: String(localized: "Short"), format: .sentences),
            SectionFormatChoice(label: String(localized: "Bullet points"), format: .bullets),
            SectionFormatChoice(label: String(localized: "Free"), format: .free),
        ]
    }
}

private struct DescribeStyleCard: View {
    let viewModel: NoteStyleEditorViewModel
    let describe: DescribeStyleUi?

    private var description: String { describe?.description_ ?? "" }
    private var isRunning: Bool { describe?.status == .running }

    var body: some View {
        SurfaceCard {
            VStack(alignment: .leading, spacing: 10) {
                Text(String(localized: "Describe it and let the AI fill the form"))
                    .offhandText(.caption, face: .interSemibold)
                    .foregroundStyle(OffhandColor.ink)
                OffhandTextField(
                    text: Binding(
                        get: { description },
                        set: { viewModel.onDescriptionChanged(description: String($0.prefix(Int(NoteStyleLimits.shared.MAX_DESCRIPTION_LENGTH)))) }
                    ),
                    placeholder: String(localized: "For example: I coach football teams. After each training I record what we practised, what went well, what each player should work on and what to plan next time."),
                    isMultiline: true,
                    minLines: 2,
                    isFilledWithGround: true
                )
                .disabled(isRunning)
                HStack(spacing: 12) {
                    Button(String(localized: "Fill the form")) { viewModel.onDraftRequested() }
                        .buttonStyle(OutlinedPillStyle())
                        .disabled(isRunning || description.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                    if isRunning {
                        ProgressView().tint(OffhandColor.ink2)
                    }
                }
                status
            }
            .padding(OffhandSpacing.card)
        }
    }

    @ViewBuilder
    private var status: some View {
        switch describe?.status ?? .idle {
        case .running:
            Text(String(localized: "Thinking about your sections…"))
                .offhandText(.caption)
                .foregroundStyle(OffhandColor.ink2)
        case .modelUnavailable:
            Text(String(localized: "Set up the on-device AI first to build a style."))
                .offhandText(.caption)
                .foregroundStyle(OffhandColor.danger)
        case .failed:
            Text(String(localized: "The AI could not turn that into a style. Try describing it differently."))
                .offhandText(.caption)
                .foregroundStyle(OffhandColor.danger)
        default:
            EmptyView()
        }
    }
}

private struct OutlinedPillStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        OutlinedPillBody(configuration: configuration)
    }
}

private struct OutlinedPillBody: View {
    let configuration: ButtonStyleConfiguration
    @Environment(\.isEnabled) private var isEnabled

    var body: some View {
        configuration.label
            .offhandText(.bodySmall, face: .interSemibold)
            .foregroundStyle(OffhandColor.ink)
            .padding(.horizontal, 18)
            .frame(minHeight: OffhandSpacing.touchTarget)
            .overlay { Capsule().strokeBorder(OffhandColor.border, lineWidth: 1) }
            .contentShape(Capsule())
            .opacity(isEnabled ? (configuration.isPressed ? KitAlpha.pressed : 1) : KitAlpha.disabled)
    }
}
