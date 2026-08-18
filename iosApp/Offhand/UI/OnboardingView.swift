import OffhandShared
import SwiftUI

struct OnboardingView: View {
    let onFinished: () -> Void
    private let viewModel = SharedGraph.shared.onboardingViewModel()
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

    var body: some View {
        VStack(spacing: 24) {
            Spacer()
            stepContent
            Spacer()
        }
        .padding(24)
        .background(Color(.systemGroupedBackground))
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
    }

    @ViewBuilder
    private var stepContent: some View {
        switch state.step {
        case .deviceCheck:
            ProgressView()
        case .deviceIncompatible:
            incompatibleStep
        case .privacy:
            privacyStep
        case .noteStyle:
            noteStyleStep
        case .deviceLock:
            deviceLockStep
        case .telemetryConsent:
            telemetryStep
        case .modelDownload:
            downloadStep
        }
    }

    private var incompatibleStep: some View {
        VStack(spacing: 16) {
            Image(systemName: "iphone.slash")
                .font(.largeTitle)
                .foregroundStyle(.orange)
            Text(String(localized: "This device can't run Offhand"))
                .font(.title3.weight(.semibold))
            if let specs = state.deviceSpecs {
                VStack(alignment: .leading, spacing: 8) {
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
                .background(Color(.secondarySystemGroupedBackground), in: RoundedRectangle(cornerRadius: 16))
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

    private var privacyStep: some View {
        onboardingCard(
            icon: "lock.shield",
            title: String(localized: "Private by design"),
            body: String(localized: "Recordings are transcribed and organized entirely on this iPhone. Nothing ever leaves your device."),
            buttonTitle: String(localized: "Continue")
        ) {
            viewModel.onPrivacyContinue()
        }
    }

    private var noteStyleStep: some View {
        VStack(spacing: 20) {
            Text(String(localized: "How should notes be organized?"))
                .font(.title3.weight(.semibold))
            NotePresetPicker(selected: state.notePreset) { preset in
                viewModel.onNoteStyleSelected(preset: preset)
            }
            Button(String(localized: "Continue")) {
                viewModel.onNoteStyleContinue()
            }
            .buttonStyle(.borderedProminent)
            .tint(Brand.primary)
        }
    }

    private var deviceLockStep: some View {
        onboardingCard(
            icon: "faceid",
            title: String(localized: "Add a device lock"),
            body: String(localized: "Your notes are protected by this device's lock. Set a passcode in Settings for the strongest protection."),
            buttonTitle: String(localized: "Continue")
        ) {
            viewModel.onDeviceLockSkipped()
        }
    }

    private var telemetryStep: some View {
        VStack(spacing: 20) {
            Image(systemName: "chart.bar")
                .font(.largeTitle)
                .foregroundStyle(Brand.primary)
            Text(String(localized: "Share stability reports?"))
                .font(.title3.weight(.semibold))
            Text(String(localized: "Anonymous crash and stability data only — never your notes, audio, or any personal data."))
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
            Toggle(String(localized: "Share stability reports"), isOn: Binding(
                get: { state.isTelemetryEnabled },
                set: { viewModel.onTelemetryToggled(granted: $0) }
            ))
            .padding(.horizontal)
            Button(String(localized: "Continue")) {
                viewModel.onConsentContinue()
            }
            .buttonStyle(.borderedProminent)
            .tint(Brand.primary)
        }
    }

    private var downloadStep: some View {
        VStack(spacing: 20) {
            Image(systemName: "arrow.down.circle")
                .font(.largeTitle)
                .foregroundStyle(Brand.primary)
            Text(String(localized: "Downloading the AI model"))
                .font(.title3.weight(.semibold))
            Text(String(format: String(localized: "One-time download of about %@ GB. Keep Offhand open on Wi-Fi."), state.downloadSizeGb))
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
            downloadProgress
        }
    }

    @ViewBuilder
    private var downloadProgress: some View {
        switch onEnum(of: modelState) {
        case .downloading(let downloading):
            VStack(spacing: 8) {
                ProgressView(value: downloading.progress)
                    .tint(Brand.primary)
                Text("\(Int(downloading.progress * 100))%")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
        case .error(let error):
            VStack(spacing: 12) {
                Text(error.message)
                    .foregroundStyle(.red)
                Button(String(localized: "Try again")) { startDownload() }
                    .buttonStyle(.borderedProminent)
                    .tint(Brand.primary)
            }
        case .loading:
            ProgressView(String(localized: "Loading the model"))
        case .ready, .downloaded:
            ProgressView()
        case .notDownloaded:
            Button(String(localized: "Download")) { startDownload() }
                .buttonStyle(.borderedProminent)
                .tint(Brand.primary)
                .onAppear { startDownload() }
        }
    }

    private func startDownload() {
        SharedGraph.shared.startModelDownload()
    }

    private func onboardingCard(
        icon: String,
        title: String,
        body bodyText: String,
        buttonTitle: String,
        action: @escaping () -> Void
    ) -> some View {
        VStack(spacing: 20) {
            Image(systemName: icon)
                .font(.largeTitle)
                .foregroundStyle(Brand.primary)
            Text(title)
                .font(.title3.weight(.semibold))
            Text(bodyText)
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
            Button(buttonTitle, action: action)
                .buttonStyle(.borderedProminent)
                .tint(Brand.primary)
        }
    }
}

struct NotePresetPicker: View {
    let selected: NotePreset
    let onSelect: (NotePreset) -> Void

    private let options: [(NotePreset, String, String)] = [
        (.summary, String(localized: "Summary"), "doc.text"),
        (.meeting, String(localized: "Meeting"), "person.2"),
        (.visit, String(localized: "Visit"), "stethoscope"),
        (.legal, String(localized: "File note"), "briefcase"),
    ]

    var body: some View {
        VStack(spacing: 10) {
            ForEach(options, id: \.1) { option in
                Button {
                    onSelect(option.0)
                } label: {
                    HStack {
                        Image(systemName: option.2)
                            .frame(width: 32)
                        Text(option.1)
                        Spacer()
                        if selected == option.0 {
                            Image(systemName: "checkmark")
                                .foregroundStyle(Brand.primary)
                        }
                    }
                    .padding()
                    .background(Color(.secondarySystemGroupedBackground), in: RoundedRectangle(cornerRadius: 14))
                }
                .buttonStyle(.plain)
            }
        }
    }
}
