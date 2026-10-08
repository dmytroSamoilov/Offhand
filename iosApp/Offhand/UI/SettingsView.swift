import OffhandShared
import SwiftUI
import UniformTypeIdentifiers

struct SettingsView: View {
    private let viewModel = AppViewModels.settings
    @Environment(\.scenePhase) private var scenePhase
    @State private var state = SettingsUiState(
        noteStyle: NoteStyleRefBuiltIn(preset: .summary),
        customStyles: [],
        isDynamicColorEnabled: false,
        isAppLockEnabled: false,
        isDeviceSecure: false,
        isAudioImportUnlocked: false,
        isSmartSuggestionsEnabled: false,
        isSmartSuggestionsUnlocked: false,
        pro: ProStatusUiFree.shared,
        proOverride: nil,
        isImportPickerRequested: false,
        isRedeemFallbackRequested: false,
        importNotice: nil
    )
    @State private var isAudioImporterPresented = false

    var body: some View {
        NavigationStack {
            Form {
                proSection
                Section(String(localized: "Notes")) {
                    NavigationLink {
                        NoteStylesView()
                    } label: {
                        VStack(alignment: .leading, spacing: 2) {
                            Text(String(localized: "Summary styles"))
                            Text(
                                String(
                                    format: String(localized: "Default: %@"),
                                    NoteStyleLabels.label(for: state.noteStyle, customStyles: state.customStyles)
                                )
                            )
                            .font(.caption)
                            .foregroundStyle(.secondary)
                        }
                    }
                    SettingsSwitchRow(
                        title: String(localized: "Show smart suggestions"),
                        subtitle: String(localized: "Find calendar events and to-dos in every new note. Adds a few seconds of processing."),
                        isOn: state.isSmartSuggestionsEnabled,
                        showsProBadge: !state.isSmartSuggestionsUnlocked
                    ) {
                        viewModel.onSmartSuggestionsChanged(enabled: !state.isSmartSuggestionsEnabled)
                    }
                }
                Section(String(localized: "Security")) {
                    SettingsSwitchRow(
                        title: String(localized: "Require unlock to open Offhand"),
                        subtitle: state.isDeviceSecure
                            ? String(localized: "Ask for Face ID, Touch ID, or your passcode every time Offhand opens.")
                            : String(localized: "Set a passcode on this iPhone to use this."),
                        isOn: state.isAppLockEnabled && state.isDeviceSecure
                    ) {
                        viewModel.onAppLockChanged(enabled: !state.isAppLockEnabled)
                    }
                    .disabled(!state.isDeviceSecure)
                }
                Section {
                    NavigationLink {
                        BackupView()
                    } label: {
                        VStack(alignment: .leading, spacing: 2) {
                            Text(String(localized: "Backup & restore"))
                            Text(String(localized: "Move your notes to a new iPhone or keep a copy."))
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        }
                    }
                    SettingsActionRow(
                        title: String(localized: "Import audio"),
                        subtitle: String(localized: "Turn audio files into notes. Pick one or several at once."),
                        showsProBadge: !state.isAudioImportUnlocked
                    ) {
                        viewModel.onImportAudioClicked()
                    }
                } header: {
                    Text(String(localized: "Backup"))
                }
                Section {
                    NavigationLink {
                        AboutSupportView()
                    } label: {
                        VStack(alignment: .leading, spacing: 2) {
                            Text(String(localized: "About & support"))
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
                    Text(String(localized: "Everything stays on your iPhone. Nothing is uploaded, ever."))
                }
                if let override = state.proOverride {
                    proOverrideSection(override)
                }
            }
            .navigationTitle(String(localized: "Settings"))
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
            let staged = urls.map(AudioFileStaging.stage)
            viewModel.onAudioImportSelected(
                sources: staged.compactMap { $0 },
                unreadableCount: Int32(staged.filter { $0 == nil }.count)
            )
        }
        .alert(importNoticeTitle, isPresented: importNoticeBinding) {
            Button(String(localized: "OK")) { viewModel.onImportNoticeDismissed() }
        } message: {
            if let notice = state.importNotice {
                Text(ImportNoticeText.message(notice))
            }
        }
        .task {
            viewModel.onScreenShown()
            for await newState in viewModel.uiState {
                state = newState
            }
        }
    }

    private var importNoticeBinding: Binding<Bool> {
        Binding(
            get: { state.importNotice != nil },
            set: { isShown in if !isShown { viewModel.onImportNoticeDismissed() } }
        )
    }

    private var importNoticeTitle: String {
        ImportNoticeText.title(state.importNotice)
    }

    private var appVersion: String {
        Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "1.0"
    }

    // One Subscription section in both states: free users get the upgrade row,
    // subscribers the plan and the store's own management page, where
    // cancellation lives.
    @ViewBuilder
    private var proSection: some View {
        if case .free = onEnum(of: state.pro) {
            Section(String(localized: "Subscription")) {
                HStack(spacing: 12) {
                    VStack(alignment: .leading, spacing: 2) {
                        HStack(spacing: 8) {
                            Text(String(localized: "Offhand Pro"))
                            ProCrown(size: 20)
                        }
                        Text(String(localized: "Custom styles, PDF and Word export, smart suggestions and audio import"))
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                    Spacer()
                    Button(String(localized: "Upgrade")) { viewModel.onProCardClicked() }
                        .buttonStyle(.borderedProminent)
                }
                .padding(.vertical, 4)
                .contentShape(Rectangle())
                .onTapGesture { viewModel.onProCardClicked() }
                SettingsActionRow(title: String(localized: "Redeem a code")) {
                    viewModel.onRedeemCodeClicked()
                    OfferCodeRedemption.present()
                }
            }
        } else {
            Section(String(localized: "Subscription")) {
                Button {
                    viewModel.onProCardClicked()
                } label: {
                    HStack(spacing: 12) {
                        VStack(alignment: .leading, spacing: 2) {
                            HStack(spacing: 8) {
                                Text(String(localized: "Offhand Pro"))
                                ProCrown(size: 20)
                            }
                            Text(proStatusLabel).font(.caption).foregroundStyle(.secondary)
                        }
                        Spacer()
                        Image(systemName: "chevron.right")
                            .font(.footnote.weight(.semibold))
                            .foregroundStyle(.tertiary)
                    }
                    .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                if case .lifetime = onEnum(of: state.pro) {} else {
                    SettingsActionRow(title: String(localized: "Manage subscription")) {
                        SubscriptionManagement.present()
                    }
                }
                SettingsActionRow(title: String(localized: "Redeem a code")) {
                    viewModel.onRedeemCodeClicked()
                    OfferCodeRedemption.present()
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
}

// A tappable Settings row that looks like a navigation row: primary text, an
// optional grey subtitle or trailing value, and the same chevron. A plain
// Button in a Form tints its label with the accent colour instead.
private struct SettingsActionRow: View {
    let title: String
    var subtitle: String?
    var value: String?
    var showsProBadge = false
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    HStack(spacing: 6) {
                        Text(title)
                        if showsProBadge { ProBadge() }
                    }
                    if let subtitle {
                        Text(subtitle).font(.caption).foregroundStyle(.secondary)
                    }
                }
                Spacer()
                if let value {
                    Text(value).foregroundStyle(.secondary)
                }
                Image(systemName: "chevron.right")
                    .font(.footnote.weight(.semibold))
                    .foregroundStyle(.tertiary)
            }
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }
}

// The whole row flips the switch, like the Android row; a plain Toggle only
// reacts to the switch itself.
private struct SettingsSwitchRow: View {
    let title: String
    let subtitle: String
    let isOn: Bool
    var showsProBadge = false
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    HStack(spacing: 6) {
                        Text(title)
                        if showsProBadge { ProBadge() }
                    }
                    Text(subtitle).font(.caption).foregroundStyle(.secondary)
                }
                Spacer()
                Toggle("", isOn: .constant(isOn))
                    .tint(Brand.toggle)
                    .labelsHidden()
                    .allowsHitTesting(false)
            }
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }
}
