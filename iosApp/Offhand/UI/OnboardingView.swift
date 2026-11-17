import OffhandShared
import SwiftUI

struct OnboardingView: View {
    let onFinished: () -> Void
    private let viewModel = AppViewModels.onboarding
    @State private var state = OnboardingUiState(step: .deviceCheck, deviceSpecs: nil)

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
        case .welcome:
            welcome
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
                stageRow(String(localized: "Recorded"))
                Hairline()
                stageRow(String(localized: "Transcribed"))
                Hairline()
                stageRow(String(localized: "Summarised"))
            }
            .padding(.top, OffhandSpacing.screen)
            Text(String(localized: "Everything stays on your iPhone. Nothing is uploaded, ever."))
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

    private func headline(_ text: String) -> some View {
        Text(text)
            .offhandText(.display)
            .foregroundStyle(OffhandColor.ink)
            .accessibilityAddTraits(.isHeader)
            .padding(.top, 56)
    }

    private func stageRow(_ label: String) -> some View {
        HStack {
            Text(label)
                .offhandText(.body)
                .foregroundStyle(OffhandColor.ink)
            Spacer()
            MetaText(String(localized: "On your iPhone"))
        }
        .frame(minHeight: 52)
    }

    private var incompatible: some View {
        VStack(alignment: .leading, spacing: 0) {
            headline(String(localized: "This iPhone can't run Offhand"))
            Text(String(localized: "Offhand needs more memory to run its on-device AI."))
                .offhandText(.body)
                .foregroundStyle(OffhandColor.ink2)
                .padding(.top, 12)
            if let specs = state.deviceSpecs {
                VStack(spacing: 0) {
                    specRow(
                        label: String(localized: "Memory"),
                        value: "\(specs.totalRamGb) GB / \(specs.requiredRamGb) GB",
                        satisfied: specs.isRamSatisfied
                    )
                    Hairline()
                    specRow(
                        label: String(localized: "CPU cores"),
                        value: "\(specs.cpuCores) / \(specs.requiredCpuCores)",
                        satisfied: specs.isCoresSatisfied
                    )
                }
                .padding(.top, OffhandSpacing.screen)
            }
        }
    }

    private func specRow(label: String, value: String, satisfied: Bool) -> some View {
        HStack {
            Text(label)
                .offhandText(.body)
                .foregroundStyle(OffhandColor.ink)
            Spacer()
            Text(value)
                .offhandText(.number)
                .foregroundStyle(satisfied ? OffhandColor.ink2 : OffhandColor.danger)
        }
        .frame(minHeight: 52)
    }
}
