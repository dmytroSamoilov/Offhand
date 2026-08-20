import OffhandShared
import SwiftUI
import UIKit

struct AboutSupportView: View {
    private let viewModel = AppViewModels.aboutSupport
    @State private var state = AboutSupportUiState(
        selectedBackend: .cpu,
        model: ModelUi(displayName: "", sizeGb: "", status: .notDownloaded, downloadPercent: 0, errorMessage: nil),
        modelOptions: [],
        selectedModelId: nil,
        isTelemetryEnabled: false,
        isDeveloperSectionVisible: false,
        isDeveloperOptionsEnabled: false,
        isDeleteModelConfirmationVisible: false
    )

    var body: some View {
        Form {
            modelSection
            privacySection
            feedbackSection
            legalSection
            if state.isDeveloperSectionVisible {
                developerSection
            }
        }
        .navigationTitle(String(localized: "About & Support"))
        .navigationBarTitleDisplayMode(.inline)
        .confirmationDialog(
            String(localized: "Delete the AI model?"),
            isPresented: deleteBinding,
            titleVisibility: .visible
        ) {
            Button(String(localized: "Delete"), role: .destructive) { viewModel.onDeleteModelConfirmed() }
            Button(String(localized: "Cancel"), role: .cancel) { viewModel.onDeleteModelDismissed() }
        } message: {
            Text(String(localized: "Your notes are kept. Offhand will need to download the model again before it can process a new recording."))
        }
        .task {
            for await newState in viewModel.uiState {
                state = newState
            }
        }
    }

    private var modelSection: some View {
        Section {
            LabeledContent(String(localized: "Model"), value: state.model.displayName)
            LabeledContent(
                String(localized: "Size"),
                value: String(format: String(localized: "%@ GB"), state.model.sizeGb)
            )
            modelStatusRow
            if let message = state.model.errorMessage {
                Text(message)
                    .font(.footnote)
                    .foregroundStyle(.red)
            }
        } header: {
            Text(String(localized: "On-device AI"))
        } footer: {
            Text(String(localized: "The model runs entirely on this iPhone. Deleting it frees the space but pauses processing until it is downloaded again."))
        }
    }

    @ViewBuilder
    private var modelStatusRow: some View {
        switch state.model.status {
        case .ready:
            Button(String(localized: "Delete model"), role: .destructive) {
                viewModel.onDeleteModelRequested()
            }
        case .downloading:
            VStack(alignment: .leading, spacing: 6) {
                Text(String(localized: "Downloading"))
                ProgressView(value: Double(state.model.downloadPercent), total: 100)
                    .tint(Brand.primary)
                Text("\(state.model.downloadPercent)%")
                    .font(.caption)
                    .monospacedDigit()
                    .foregroundStyle(.secondary)
            }
        case .loading:
            LabeledContent(String(localized: "Status"), value: String(localized: "Loading"))
        case .notDownloaded, .error:
            Button(String(localized: "Download model")) { viewModel.onDownloadModel() }
        default:
            EmptyView()
        }
    }

    private var privacySection: some View {
        Section {
            Toggle(
                String(localized: "Share stability reports"),
                isOn: Binding(
                    get: { state.isTelemetryEnabled },
                    set: { viewModel.onTelemetryChanged(granted: $0) }
                )
            )
        } header: {
            Text(String(localized: "Privacy"))
        } footer: {
            Text(String(localized: "Anonymous crash and stability data only — never your notes, audio, or any personal data."))
        }
    }

    private var feedbackSection: some View {
        Section(String(localized: "Feedback")) {
            Button(String(localized: "Send feedback")) { openFeedbackMail() }
        }
    }

    private var legalSection: some View {
        Section(String(localized: "Legal")) {
            Button(String(localized: "Terms & Conditions")) { open(Links.terms) }
            Button(String(localized: "Privacy Policy")) { open(Links.privacy) }
            LabeledContent(String(localized: "Version"), value: Diagnostics.appVersion)
        }
    }

    private var developerSection: some View {
        Section(String(localized: "Developer")) {
            Toggle(
                String(localized: "Developer options"),
                isOn: Binding(
                    get: { state.isDeveloperOptionsEnabled },
                    set: { viewModel.onDeveloperOptionsChanged(enabled: $0) }
                )
            )
        }
    }

    private var deleteBinding: Binding<Bool> {
        Binding(
            get: { state.isDeleteModelConfirmationVisible },
            set: { isShown in if !isShown { viewModel.onDeleteModelDismissed() } }
        )
    }

    private func open(_ raw: String) {
        guard let url = URL(string: raw) else { return }
        UIApplication.shared.open(url)
    }

    // Opens the composer pre-filled; the user still sends it themselves.
    private func openFeedbackMail() {
        var components = URLComponents(string: "mailto:\(Links.feedbackEmail)")
        components?.queryItems = [
            URLQueryItem(name: "subject", value: String(localized: "Offhand feedback")),
            URLQueryItem(name: "body", value: "\n\n\(Diagnostics.summary)"),
        ]
        guard let url = components?.url else { return }
        UIApplication.shared.open(url)
    }

    private enum Links {
        static let feedbackEmail = "dmytro@dmytrosamoilov.com"
        static let terms = "https://dmytrosamoilov.com/offhand/terms-and-conditions"
        static let privacy = "https://dmytrosamoilov.com/offhand/privacy-policy"
    }

    private enum Diagnostics {
        static var appVersion: String {
            Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "—"
        }

        static var summary: String {
            let build = Bundle.main.infoDictionary?["CFBundleVersion"] as? String ?? "—"
            let device = UIDevice.current
            return """
            ---
            Offhand \(appVersion) (\(build))
            \(device.systemName) \(device.systemVersion)
            """
        }
    }
}
