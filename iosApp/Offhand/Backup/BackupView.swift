import OffhandShared
import SwiftUI
import UniformTypeIdentifiers

struct BackupView: View {
    private let viewModel = AppViewModels.backup
    @State private var state = BackupUiState(
        includeAudio: false,
        isAudioUnlocked: false,
        includeStyles: true,
        passphrase: "",
        passphraseConfirmation: "",
        passphraseError: nil,
        isRestorePassphraseRequested: false,
        backupStep: nil,
        backupFileName: "",
        operation: nil
    )
    @State private var isImporterPresented = false

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                Text(String(localized: "Backup & restore"))
                    .offhandText(.title)
                    .foregroundStyle(OffhandColor.ink)
                    .padding(.top, 8)
                    .accessibilityAddTraits(.isHeader)
                Text(String(localized: "Offhand makes one encrypted file. You decide where it is kept."))
                    .offhandText(.dialogBody)
                    .foregroundStyle(OffhandColor.ink2)
                    .padding(.top, 8)
                BackupSectionLabel(text: String(localized: "Make a backup"))
                PrimaryButton(String(localized: "Create backup file"), fillsWidth: true) { viewModel.onBackupFlowStarted() }
                BackupHint(text: String(localized: "Save an encrypted copy of your notes, folders and recordings to a file. You will need the passphrase to restore it, and it cannot be recovered."))
                BackupSectionLabel(text: String(localized: "Restore")).padding(.top, 8)
                SecondaryButton(String(localized: "Choose a backup file"), fillsWidth: true) { isImporterPresented = true }
                BackupHint(text: String(localized: "Add notes and folders from a backup file. Notes already on this iPhone are kept and duplicates are skipped."))
            }
            .padding(.horizontal, OffhandSpacing.screen)
            .padding(.bottom, OffhandSpacing.screen)
        }
        .background(OffhandColor.ground.ignoresSafeArea())
        .navigationBarTitleDisplayMode(.inline)
        .sheet(isPresented: backupSheetBinding) {
            BackupSheet(viewModel: viewModel, state: state)
                .interactiveDismissDisabled(isRunning)
        }
        .fileImporter(isPresented: $isImporterPresented, allowedContentTypes: [.data]) { result in
            if case .success(let url) = result { importBackup(from: url) }
        }
        .alert(String(localized: "Enter the backup passphrase"), isPresented: restorePassphraseBinding) {
            SecureField(String(localized: "Passphrase"), text: passphraseBinding)
            Button(String(localized: "Restore")) { viewModel.onRestoreConfirmed() }
            Button(String(localized: "Cancel"), role: .cancel) { viewModel.onRestorePassphraseDismissed() }
        }
        .offhandDialog(isPresented: restoreResultBinding) {
            OffhandDialog(
                title: restoreResultTitle,
                message: BackupTexts.message(for: state.operation),
                confirmLabel: String(localized: "OK"),
                onConfirm: { viewModel.onOperationDismissed() },
                onDismiss: { viewModel.onOperationDismissed() }
            )
        }
        .overlay {
            if case .running(let running) = onEnum(of: state.operation), running.mode == .restore {
                ProgressOverlay(percent: Int(running.percent))
            }
        }
        .task {
            for await newState in viewModel.uiState {
                state = newState
            }
        }
    }

    private var isRunning: Bool {
        if case .running = onEnum(of: state.operation) { return true }
        return false
    }

    private func importBackup(from url: URL) {
        let accessed = url.startAccessingSecurityScopedResource()
        defer { if accessed { url.stopAccessingSecurityScopedResource() } }
        let copy = FileManager.default.temporaryDirectory.appendingPathComponent("restore.offhand")
        try? FileManager.default.removeItem(at: copy)
        guard (try? FileManager.default.copyItem(at: url, to: copy)) != nil else { return }
        viewModel.onRestoreSourceChosen(file: PathBackupFile(path: copy.path))
    }

    private var backupSheetBinding: Binding<Bool> {
        Binding(
            get: { state.backupStep != nil },
            set: { isShown in if !isShown { viewModel.onBackupFlowDismissed() } }
        )
    }

    private var passphraseBinding: Binding<String> {
        Binding(get: { state.passphrase }, set: { viewModel.onPassphraseChanged(value: $0) })
    }

    private var restorePassphraseBinding: Binding<Bool> {
        Binding(
            get: { state.isRestorePassphraseRequested },
            set: { isShown in if !isShown { viewModel.onRestorePassphraseDismissed() } }
        )
    }

    private var restoreResultBinding: Binding<Bool> {
        Binding(
            get: {
                switch onEnum(of: state.operation) {
                case .restoreCompleted: return true
                case .failed(let failed): return failed.mode == .restore
                default: return false
                }
            },
            set: { isShown in if !isShown { viewModel.onOperationDismissed() } }
        )
    }

    private var restoreResultTitle: String {
        switch onEnum(of: state.operation) {
        case .restoreCompleted: return String(localized: "Restore complete")
        case .failed: return String(localized: "Restore failed")
        default: return ""
        }
    }
}

private struct BackupSectionLabel: View {
    let text: String

    var body: some View {
        SectionLabel(text)
            .padding(.top, 26)
            .padding(.bottom, 10)
    }
}

private struct BackupHint: View {
    let text: String

    var body: some View {
        Text(text)
            .offhandText(.caption)
            .foregroundStyle(OffhandColor.ink2)
            .padding(.top, 8)
    }
}

private struct BackupSheet: View {
    let viewModel: BackupViewModel
    let state: BackupUiState
    @State private var exportURL: URL?
    @State private var pendingBackupURL: URL?

    var body: some View {
        OffhandSheet(title: sheetTitle) {
            if state.operation == nil {
                PlainTextButton(String(localized: "Cancel"), tone: .muted) { viewModel.onBackupFlowDismissed() }
            }
        } content: {
            VStack(alignment: .leading, spacing: 12) {
                switch onEnum(of: state.operation) {
                case .none: stepContent
                case .running(let running): progressContent(percent: Int(running.percent))
                default: resultContent
                }
            }
        }
        .sheet(item: $exportURL) { url in
            DocumentExporter(url: url)
        }
        .onChange(of: isBackupCompleted) {
            if isBackupCompleted, let url = pendingBackupURL {
                pendingBackupURL = nil
                exportURL = url
            }
        }
    }

    @ViewBuilder
    private var stepContent: some View {
        switch state.backupStep {
        case .passphrase: passphraseStep
        case .options: optionsStep
        default: EmptyView()
        }
    }

    @ViewBuilder
    private var passphraseStep: some View {
        OffhandTextField(text: passphraseBinding, placeholder: String(localized: "Passphrase"), isSecure: true)
        OffhandTextField(
            text: confirmationBinding,
            placeholder: String(localized: "Repeat passphrase"),
            error: state.passphraseError.map(BackupTexts.passphraseError),
            isSecure: true
        )
        Text(String(localized: "Keep this passphrase safe. If you lose it, neither you nor we can recover the notes in this backup."))
            .offhandText(.caption)
            .foregroundStyle(OffhandColor.ink2)
        PrimaryButton(String(localized: "Continue"), fillsWidth: true) { viewModel.onBackupPassphraseContinued() }
            .padding(.top, 8)
    }

    @ViewBuilder
    private var optionsStep: some View {
        OffhandTextField(text: fileNameBinding, placeholder: String(localized: "File name")) {
            Text(BackupFileNames.shared.EXTENSION)
                .offhandText(.body)
                .foregroundStyle(OffhandColor.ink3)
                .padding(.trailing, 12)
        }
        SurfaceCard {
            SwitchRow(
                title: String(localized: "Include audio recordings"),
                subtitle: String(localized: "Recordings make the file much larger."),
                isOn: state.includeAudio,
                showsProBadge: !state.isAudioUnlocked
            ) { viewModel.onIncludeAudioChanged(enabled: $0) }
            SwitchRow(
                title: String(localized: "Include writing styles"),
                isOn: state.includeStyles,
                showsDivider: false
            ) { viewModel.onIncludeStylesChanged(enabled: $0) }
        }
        PrimaryButton(String(localized: "Save backup"), fillsWidth: true) { startBackup() }
            .padding(.top, 8)
    }

    private func progressContent(percent: Int) -> some View {
        HStack(spacing: 12) {
            ProgressLine(progress: Double(percent) / 100, thickness: 3)
            Text(verbatim: "\(percent)%")
                .offhandText(.number)
                .foregroundStyle(OffhandColor.ink2)
        }
        .padding(.vertical, 12)
    }

    @ViewBuilder
    private var resultContent: some View {
        Text(BackupTexts.message(for: state.operation))
            .offhandText(.dialogBody)
            .foregroundStyle(OffhandColor.ink2)
        PrimaryButton(String(localized: "Done"), fillsWidth: true) { viewModel.onBackupFlowDismissed() }
            .padding(.top, 8)
    }

    private var sheetTitle: String {
        switch onEnum(of: state.operation) {
        case .none:
            return state.backupStep == .passphrase
                ? String(localized: "Set a passphrase")
                : String(localized: "Backup options")
        case .running: return String(localized: "Backing up")
        case .backupCompleted: return String(localized: "Backup saved")
        case .failed: return String(localized: "Backup failed")
        default: return ""
        }
    }

    private func startBackup() {
        let name = BackupFileNames.shared.withExtension(raw: state.backupFileName)
        let url = FileManager.default.temporaryDirectory.appendingPathComponent(name)
        try? FileManager.default.removeItem(at: url)
        pendingBackupURL = url
        viewModel.onBackupTargetChosen(file: PathBackupFile(path: url.path))
    }

    private var isBackupCompleted: Bool {
        if case .backupCompleted = onEnum(of: state.operation) { return true }
        return false
    }

    private var passphraseBinding: Binding<String> {
        Binding(get: { state.passphrase }, set: { viewModel.onPassphraseChanged(value: $0) })
    }

    private var confirmationBinding: Binding<String> {
        Binding(get: { state.passphraseConfirmation }, set: { viewModel.onPassphraseConfirmationChanged(value: $0) })
    }

    private var fileNameBinding: Binding<String> {
        Binding(get: { state.backupFileName }, set: { viewModel.onBackupFileNameChanged(name: $0) })
    }
}

private enum BackupTexts {
    static func message(for operation: BackupOperationUi?) -> String {
        switch onEnum(of: operation) {
        case .backupCompleted(let done):
            return String(format: String(localized: "%d notes and %d folders were saved."), Int(done.notes), Int(done.folders))
        case .restoreCompleted(let done):
            return String(
                format: String(localized: "%d notes restored, %d skipped as duplicates. Folders: %d created, %d reused."),
                Int(done.notesRestored), Int(done.notesSkipped), Int(done.foldersCreated), Int(done.foldersReused)
            )
        case .failed(let failed): return errorMessage(failed.error)
        default: return ""
        }
    }

    static func passphraseError(_ error: PassphraseErrorUi) -> String {
        switch error {
        case .tooShort:
            return String(format: String(localized: "Use at least %d characters."), Int(BackupUiState.companion.MIN_PASSPHRASE_LENGTH))
        case .mismatch: return String(localized: "The passphrases do not match.")
        default: return ""
        }
    }

    private static func errorMessage(_ error: BackupErrorUi) -> String {
        switch error {
        case .wrongPassphrase: return String(localized: "That passphrase does not open this backup.")
        case .corruptFile: return String(localized: "This file is not a readable Offhand backup.")
        case .unsupportedVersion: return String(localized: "This backup was made with a newer version of Offhand.")
        case .io: return String(localized: "The file could not be read or written.")
        default: return ""
        }
    }
}

private struct ProgressOverlay: View {
    let percent: Int

    var body: some View {
        ZStack {
            OffhandColor.scrim.ignoresSafeArea()
            VStack(alignment: .leading, spacing: 14) {
                Text(String(localized: "Restoring"))
                    .offhandText(.heading)
                    .foregroundStyle(OffhandColor.ink)
                HStack(spacing: 12) {
                    ProgressLine(progress: Double(percent) / 100, thickness: 3)
                    Text(verbatim: "\(percent)%")
                        .offhandText(.number)
                        .foregroundStyle(OffhandColor.ink2)
                }
            }
            .padding(OffhandSpacing.screen)
            .frame(maxWidth: 300)
            .background(OffhandColor.surface, in: RoundedRectangle(cornerRadius: OffhandRadius.sheet, style: .continuous))
        }
    }
}

extension URL: @retroactive Identifiable {
    public var id: String { absoluteString }
}

private struct DocumentExporter: UIViewControllerRepresentable {
    let url: URL

    func makeUIViewController(context: Context) -> UIDocumentPickerViewController {
        UIDocumentPickerViewController(forExporting: [url], asCopy: false)
    }

    func updateUIViewController(_ uiViewController: UIDocumentPickerViewController, context: Context) {}
}
