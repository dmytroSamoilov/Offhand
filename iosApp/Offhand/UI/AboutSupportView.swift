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
        isDeveloperSectionVisible: false,
        isDeveloperOptionsEnabled: false,
        isDeleteModelConfirmationVisible: false,
        isSummaryAvailable: true
    )

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                Text(String(localized: "About & support"))
                    .offhandText(.title)
                    .foregroundStyle(OffhandColor.ink)
                    .padding(.horizontal, 8)
                    .padding(.top, 8)
                    .accessibilityAddTraits(.isHeader)
                if state.isDeveloperOptionsEnabled && state.isSummaryAvailable {
                    modelGroup
                }
                feedbackGroup
                legalGroup
                if state.isDeveloperSectionVisible {
                    developerGroup
                }
            }
            .padding(.horizontal, OffhandSpacing.card)
            .padding(.bottom, OffhandSpacing.screen)
        }
        .background(OffhandColor.ground.ignoresSafeArea())
        .navigationBarTitleDisplayMode(.inline)
        .offhandDialog(isPresented: deleteBinding) {
            OffhandDialog(
                title: String(localized: "Remove the on-device AI?"),
                message: String(localized: "Your notes are kept. Offhand will need to download the on-device AI again before it can process a new recording."),
                confirmLabel: String(localized: "Cancel"),
                onConfirm: { viewModel.onDeleteModelDismissed() },
                destructiveLabel: String(localized: "Remove"),
                onDestructive: { viewModel.onDeleteModelConfirmed() },
                onDismiss: { viewModel.onDeleteModelDismissed() }
            )
        }
        .task {
            for await newState in viewModel.uiState {
                state = newState
            }
        }
    }

    private var modelGroup: some View {
        VStack(alignment: .leading, spacing: 0) {
            SettingsGroup(label: String(localized: "On-device AI")) {
                ValueRow(title: String(localized: "Model"), value: state.model.displayName)
                ValueRow(title: String(localized: "Size"), value: String(format: String(localized: "%@ GB"), state.model.sizeGb))
                modelStatusRow
                if state.model.status == .error {
                    GroupNote(text: String(localized: "Something went wrong with the on-device AI"), color: OffhandColor.danger)
                }
                if let message = state.model.errorMessage {
                    GroupNote(text: message, color: OffhandColor.ink2)
                }
            }
            GroupFooter(text: String(localized: "The on-device AI runs entirely on your iPhone. Removing it frees the space but pauses processing until it is downloaded again."))
        }
    }

    @ViewBuilder
    private var modelStatusRow: some View {
        switch state.model.status {
        case .ready:
            ActionRow(title: String(localized: "Remove on-device AI"), tone: .danger) { viewModel.onDeleteModelRequested() }
        case .downloading:
            VStack(alignment: .leading, spacing: 10) {
                HStack {
                    Text(String(localized: "Downloading"))
                        .offhandText(.settingTitle)
                        .frame(maxWidth: .infinity, alignment: .leading)
                    Text(verbatim: "\(state.model.downloadPercent)%")
                        .offhandText(.number)
                        .foregroundStyle(OffhandColor.ink2)
                }
                ProgressLine(progress: Double(state.model.downloadPercent) / 100, thickness: 3)
            }
            .padding(OffhandSpacing.card)
        case .loading:
            ValueRow(title: String(localized: "Status"), value: String(localized: "Loading"), showsDivider: false)
        case .notDownloaded, .error:
            ActionRow(title: String(localized: "Download on-device AI"), tone: .accent) { viewModel.onDownloadModel() }
        }
    }

    private var feedbackGroup: some View {
        SettingsGroup(label: String(localized: "Feedback")) {
            LinkRow(title: String(localized: "Send feedback"), showsDivider: false) { openFeedbackMail() }
        }
    }

    private var legalGroup: some View {
        SettingsGroup(label: String(localized: "Legal")) {
            LinkRow(title: String(localized: "Terms & Conditions")) { open(Links.terms) }
            LinkRow(title: String(localized: "Privacy Policy")) { open(Links.privacy) }
            ValueRow(title: String(localized: "Version"), value: Diagnostics.appVersion, showsDivider: false)
        }
    }

    private var developerGroup: some View {
        SettingsGroup(label: String(localized: "Developer")) {
            SwitchRow(
                title: String(localized: "Developer options"),
                isOn: state.isDeveloperOptionsEnabled,
                showsDivider: false
            ) { viewModel.onDeveloperOptionsChanged(enabled: $0) }
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

private struct ValueRow: View {
    let title: String
    let value: String
    var showsDivider = true

    var body: some View {
        SettingsRowLayout(title: title, subtitle: nil, showsProBadge: false, showsDivider: showsDivider) {
            Text(value)
                .offhandText(.bodySmall)
                .foregroundStyle(OffhandColor.ink2)
        }
    }
}

private struct ActionRow: View {
    let title: String
    let tone: TextButtonTone
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(title)
                .offhandText(.settingTitle)
                .foregroundStyle(tone.color)
                .padding(.horizontal, OffhandSpacing.card)
                .frame(maxWidth: .infinity, minHeight: 62, alignment: .leading)
        }
        .buttonStyle(.offhandRow)
    }
}

private struct GroupNote: View {
    let text: String
    let color: Color

    var body: some View {
        Text(text)
            .offhandText(.caption)
            .foregroundStyle(color)
            .padding(.horizontal, OffhandSpacing.card)
            .padding(.vertical, 10)
    }
}

private struct GroupFooter: View {
    let text: String

    var body: some View {
        Text(text)
            .offhandText(.caption)
            .foregroundStyle(OffhandColor.ink2)
            .padding(.horizontal, OffhandSpacing.card)
            .padding(.top, 12)
    }
}
