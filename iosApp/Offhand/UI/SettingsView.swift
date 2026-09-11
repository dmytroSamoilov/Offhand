import OffhandShared
import SwiftUI
import UniformTypeIdentifiers

struct SettingsView: View {
    private let viewModel = AppViewModels.settings
    @Environment(\.scenePhase) private var scenePhase
    @State private var state = SettingsUiState(
        noteStyle: NoteStyleRefBuiltIn(preset: .summary),
        customStyles: [],
        isCustomStylesUnlocked: false,
        isDynamicColorEnabled: false,
        isAppLockEnabled: false,
        isDeviceSecure: false,
        isAudioImportUnlocked: false,
        isSmartSuggestionsEnabled: false,
        isSmartSuggestionsUnlocked: false,
        pro: ProStatusUiFree.shared,
        proOverride: nil,
        isImportPickerRequested: false,
        importNotice: nil
    )
    @State private var isPresetPickerVisible = false
    @State private var isAudioImporterPresented = false

    var body: some View {
        NavigationStack {
            Form {
                proSection
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
                    smartSuggestionsToggle
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
                    Button {
                        viewModel.onImportAudioClicked()
                    } label: {
                        HStack {
                            VStack(alignment: .leading, spacing: 2) {
                                Text(String(localized: "Import audio")).foregroundStyle(.primary)
                                Text(String(localized: "Turn audio files into notes. Pick one or several at once"))
                                    .font(.caption)
                                    .foregroundStyle(.secondary)
                            }
                            if !state.isAudioImportUnlocked {
                                Spacer()
                                ProCrown()
                            }
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
                #if DEBUG
                if let override = state.proOverride {
                    proOverrideSection(override)
                }
                #endif
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
                                CustomStyleCard(
                                    style: style,
                                    isSelected: state.noteStyle.customId == style.id,
                                    showProCrown: !state.isCustomStylesUnlocked
                                ) {
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
        .onChange(of: state.isImportPickerRequested) {
            guard state.isImportPickerRequested else { return }
            viewModel.onImportPickerOpened()
            isAudioImporterPresented = true
        }
        .fileImporter(
            isPresented: $isAudioImporterPresented,
            allowedContentTypes: [.audio],
            allowsMultipleSelection: true
        ) { result in
            guard case .success(let urls) = result else { return }
            let staged = urls.map(stageAudio)
            viewModel.onAudioImportSelected(
                sources: staged.compactMap { $0 },
                unreadableCount: Int32(staged.filter { $0 == nil }.count)
            )
        }
        .alert(importNoticeTitle, isPresented: importNoticeBinding) {
            Button(String(localized: "OK")) { viewModel.onImportNoticeDismissed() }
        } message: {
            if let notice = state.importNotice {
                Text(importNoticeMessage(notice))
            }
        }
        .task {
            viewModel.onScreenShown()
            for await newState in viewModel.uiState {
                state = newState
            }
        }
    }

    // The picker's grant is tied to this view, while decoding runs later on
    // the session's scope, so the file is copied into tmp first; the decoder
    // deletes the copy when it is done.
    private func stageAudio(_ url: URL) -> AudioImportSource? {
        let accessed = url.startAccessingSecurityScopedResource()
        defer { if accessed { url.stopAccessingSecurityScopedResource() } }
        let directory = FileManager.default.temporaryDirectory.appendingPathComponent("imports", isDirectory: true)
        try? FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        let copy = directory.appendingPathComponent(UUID().uuidString).appendingPathExtension(url.pathExtension)
        guard (try? FileManager.default.copyItem(at: url, to: copy)) != nil else { return nil }
        return AudioImportSource(handle: copy.path, displayName: url.lastPathComponent)
    }

    private var importNoticeBinding: Binding<Bool> {
        Binding(
            get: { state.importNotice != nil },
            set: { isShown in if !isShown { viewModel.onImportNoticeDismissed() } }
        )
    }

    private var importNoticeTitle: String {
        if case .started = onEnum(of: state.importNotice) {
            return String(localized: "Import started")
        }
        return String(localized: "Import audio")
    }

    private func importNoticeMessage(_ notice: ImportNoticeUi) -> String {
        switch onEnum(of: notice) {
        case .started(let started):
            return String.localizedStringWithFormat(
                String(localized: "%d files are being imported. The notes are created in the background and will show up in your notes list as soon as they are ready."),
                started.fileCount
            )
        case .unreadable:
            return String(localized: "Some of the files could not be read.")
        }
    }

    private var appVersion: String {
        Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "1.0"
    }

    // Free users get the upgrade card on top; subscribers get the status and
    // the store's own management page, where cancellation lives.
    @ViewBuilder
    private var proSection: some View {
        if case .free = onEnum(of: state.pro) {
            Section {
                HStack(spacing: 12) {
                    ProCrown(size: 28)
                    VStack(alignment: .leading, spacing: 2) {
                        Text(String(localized: "Offhand Pro")).font(.headline)
                        Text(String(localized: "Custom styles, PDF and Word export, smart suggestions and audio import"))
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                    Spacer()
                    Button(String(localized: "Upgrade")) { viewModel.onUpgradeClicked() }
                        .buttonStyle(.borderedProminent)
                }
                .padding(.vertical, 4)
            }
            .listRowBackground(Brand.primaryContainer)
        } else {
            Section(String(localized: "Subscription")) {
                HStack(spacing: 12) {
                    ProCrown(size: 24)
                    VStack(alignment: .leading, spacing: 2) {
                        Text(String(localized: "Offhand Pro"))
                        Text(proStatusLabel).font(.caption).foregroundStyle(.secondary)
                    }
                }
                if case .lifetime = onEnum(of: state.pro) {} else {
                    Link(String(localized: "Manage subscription"), destination: URL(string: "https://apps.apple.com/account/subscriptions")!)
                }
            }
        }
    }

    private var proStatusLabel: String {
        switch onEnum(of: state.pro) {
        case .free: return ""
        case .lifetime: return String(localized: "Lifetime, yours forever")
        case .trial(let trial):
            guard let endsAt = trial.endsAtMs else { return String(localized: "Free trial") }
            return String(format: String(localized: "Free trial until %@"), formatDate(endsAt.int64Value))
        case .yearly(let yearly):
            guard let renewsAt = yearly.renewsAtMs else { return String(localized: "Yearly") }
            return String(format: String(localized: "Yearly, renews on %@"), formatDate(renewsAt.int64Value))
        }
    }

    private func formatDate(_ epochMs: Int64) -> String {
        Date(timeIntervalSince1970: TimeInterval(epochMs) / 1000).formatted(date: .abbreviated, time: .omitted)
    }

    // The whole row flips the switch, like the Android row; a plain Toggle
    // only reacts to the switch itself.
    private var smartSuggestionsToggle: some View {
        Button {
            viewModel.onSmartSuggestionsChanged(enabled: !state.isSmartSuggestionsEnabled)
        } label: {
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text(String(localized: "Show smart suggestions")).foregroundStyle(.primary)
                    Text(String(localized: "Find calendar events and to-dos in every new note. Adds a few seconds of processing."))
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
                Spacer()
                if !state.isSmartSuggestionsUnlocked { ProCrown() }
                Toggle("", isOn: .constant(state.isSmartSuggestionsEnabled))
                    .labelsHidden()
                    .allowsHitTesting(false)
            }
        }
        .buttonStyle(.plain)
    }

    #if DEBUG
    private func proOverrideSection(_ override: ProOverride) -> some View {
        Section(String(localized: "Developer")) {
            VStack(alignment: .leading, spacing: 8) {
                Text(String(localized: "Pro status override"))
                Picker(String(localized: "Pro status override"), selection: Binding(
                    get: { override },
                    set: { viewModel.onProOverrideSelected(override: $0) }
                )) {
                    Text("Store").tag(ProOverride.store)
                    Text("Free").tag(ProOverride.free)
                    Text("Pro").tag(ProOverride.pro)
                }
                .pickerStyle(.segmented)
                .labelsHidden()
            }
        }
    }
    #endif
}

private struct CustomStyleCard: View {
    let style: CustomStyleOptionUi
    let isSelected: Bool
    var showProCrown = false
    let onSelect: () -> Void

    var body: some View {
        PresetCard(
            title: style.name,
            details: style.description_,
            symbol: NoteStyleLabels.customSymbol,
            isSelected: isSelected,
            showProCrown: showProCrown,
            action: onSelect
        )
    }
}
