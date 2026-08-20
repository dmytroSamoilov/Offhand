import OffhandShared
import SwiftUI

struct SettingsView: View {
    private let viewModel = AppViewModels.settings
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
                                .foregroundStyle(.primary)
                            Spacer()
                            Text(presetLabel(state.notePreset))
                                .foregroundStyle(.secondary)
                        }
                    }
                }
                Section {
                    NavigationLink {
                        AboutSupportView()
                    } label: {
                        VStack(alignment: .leading, spacing: 2) {
                            Text(String(localized: "About & Support"))
                            Text(
                                String(
                                    format: String(localized: "Version %@ · feedback, privacy and legal"),
                                    appVersion
                                )
                            )
                            .font(.caption)
                            .foregroundStyle(.secondary)
                        }
                    }
                } header: {
                    Text(String(localized: "About"))
                } footer: {
                    Text(String(localized: "Offhand keeps every recording and note on this device. Nothing is uploaded anywhere."))
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
