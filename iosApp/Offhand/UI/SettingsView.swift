import OffhandShared
import SwiftUI

struct SettingsView: View {
    private let viewModel = AppViewModels.settings
    @Environment(\.scenePhase) private var scenePhase
    @State private var state = SettingsUiState(
        noteStyle: NoteStyleRefBuiltIn(preset: .summary),
        customStyles: [],
        isCustomStylesUnlocked: false,
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
                            Text(String(localized: "Default note style"))
                                .foregroundStyle(.primary)
                            Spacer()
                            Text(NoteStyleLabels.label(for: state.noteStyle, customStyles: state.customStyles))
                                .foregroundStyle(.secondary)
                        }
                    }
                    NavigationLink {
                        NoteStylesView()
                    } label: {
                        VStack(alignment: .leading, spacing: 2) {
                            Text(String(localized: "Manage note styles"))
                            Text(String(localized: "Create your own headings and format"))
                                .font(.caption)
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
                        BackupView()
                    } label: {
                        VStack(alignment: .leading, spacing: 2) {
                            Text(String(localized: "Backup & restore"))
                            Text(String(localized: "Move your notes to a new iPhone or keep a copy"))
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        }
                    }
                } header: {
                    Text(String(localized: "Backup"))
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
                    VStack(alignment: .leading, spacing: 10) {
                        NotePresetPicker(selected: state.noteStyle.builtInPreset) { preset in
                            viewModel.onNoteStyleSelected(style: NoteStyleRefBuiltIn(preset: preset))
                            isPresetPickerVisible = false
                        }
                        if !state.customStyles.isEmpty {
                            Text(String(localized: "Your styles"))
                                .font(.footnote.weight(.semibold))
                                .foregroundStyle(.secondary)
                                .padding(.top, 8)
                            ForEach(state.customStyles, id: \.id) { style in
                                CustomStyleCard(style: style, isSelected: state.noteStyle.customId == style.id) {
                                    viewModel.onNoteStyleSelected(style: NoteStyleRefCustom(id: style.id))
                                    isPresetPickerVisible = false
                                }
                            }
                        }
                    }
                    .padding()
                }
                .navigationTitle(String(localized: "Default note style"))
                .navigationBarTitleDisplayMode(.inline)
            }
            .presentationDetents([.medium, .large])
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
}

private struct CustomStyleCard: View {
    let style: CustomStyleOptionUi
    let isSelected: Bool
    let onSelect: () -> Void

    var body: some View {
        PresetCard(
            title: style.name,
            details: style.description_,
            symbol: NoteStyleLabels.customSymbol,
            isSelected: isSelected,
            action: onSelect
        )
    }
}
