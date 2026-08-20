import OffhandShared
import SwiftUI

struct OnboardingView: View {
    let onFinished: () -> Void
    @Environment(\.scenePhase) private var scenePhase
    private let viewModel = AppViewModels.onboarding
    @State private var state = OnboardingUiState(
        step: .deviceCheck,
        deviceSpecs: nil,
        downloadSizeGb: "",
        notePreset: .summary,
        isTelemetryEnabled: true,
        currentPage: 0,
        pageCount: 0
    )
    @State private var modelState: ModelState = ModelStateNotDownloaded.shared
    @State private var downloadPercent = 0
    @State private var hasStartedDownload = false

    var body: some View {
        stepContent
            .background(Color(.systemGroupedBackground))
            .onChange(of: scenePhase) {
                if scenePhase == .active { viewModel.onDeviceLockRecheck() }
            }
            .task {
                for await newState in viewModel.uiState {
                    state = newState
                }
            }
            .task {
                for await downloadState in SharedGraph.shared.modelManager().modelState {
                    modelState = downloadState
                    if downloadState is ModelStateReady, state.step == .modelDownload {
                        viewModel.onDownloadContinue()
                        onFinished()
                    }
                }
            }
            .task {
                // Covers the speech model as well as the LLM, so the bar reflects
                // the total the download step actually promises.
                for await status in SharedGraph.shared.aiCoreDownloadStatus().state {
                    if let downloading = status as? AiCoreDownloadStateDownloading {
                        downloadPercent = Int(downloading.progressPercent)
                    }
                }
            }
    }

    @ViewBuilder
    private var stepContent: some View {
        switch state.step {
        case .deviceCheck:
            ProgressView()
                .frame(maxWidth: .infinity, maxHeight: .infinity)
        case .deviceIncompatible:
            incompatibleStep
        case .privacy:
            StepScaffold(
                icon: "lock.shield.fill",
                title: String(localized: "Private by design"),
                message: String(localized: "Recordings are transcribed and organized entirely on this iPhone. Nothing ever leaves your device."),
                buttonTitle: String(localized: "Continue"),
                action: { viewModel.onPrivacyContinue() }
            )
        case .noteStyle:
            StepScaffold(
                icon: "text.badge.checkmark",
                title: String(localized: "How should notes be organized?"),
                message: String(localized: "Pick a default style. You can change it for any note later."),
                buttonTitle: String(localized: "Continue"),
                action: { viewModel.onNoteStyleContinue() }
            ) {
                NotePresetPicker(selected: state.notePreset) { preset in
                    viewModel.onNoteStyleSelected(preset: preset)
                }
            }
        case .deviceLock:
            StepScaffold(
                icon: "faceid",
                title: String(localized: "Add a device lock"),
                message: String(localized: "Your notes are protected by this device's lock. Set a passcode in Settings for the strongest protection."),
                buttonTitle: String(localized: "Continue"),
                action: { viewModel.onDeviceLockSkipped() }
            )
        case .telemetryConsent:
            StepScaffold(
                icon: "chart.bar.fill",
                title: String(localized: "Share stability reports?"),
                message: String(localized: "Anonymous crash and stability data only — never your notes, audio, or any personal data."),
                buttonTitle: String(localized: "Continue"),
                action: { viewModel.onConsentContinue() }
            ) {
                Toggle(String(localized: "Share stability reports"), isOn: Binding(
                    get: { state.isTelemetryEnabled },
                    set: { viewModel.onTelemetryToggled(granted: $0) }
                ))
                .padding()
                .background(Color(.secondarySystemGroupedBackground), in: RoundedRectangle(cornerRadius: 12))
            }
        case .modelDownload:
            downloadStep
        }
    }

    private var incompatibleStep: some View {
        StepScaffold(
            icon: "iphone.slash",
            iconTint: .orange,
            title: String(localized: "This device can't run Offhand"),
            message: String(localized: "Offhand needs more memory to run its on-device AI models.")
        ) {
            if let specs = state.deviceSpecs {
                VStack(alignment: .leading, spacing: 12) {
                    specRow(
                        label: String(localized: "Memory"),
                        value: "\(specs.totalRamGb) GB / \(specs.requiredRamGb) GB",
                        satisfied: specs.isRamSatisfied
                    )
                    specRow(
                        label: String(localized: "CPU cores"),
                        value: "\(specs.cpuCores) / \(specs.requiredCpuCores)",
                        satisfied: specs.isCoresSatisfied
                    )
                }
                .padding()
                .background(Color(.secondarySystemGroupedBackground), in: RoundedRectangle(cornerRadius: 12))
            }
        }
    }

    private func specRow(label: String, value: String, satisfied: Bool) -> some View {
        HStack {
            Image(systemName: satisfied ? "checkmark.circle.fill" : "xmark.circle.fill")
                .foregroundStyle(satisfied ? .green : .red)
            Text(label)
            Spacer()
            Text(value).foregroundStyle(.secondary)
        }
    }

    private var downloadStep: some View {
        StepScaffold(
            icon: "arrow.down.circle.fill",
            title: String(localized: "Downloading the AI model"),
            message: String(format: String(localized: "One-time download of about %@ GB. Keep Offhand open on Wi-Fi."), state.downloadSizeGb)
        ) {
            downloadProgress
        }
    }

    @ViewBuilder
    private var downloadProgress: some View {
        switch onEnum(of: modelState) {
        case .error(let error):
            VStack(spacing: 12) {
                Text(error.message)
                    .foregroundStyle(.red)
                Button(String(localized: "Try again")) {
                    hasStartedDownload = false
                    startDownload()
                }
                .buttonStyle(.borderedProminent)
                .controlSize(.large)
                .tint(Brand.primary)
            }
        case .loading:
            ProgressView(String(localized: "Loading the model"))
        case .notDownloaded, .downloading, .downloaded, .ready:
            VStack(spacing: 8) {
                ProgressView(value: Double(downloadPercent), total: 100)
                    .tint(Brand.primary)
                Text("\(downloadPercent)%")
                    .font(.caption)
                    .monospacedDigit()
                    .foregroundStyle(.secondary)
            }
            .onAppear { startDownload() }
        }
    }

    private func startDownload() {
        guard !hasStartedDownload else { return }
        hasStartedDownload = true
        SharedGraph.shared.startModelDownload()
    }
}

private struct StepScaffold<Content: View>: View {
    let icon: String
    var iconTint: Color = Brand.primary
    let title: String
    let message: String
    var buttonTitle: String?
    var action: (() -> Void)?
    @ViewBuilder var content: () -> Content

    init(
        icon: String,
        iconTint: Color = Brand.primary,
        title: String,
        message: String,
        buttonTitle: String? = nil,
        action: (() -> Void)? = nil,
        @ViewBuilder content: @escaping () -> Content = { EmptyView() }
    ) {
        self.icon = icon
        self.iconTint = iconTint
        self.title = title
        self.message = message
        self.buttonTitle = buttonTitle
        self.action = action
        self.content = content
    }

    var body: some View {
        VStack(spacing: 16) {
            Spacer()
            Image(systemName: icon)
                .font(.system(size: 56))
                .foregroundStyle(iconTint)
                .padding(.bottom, 8)
            Text(title)
                .font(.title.bold())
                .multilineTextAlignment(.center)
            Text(message)
                .font(.body)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
            content()
                .padding(.top, 8)
            Spacer()
        }
        .padding(.horizontal, 24)
        .safeAreaInset(edge: .bottom) {
            if let buttonTitle, let action {
                Button(action: action) {
                    Text(buttonTitle)
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.borderedProminent)
                .controlSize(.large)
                .tint(Brand.primary)
                .padding(.horizontal, 24)
                .padding(.bottom, 8)
            }
        }
    }
}

struct NotePresetPicker: View {
    let selected: NotePreset
    let onSelect: (NotePreset) -> Void

    private let options: [(NotePreset, String, String)] = [
        (.summary, String(localized: "Summary"), "doc.plaintext"),
        (.meeting, String(localized: "Meeting notes"), "person.3"),
        (.visit, String(localized: "Visit report"), "list.clipboard"),
        (.legal, String(localized: "Legal note"), "building.columns"),
    ]

    var body: some View {
        VStack(spacing: 2) {
            ForEach(options, id: \.1) { option in
                Button {
                    onSelect(option.0)
                } label: {
                    HStack {
                        Image(systemName: option.2)
                            .foregroundStyle(Brand.primary)
                            .frame(width: 32)
                        Text(option.1)
                            .foregroundStyle(Color.primary)
                        Spacer()
                        if selected == option.0 {
                            Image(systemName: "checkmark")
                                .fontWeight(.semibold)
                                .foregroundStyle(Brand.primary)
                        }
                    }
                    .padding()
                    .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                if option.1 != options.last?.1 {
                    Divider().padding(.leading, 60)
                }
            }
        }
        .background(Color(.secondarySystemGroupedBackground), in: RoundedRectangle(cornerRadius: 12))
    }
}
