import OffhandShared
import SwiftUI

struct SettingsView: View {
    private let viewModel = SharedGraph.shared.settingsViewModel()
    @State private var state = SettingsUiState(notePreset: .summary, isDynamicColorEnabled: false)
    @State private var isPresetPickerVisible = false

    var body: some View {
        NavigationStack {
            Form {
                Section(String(localized: "Notes")) {
                    Button {
                        isPresetPickerVisible = true
                    } label: {
                        HStack {
                            Text(String(localized: "Note style"))
                                .foregroundStyle(Brand.onSurface)
                            Spacer()
                            Text(presetLabel(state.notePreset))
                                .foregroundStyle(Brand.onSurfaceVariant)
                        }
                    }
                }
                Section(String(localized: "About")) {
                    LabeledContent(String(localized: "Version"), value: appVersion)
                    Text(String(localized: "Offhand keeps every recording and note on this device. Nothing is uploaded anywhere."))
                        .font(.footnote)
                        .foregroundStyle(Brand.onSurfaceVariant)
                }
            }
            .navigationTitle(String(localized: "Settings"))
        }
        .sheet(isPresented: $isPresetPickerVisible) {
            NavigationStack {
                ScrollView {
                    NotePresetPicker(selected: state.notePreset) { preset in
                        viewModel.onNotePresetSelected(preset: preset)
                        isPresetPickerVisible = false
                    }
                    .padding()
                }
                .navigationTitle(String(localized: "Note style"))
                .navigationBarTitleDisplayMode(.inline)
            }
            .presentationDetents([.medium])
        }
        .task {
            for await newState in viewModel.uiState {
                state = newState
            }
        }
    }

    private var appVersion: String {
        Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "1.0"
    }

    private func presetLabel(_ preset: NotePreset) -> String {
        switch preset {
        case .summary: return String(localized: "Summary")
        case .meeting: return String(localized: "Meeting")
        case .visit: return String(localized: "Visit")
        case .legal: return String(localized: "File note")
        default: return String(localized: "Summary")
        }
    }
}
