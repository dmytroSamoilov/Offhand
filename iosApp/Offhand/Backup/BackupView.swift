import OffhandShared
import SwiftUI
import UniformTypeIdentifiers

struct BackupView: View {
    private let viewModel = AppViewModels.backup
    @State private var state = BackupUiState(
        includeAudio: true,
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
        Form {
            Section {
                Button(String(localized: "Back up")) { viewModel.onBackupFlowStarted() }
            } header: {
                Text(String(localized: "Backup"))
            } footer: {
                Text(String(localized: "Save an encrypted copy of your notes, folders and recordings to a file. You will need the passphrase to restore it, and it cannot be recovered."))
            }
            Section {
                Button(String(localized: "Choose a backup file")) { isImporterPresented = true }
            } header: {
                Text(String(localized: "Restore"))
            } footer: {
                Text(String(localized: "Add notes and folders from a backup file. Notes already on this iPhone are kept and duplicates are skipped."))
            }
        }
        .navigationTitle(String(localized: "Backup & restore"))
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
        .alert(restoreResultTitle, isPresented: restoreResultBinding) {
            Button(String(localized: "OK")) { viewModel.onOperationDismissed() }
        } message: {
            Text(BackupTexts.message(for: state.operation))
        }
        .overlay {
            if case .running(let running) = onEnum(of: state.operation), running.mode == .restore {
                ProgressOverlay(mode: running.mode, percent: Int(running.percent))
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

private struct BackupSheet: View {
    let viewModel: BackupViewModel
    let state: BackupUiState
    @State private var exportURL: URL?
    @State private var pendingBackupURL: URL?

    var body: some View {
        NavigationStack {
            Form {
                switch onEnum(of: state.operation) {
                case .none: stepContent
                case .running(let running): progressContent(percent: Int(running.percent))
                default: resultContent
                }
            }
            .navigationTitle(sheetTitle)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                if state.operation == nil {
                    ToolbarItem(placement: .cancellationAction) {
                        Button(String(localized: "Cancel")) { viewModel.onBackupFlowDismissed() }
                    }
                }
            }
        }
        .presentationDetents([.large])
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

    private var passphraseStep: some View {
        Group {
            Section {
                SecureField(String(localized: "Passphrase"), text: passphraseBinding)
                SecureField(String(localized: "Repeat passphrase"), text: confirmationBinding)
                if let error = state.passphraseError {
                    Text(BackupTexts.passphraseError(error))
                        .font(.footnote)
                        .foregroundStyle(.red)
                }
            }
            Section {
                WarningBox(text: String(localized: "Keep this passphrase safe. If you lose it, neither you nor we can recover the notes in this backup."))
                    .listRowInsets(EdgeInsets())
                    .listRowBackground(Color.clear)
            }
            Section {
                Button(String(localized: "Continue")) { viewModel.onBackupPassphraseContinued() }
                    .frame(maxWidth: .infinity)
            }
        }
    }

    private var optionsStep: some View {
        Group {
            Section {
                HStack {
                    TextField(String(localized: "File name"), text: fileNameBinding)
                    Text(BackupFileNames.shared.EXTENSION)
                        .foregroundStyle(.secondary)
                }
                Toggle(String(localized: "Include audio recordings"), isOn: Binding(
                    get: { state.includeAudio },
                    set: { viewModel.onIncludeAudioChanged(enabled: $0) }
                ))
            } footer: {
                Text(String(localized: "Recordings make the file much larger."))
            }
            Section {
                Button(String(localized: "Save backup")) { startBackup() }
                    .frame(maxWidth: .infinity)
            }
        }
    }

    private func progressContent(percent: Int) -> some View {
        Section {
            VStack(spacing: 14) {
                ProgressView(value: Double(percent), total: 100)
                    .tint(Brand.primary)
                Text("\(percent)%")
                    .font(.subheadline.monospacedDigit())
                    .foregroundStyle(.secondary)
            }
            .padding(.vertical, 8)
        }
    }

    private var resultContent: some View {
        Group {
            Section {
                Text(BackupTexts.message(for: state.operation))
            }
            Section {
                Button(String(localized: "Done")) { viewModel.onBackupFlowDismissed() }
                    .frame(maxWidth: .infinity)
            }
        }
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

private struct WarningBox: View {
    let text: String

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            Image(systemName: "exclamationmark.triangle.fill")
            Text(text)
                .font(.subheadline)
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Brand.warningContainer, in: RoundedRectangle(cornerRadius: 12))
        .foregroundStyle(Brand.onWarningContainer)
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
    let mode: BackupModeUi
    let percent: Int

    var body: some View {
        ZStack {
            Color.black.opacity(0.35).ignoresSafeArea()
            VStack(spacing: 14) {
                ProgressView(value: Double(percent), total: 100)
                    .tint(Brand.primary)
                Text(mode == .backup ? String(localized: "Backing up") : String(localized: "Restoring"))
                    .font(.headline)
                Text("\(percent)%")
                    .font(.subheadline.monospacedDigit())
                    .foregroundStyle(.secondary)
            }
            .padding(24)
            .frame(maxWidth: 280)
            .background(Color(.secondarySystemGroupedBackground), in: RoundedRectangle(cornerRadius: 16))
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
