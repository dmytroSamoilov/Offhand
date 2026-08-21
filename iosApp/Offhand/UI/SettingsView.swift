import OffhandShared
import SwiftUI

struct SettingsView: View {
    private let viewModel = AppViewModels.settings
    @Environment(\.scenePhase) private var scenePhase
    @State private var state = SettingsUiState(
        notePreset: .summary,
        isDynamicColorEnabled: false,
        isAppLockEnabled: false,
        isDeviceSecure: false
    )
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
                    Toggle(String(localized: "Require unlock to open Offhand"), isOn: Binding(
                        get: { state.isAppLockEnabled && state.isDeviceSecure },
                        set: { viewModel.onAppLockChanged(enabled: $0) }
                    ))
                    .disabled(!state.isDeviceSecure)
                } header: {
                    Text(String(localized: "Security"))
                } footer: {
                    Text(
                        state.isDeviceSecure
                            ? String(localized: "Ask for Face ID, Touch ID, or your passcode every time Offhand opens.")
                            : String(localized: "Set a passcode on this iPhone to use this.")
                    )
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
        .onChange(of: scenePhase) {
            // A passcode can be added or removed while this screen is backgrounded.
            if scenePhase == .active { viewModel.onScreenShown() }
        }
        .task {
            viewModel.onScreenShown()
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
