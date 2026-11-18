import OffhandShared
import SwiftUI

struct OnboardingView: View {
    let onFinished: () -> Void
    private let viewModel = AppViewModels.onboarding
    @State private var state = OnboardingUiState(step: .deviceCheck, deviceSpecs: nil, isSummaryAvailable: true)

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                Text(verbatim: "Offhand")
                    .font(.offhand(.tightSemibold, size: 17, relativeTo: .headline))
                    .foregroundStyle(OffhandColor.ink)
                    .frame(minHeight: OffhandSpacing.touchTarget)
                content
            }
            .padding(.horizontal, OffhandSpacing.screen)
            .padding(.bottom, 28)
            .frame(maxWidth: 560)
            .frame(maxWidth: .infinity)
            .containerRelativeFrame(.vertical, alignment: .top) { height, _ in height }
        }
        .scrollBounceBehavior(.basedOnSize)
        .background(OffhandColor.ground.ignoresSafeArea())
        .task {
            for await newState in viewModel.uiState {
                state = newState
            }
        }
    }

    @ViewBuilder
    private var content: some View {
        switch state.step {
        case .analyticsConsent:
            analyticsConsent
        case .welcome:
            welcome
        case .transcriptsOnly:
            transcriptsOnly
        case .deviceIncompatible:
            incompatible
        default:
            Skeleton()
                .padding(.top, 56)
        }
    }

    private var welcome: some View {
        VStack(alignment: .leading, spacing: 0) {
            headline(String(localized: "Speak. Offhand writes it down, on your iPhone."))
            VStack(spacing: 0) {
                stageRow(String(localized: "Recorded"), meta: String(localized: "On your iPhone"))
                Hairline()
                stageRow(String(localized: "Transcribed"), meta: String(localized: "On your iPhone"))
                Hairline()
                stageRow(
                    String(localized: "Summarised"),
                    meta: state.isSummaryAvailable ? String(localized: "On your iPhone") : String(localized: "Not on this iPhone"),
                    isMuted: !state.isSummaryAvailable
                )
            }
            .padding(.top, OffhandSpacing.screen)
            Text(String(localized: "Your notes never leave your iPhone."))
                .offhandText(.bodySmall)
                .foregroundStyle(OffhandColor.ink2)
                .padding(.top, 16)
            Spacer(minLength: 40)
            RecordPill(String(localized: "Record your first note"), fillsWidth: true) {
                ShortcutRequests.shared.requestedFolder = .unsorted
                ShortcutRequests.shared.pending = .startRecording
                viewModel.onRecordFirstNoteClicked()
                onFinished()
            }
            Text(String(localized: "Offhand will ask to use your microphone."))
                .offhandText(.caption)
                .foregroundStyle(OffhandColor.ink2)
                .multilineTextAlignment(.center)
                .frame(maxWidth: .infinity)
                .padding(.top, 12)
            PlainTextButton(String(localized: "Not now, look around first")) {
                viewModel.onLookAroundClicked()
                onFinished()
            }
            .frame(maxWidth: .infinity)
        }
        .frame(maxHeight: .infinity, alignment: .top)
    }

    private var analyticsConsent: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text(String(localized: "Help make Offhand better?"))
                .offhandText(.display)
                .foregroundStyle(OffhandColor.ink)
                .fixedSize(horizontal: false, vertical: true)
                .accessibilityAddTraits(.isHeader)
                .padding(.top, OffhandSpacing.section)
            Text(String(localized: "Offhand is made by one developer. Anonymous usage numbers show me what to fix next."))
                .offhandText(.dialogBody)
                .foregroundStyle(OffhandColor.ink2)
                .fixedSize(horizontal: false, vertical: true)
                .padding(.top, 12)
            SectionLabel(String(localized: "Shared, without your name"))
                .padding(.top, OffhandSpacing.section)
                .padding(.bottom, 4)
            consentRows([
                String(localized: "Which features get used"),
                String(localized: "Crashes and errors"),
                String(localized: "iPhone model and app version"),
            ])
            SectionLabel(String(localized: "Never leaves your iPhone"), isEmphasised: true)
                .padding(.top, 20)
                .padding(.bottom, 4)
            consentRows([
                String(localized: "Your recordings"),
                String(localized: "Your transcripts and summaries"),
                String(localized: "Titles, folder names and searches"),
            ])
            Spacer(minLength: 32)
            PrimaryButton(String(localized: "Share with the developer"), fillsWidth: true) {
                viewModel.onShareAnalyticsClicked()
            }
            SecondaryButton(String(localized: "Don’t share"), fillsWidth: true) {
                viewModel.onDontShareAnalyticsClicked()
            }
            .padding(.top, 10)
            Text(String(localized: "You can change this any time in Settings."))
                .offhandText(.caption)
                .foregroundStyle(OffhandColor.ink2)
                .multilineTextAlignment(.center)
                .frame(maxWidth: .infinity)
                .padding(.top, 10)
        }
        .frame(maxHeight: .infinity, alignment: .top)
    }

    private func consentRows(_ labels: [String]) -> some View {
        VStack(spacing: 0) {
            ForEach(Array(labels.enumerated()), id: \.offset) { index, label in
                if index > 0 {
                    Hairline()
                }
                Text(label)
                    .offhandText(.body)
                    .foregroundStyle(OffhandColor.ink)
                    .frame(maxWidth: .infinity, minHeight: OffhandSpacing.touchTarget, alignment: .leading)
            }
        }
    }

    private func headline(_ text: String) -> some View {
        Text(text)
            .offhandText(.display)
            .foregroundStyle(OffhandColor.ink)
            .fixedSize(horizontal: false, vertical: true)
            .accessibilityAddTraits(.isHeader)
            .padding(.top, 56)
    }

    private func stageRow(_ label: String, meta: String, isMuted: Bool = false) -> some View {
        HStack(spacing: 12) {
            Text(label)
                .offhandText(.body)
                .foregroundStyle(isMuted ? OffhandColor.ink3 : OffhandColor.ink)
            Spacer()
            Text(meta)
                .offhandText(.meta)
                .foregroundStyle(isMuted ? OffhandColor.ink3 : OffhandColor.ink)
                .lineLimit(1)
        }
        .frame(minHeight: 52)
    }

    private var transcriptsOnly: some View {
        VStack(alignment: .leading, spacing: 0) {
            MetaText(String(localized: "About this iPhone"))
                .padding(.top, 56)
            Text(String(localized: "Every word, written down. Summaries need a bigger iPhone."))
                .offhandText(.display)
                .foregroundStyle(OffhandColor.ink)
                .accessibilityAddTraits(.isHeader)
                .padding(.top, 12)
            Text(String(localized: "Offhand records and transcribes on this iPhone. Writing a summary needs more memory than this iPhone has, so that part is switched off."))
                .offhandText(.body)
                .foregroundStyle(OffhandColor.ink2)
                .padding(.top, 16)
            VStack(spacing: 0) {
                stageRow(String(localized: "Recording and transcript"), meta: String(localized: "Included"))
                Hairline()
                stageRow(String(localized: "Folders, search, sharing"), meta: String(localized: "Included"))
                Hairline()
                stageRow(String(localized: "Summaries and writing styles"), meta: String(localized: "Not on this iPhone"), isMuted: true)
            }
            .padding(.top, 28)
            Text(String(localized: "Your notes never leave your iPhone."))
                .offhandText(.bodySmall)
                .foregroundStyle(OffhandColor.ink2)
                .padding(.top, 16)
            Spacer(minLength: 40)
            PrimaryButton(String(localized: "Continue with transcripts"), fillsWidth: true) {
                viewModel.onContinueWithTranscriptsClicked()
            }
        }
        .frame(maxHeight: .infinity, alignment: .top)
    }

    private var incompatible: some View {
        VStack(alignment: .leading, spacing: 0) {
            WaveGlyph(seed: 3)
                .padding(.top, 88)
            headline(String(localized: "Offhand can't run on this iPhone"))
            Text(String(localized: "Offhand does all its work on the iPhone itself, and that needs more memory than this one has. We would rather tell you now than give you an app that stalls."))
                .offhandText(.body)
                .foregroundStyle(OffhandColor.ink2)
                .padding(.top, 16)
            if let specs = state.deviceSpecs {
                VStack(spacing: 0) {
                    specRow(
                        label: String(localized: "This iPhone"),
                        value: String(format: String(localized: "%@ GB memory"), specs.totalRamGb),
                        isEmphasised: false
                    )
                    Hairline()
                    specRow(
                        label: String(localized: "Offhand needs"),
                        value: String(format: String(localized: "%@ GB or more"), specs.requiredRamGb),
                        isEmphasised: true
                    )
                }
                .padding(.top, 28)
            }
            Text(String(localized: "Nothing was recorded or stored on this iPhone."))
                .offhandText(.bodySmall)
                .foregroundStyle(OffhandColor.ink2)
                .padding(.top, 16)
        }
    }

    private func specRow(label: String, value: String, isEmphasised: Bool) -> some View {
        HStack {
            Text(label)
                .offhandText(.body)
                .foregroundStyle(OffhandColor.ink)
            Spacer()
            Text(value.uppercased())
                .offhandText(.number)
                .foregroundStyle(isEmphasised ? OffhandColor.ink : OffhandColor.ink2)
        }
        .frame(minHeight: 52)
    }
}
