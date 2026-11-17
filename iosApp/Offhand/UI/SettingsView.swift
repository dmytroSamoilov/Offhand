import OffhandShared
import SwiftUI
import UniformTypeIdentifiers

struct SettingsView<BottomBarContent: View>: View {
    private let viewModel = AppViewModels.settings
    private let bottomBar: BottomBarContent
    @Environment(\.scenePhase) private var scenePhase
    @State private var state = SettingsUiState(
        noteStyle: NoteStyleRefBuiltIn(preset: .summary),
        customStyles: [],
        isDynamicColorEnabled: false,
        isAppLockEnabled: false,
        isTelemetryEnabled: true,
        isDeviceSecure: false,
        isAudioImportUnlocked: false,
        freeImportsLeft: nil,
        allowsMultipleImports: false,
        isSmartSuggestionsEnabled: false,
        isTranscriptionEnabled: true,
        isSummaryEnabled: true,
        isSmartSuggestionsUnlocked: false,
        pro: ProStatusUiFree.shared,
        proOverride: nil,
        isImportPickerRequested: false,
        isRedeemFallbackRequested: false,
        importNotice: nil
    )
    @State private var isAudioImporterPresented = false
    @State private var destination: SettingsDestination?

    init(@ViewBuilder bottomBar: () -> BottomBarContent) {
        self.bottomBar = bottomBar()
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    Text(String(localized: "Settings"))
                        .offhandText(.display)
                        .foregroundStyle(OffhandColor.ink)
                        .padding(.horizontal, 8)
                        .padding(.top, 12)
                        .accessibilityAddTraits(.isHeader)
                    PlanCard(pro: state.pro, onOpen: viewModel.onProCardClicked)
                        .padding(.top, OffhandSpacing.card)
                    writingGroup
                    notesInAndOutGroup
                    privacyGroup
                    appGroup
                    if let override = state.proOverride {
                        developerGroup(override)
                    }
                }
                .padding(.horizontal, OffhandSpacing.card)
                .padding(.bottom, OffhandSpacing.screen)
            }
            .background(OffhandColor.ground.ignoresSafeArea())
            .safeAreaInset(edge: .bottom, spacing: 0) { bottomBar }
            .toolbar(.hidden, for: .navigationBar)
            .navigationDestination(item: $destination) { destination in
                switch destination {
                case .styles: NoteStylesView()
                case .backup: BackupView()
                case .about: AboutSupportView()
                }
            }
        }
        .onChange(of: scenePhase) {
            if scenePhase == .active { viewModel.onScreenShown() }
        }
        .onChange(of: state.isImportPickerRequested) {
            guard state.isImportPickerRequested else { return }
            viewModel.onImportPickerOpened()
            isAudioImporterPresented = true
        }
        .fileImporter(
            isPresented: $isAudioImporterPresented,
            allowedContentTypes: [.audio, .movie],
            allowsMultipleSelection: state.allowsMultipleImports
        ) { result in
            guard case .success(let urls) = result else { return }
            let staged = urls.map(AudioFileStaging.stage)
            viewModel.onAudioImportSelected(
                sources: staged.compactMap { $0 },
                unreadableCount: Int32(staged.filter { $0 == nil }.count)
            )
        }
        .offhandDialog(isPresented: importNoticeBinding) {
            OffhandDialog(
                title: ImportNoticeText.title(state.importNotice),
                message: state.importNotice.map(ImportNoticeText.message),
                confirmLabel: String(localized: "OK"),
                onConfirm: { viewModel.onImportNoticeDismissed() },
                onDismiss: { viewModel.onImportNoticeDismissed() }
            )
        }
        .task {
            viewModel.onScreenShown()
            for await newState in viewModel.uiState {
                state = newState
            }
        }
    }

    private var writingGroup: some View {
        SettingsGroup(label: String(localized: "What Offhand writes")) {
            SwitchRow(
                title: String(localized: "Transcribe recordings"),
                subtitle: String(localized: "Turns what you say into text"),
                isOn: state.isTranscriptionEnabled
            ) { viewModel.onTranscriptionChanged(enabled: $0) }
            SwitchRow(
                title: String(localized: "Write a summary"),
                subtitle: String(localized: "From the transcript, in your writing style"),
                isOn: state.isSummaryEnabled && state.isTranscriptionEnabled
            ) { viewModel.onSummaryChanged(enabled: $0) }
            .disabled(!state.isTranscriptionEnabled)
            LinkRow(
                title: String(localized: "Writing style"),
                subtitle: String(localized: "Used for new notes"),
                value: NoteStyleLabels.label(for: state.noteStyle, customStyles: state.customStyles)
            ) { destination = .styles }
            SwitchRow(
                title: String(localized: "Smart suggestions"),
                subtitle: String(localized: "Finds dates to add to your calendar"),
                isOn: state.isSmartSuggestionsEnabled,
                showsProBadge: !state.isSmartSuggestionsUnlocked,
                showsDivider: false
            ) { viewModel.onSmartSuggestionsChanged(enabled: $0) }
        }
    }

    private var notesInAndOutGroup: some View {
        SettingsGroup(label: String(localized: "Getting notes in and out")) {
            LinkRow(
                title: String(localized: "Import audio or video"),
                subtitle: importSubtitle,
                showsProBadge: !state.isAudioImportUnlocked
            ) { viewModel.onImportAudioClicked() }
            LinkRow(
                title: String(localized: "Backup & restore"),
                subtitle: String(localized: "One encrypted file that you keep"),
                showsDivider: false
            ) { destination = .backup }
        }
    }

    private var privacyGroup: some View {
        VStack(alignment: .leading, spacing: 0) {
            SettingsGroup(label: String(localized: "Privacy")) {
                SwitchRow(
                    title: String(localized: "App lock"),
                    subtitle: state.isDeviceSecure
                        ? String(localized: "Face ID, Touch ID or your passcode when you open Offhand")
                        : String(localized: "Set a passcode on this iPhone to use this."),
                    isOn: state.isAppLockEnabled && state.isDeviceSecure,
                    onChange: onAppLockChanged
                )
                .disabled(!state.isDeviceSecure)
                SwitchRow(
                    title: String(localized: "Share anonymous usage"),
                    subtitle: String(localized: "Anonymous usage statistics and crash reports — things like app opens, device model and app version. Never your notes, audio, or any personal data."),
                    isOn: state.isTelemetryEnabled,
                    showsDivider: false
                ) { viewModel.onTelemetryChanged(granted: $0) }
            }
            Text(String(localized: "Everything stays on your iPhone. Nothing is uploaded, ever."))
                .offhandText(.caption)
                .foregroundStyle(OffhandColor.ink2)
                .padding(.horizontal, OffhandSpacing.card)
                .padding(.top, 12)
        }
    }

    private var appGroup: some View {
        SettingsGroup(label: String(localized: "App")) {
            if isSubscription(state.pro) {
                LinkRow(title: String(localized: "Manage subscription")) { SubscriptionManagement.present() }
            }
            LinkRow(title: String(localized: "Redeem a code")) {
                viewModel.onRedeemCodeClicked()
                OfferCodeRedemption.present()
            }
            LinkRow(
                title: String(localized: "About & support"),
                subtitle: String(format: String(localized: "Version %@ · feedback, privacy and legal"), appVersion),
                showsDivider: false
            ) { destination = .about }
        }
    }

    private func developerGroup(_ override: ProOverride) -> some View {
        SettingsGroup(label: String(localized: "Developer")) {
            VStack(alignment: .leading, spacing: 10) {
                Text(String(localized: "Pro status override"))
                    .offhandText(.settingTitle)
                    .foregroundStyle(OffhandColor.ink)
                SegmentedControl(
                    options: ProOverrideChoice.all.map(\.label),
                    selectedIndex: ProOverrideChoice.all.firstIndex { $0.value == override } ?? 0,
                    isCompact: true
                ) { viewModel.onProOverrideSelected(override: ProOverrideChoice.all[$0].value) }
            }
            .padding(OffhandSpacing.card)
        }
    }

    private func onAppLockChanged(_ enabled: Bool) {
        guard enabled else {
            viewModel.onAppLockChanged(enabled: false)
            return
        }
        DeviceAuthenticator.confirmOwner(reason: String(localized: "Confirm it's you to turn on the app lock.")) { confirmed in
            if confirmed { viewModel.onAppLockChanged(enabled: true) }
        }
    }

    private var importNoticeBinding: Binding<Bool> {
        Binding(
            get: { state.importNotice != nil },
            set: { isShown in if !isShown { viewModel.onImportNoticeDismissed() } }
        )
    }

    private var appVersion: String {
        Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "1.0"
    }

    private var importSubtitle: String {
        guard let left = state.freeImportsLeft else {
            return String(localized: "Turn recordings, voice memos and videos into notes.")
        }
        return String.localizedStringWithFormat(
            String(localized: "%1$d of %2$d free imports left"),
            Int(truncating: left),
            Int(FreeImportLimits.shared.FREE_IMPORTS)
        )
    }

    private func isSubscription(_ status: ProStatusUi) -> Bool {
        switch onEnum(of: status) {
        case .yearly, .monthly, .trial: return true
        case .free, .lifetime, .included, .appTrial: return false
        }
    }
}

private enum SettingsDestination: Hashable {
    case styles, backup, about
}

private struct ProOverrideChoice {
    let label: String
    let value: ProOverride

    static let all = [
        ProOverrideChoice(label: "Store", value: .store),
        ProOverrideChoice(label: "Free", value: .free),
        ProOverrideChoice(label: "Pro", value: .pro),
    ]
}

private struct PlanCard: View {
    let pro: ProStatusUi
    let onOpen: () -> Void
    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        let shape = RoundedRectangle(cornerRadius: OffhandRadius.card, style: .continuous)
        content
            .padding(.horizontal, 18)
            .padding(.top, 18)
            .padding(.bottom, isUpgradeOffered ? 4 : 18)
            .frame(maxWidth: .infinity, alignment: .leading)
            .foregroundStyle(OffhandColor.ink)
            .environment(\.colorScheme, .dark)
            .background(colorScheme == .dark ? OffhandColor.surface : OffhandColor.ink, in: shape)
            .overlay { shape.strokeBorder(colorScheme == .dark ? OffhandColor.hairline : .clear, lineWidth: 1) }
    }

    @ViewBuilder
    private var content: some View {
        if isUpgradeOffered {
            upgradeContent
        } else {
            Button(action: onOpen) { planContent }
                .buttonStyle(.offhandPress)
        }
    }

    private var upgradeContent: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack(alignment: .firstTextBaseline, spacing: 12) {
                cardTitle(trial == nil ? String(localized: "Offhand Pro") : String(localized: "Offhand Pro, free trial"))
                if let trial {
                    Text(String.localizedStringWithFormat(String(localized: "%d days left"), Int(trial.daysLeft)))
                        .font(.offhand(.monoRegular, size: 12, relativeTo: .caption))
                        .textCase(.uppercase)
                        .foregroundStyle(OffhandColor.gold)
                }
            }
            if let trial {
                GoldProgress(fraction: CGFloat(trial.elapsed)).padding(.top, 14)
            }
            Text(trial == nil
                ? String(localized: "Styles, PDF and Word export, smart suggestions, audio and video import and unlimited folders")
                : String(localized: "Everything is unlocked for now. After that Offhand stays free, with the Summary style and two folders."))
                .offhandText(.caption)
                .foregroundStyle(OffhandColor.ink2)
                .padding(.top, 12)
            Button(String(localized: "Upgrade"), action: onOpen)
                .offhandText(.buttonSmall)
                .foregroundStyle(OffhandColor.gold)
                .frame(minHeight: 48)
                .contentShape(Rectangle())
                .buttonStyle(.offhandPress)
        }
        .contentShape(Rectangle())
        .onTapGesture(perform: onOpen)
    }

    private var planContent: some View {
        HStack(spacing: 12) {
            VStack(alignment: .leading, spacing: 4) {
                cardTitle(String(localized: "Offhand Pro"))
                Text(proStatusLabel)
                    .offhandText(.caption)
                    .foregroundStyle(OffhandColor.ink2)
                    .multilineTextAlignment(.leading)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            RowChevron()
        }
    }

    private func cardTitle(_ text: String) -> some View {
        Text(text)
            .font(.offhand(.tightSemibold, size: 20, relativeTo: .title3))
            .frame(maxWidth: .infinity, alignment: .leading)
    }

    private var trial: ProStatusUiAppTrial? {
        guard case .appTrial(let trial) = onEnum(of: pro) else { return nil }
        return trial
    }

    private var isUpgradeOffered: Bool {
        switch onEnum(of: pro) {
        case .free, .appTrial: return true
        case .lifetime, .included, .trial, .yearly, .monthly: return false
        }
    }

    private var proStatusLabel: String {
        switch onEnum(of: pro) {
        case .free, .appTrial: return ""
        case .lifetime: return String(localized: "Lifetime, yours forever")
        case .included: return String(localized: "Included on this iPhone. It is not part of backups and does not survive a reinstall.")
        case .trial(let trial):
            guard let endsAt = trial.endsAtMs else { return String(localized: "Free trial") }
            return String(format: String(localized: "Free trial until %@"), formatDate(endsAt.int64Value))
        case .monthly(let monthly):
            guard let renewsAt = monthly.renewsAtMs else { return String(localized: "Monthly") }
            return String(format: String(localized: "Monthly, renews on %@"), formatDate(renewsAt.int64Value))
        case .yearly(let yearly):
            guard let renewsAt = yearly.renewsAtMs else { return String(localized: "Yearly") }
            return String(format: String(localized: "Yearly, renews on %@"), formatDate(renewsAt.int64Value))
        }
    }

    private func formatDate(_ epochMs: Int64) -> String {
        Date(timeIntervalSince1970: TimeInterval(epochMs) / 1000).formatted(date: .abbreviated, time: .omitted)
    }
}

private struct GoldProgress: View {
    let fraction: CGFloat

    var body: some View {
        Capsule()
            .fill(OffhandColor.border)
            .frame(height: 3)
            .overlay(alignment: .leading) {
                GeometryReader { geometry in
                    Rectangle()
                        .fill(OffhandColor.gold)
                        .frame(width: geometry.size.width * min(max(fraction, 0), 1))
                }
            }
            .clipShape(Capsule())
            .accessibilityHidden(true)
    }
}
