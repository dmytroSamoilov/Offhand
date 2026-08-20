import AVFoundation
import OffhandShared
import SwiftUI
import UIKit

struct RecordSheetView: View {
    var autoStart = false
    @Environment(\.dismiss) private var dismiss
    private let viewModel = AppViewModels.recording
    @State private var state = RecordingUiState(
        phase: .idle,
        isPaused: false,
        elapsedTime: "00:00",
        waveform: [],
        isSilent: true,
        chunks: [],
        failureMessage: nil,
        savedNoteId: nil,
        isDeveloperMode: false,
        externalMicName: nil
    )
    @State private var isPermissionDenied = false
    @State private var isDiscardConfirmationVisible = false

    var body: some View {
        VStack(spacing: 24) {
            Spacer()
            if state.savedNoteId != nil {
                savedContent
            } else {
                switch state.phase {
                case .idle:
                    idleContent
                case .recording:
                    recordingContent
                case .finishingTranscription:
                    finishingContent
                case .failed:
                    failedContent
                }
            }
            Spacer()
        }
        .padding()
        .presentationDetents([.medium])
        .presentationDragIndicator(.visible)
        .interactiveDismissDisabled(state.phase == .recording)
        .onAppear {
            viewModel.onSheetOpened()
            if autoStart {
                requestMicThenStart()
            }
        }
        .onDisappear { viewModel.onSheetClosed() }
        .confirmationDialog(
            String(localized: "Discard this recording?"),
            isPresented: $isDiscardConfirmationVisible,
            titleVisibility: .visible
        ) {
            Button(String(localized: "Discard"), role: .destructive) {
                viewModel.onDiscardRecording()
                dismiss()
            }
            Button(String(localized: "Keep recording"), role: .cancel) {}
        } message: {
            Text(String(localized: "The audio and everything transcribed so far will be deleted."))
        }
        .task {
            for await newState in viewModel.uiState {
                state = newState
            }
        }
    }

    private var savedContent: some View {
        VStack(spacing: 16) {
            Image(systemName: "checkmark.circle.fill")
                .font(.system(size: 56))
                .foregroundStyle(Brand.teal)
            Text(String(localized: "Note saved"))
                .font(.title3.weight(.semibold))
            Text(String(localized: "It will keep processing on this device. You can close this."))
                .font(.footnote)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
            Button(String(localized: "Done")) { dismiss() }
                .buttonStyle(.borderedProminent)
                .controlSize(.large)
                .tint(Brand.primary)
        }
    }

    @ViewBuilder
    private var idleContent: some View {
        if isPermissionDenied {
            micPermissionFallback
        } else {
            VStack(spacing: 20) {
                Text(String(localized: "Record a note"))
                    .font(.title3.weight(.semibold))
                Button {
                    requestMicThenStart()
                } label: {
                    Image(systemName: "mic.fill")
                        .font(.largeTitle)
                        .foregroundStyle(.white)
                        .frame(width: 96, height: 96)
                        .background(Brand.primary, in: Circle())
                }
                .buttonStyle(.plain)
                Text(String(localized: "Everything stays on this device."))
                    .font(.footnote)
                    .foregroundStyle(.secondary)
            }
        }
    }

    private var micPermissionFallback: some View {
        VStack(spacing: 16) {
            Image(systemName: "mic.slash.fill")
                .font(.system(size: 44))
                .foregroundStyle(.secondary)
            Text(String(localized: "Offhand needs the microphone"))
                .font(.title3.weight(.semibold))
                .multilineTextAlignment(.center)
            Text(String(localized: "Turn on microphone access to record a note. Audio still never leaves this device."))
                .font(.footnote)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
            Button(String(localized: "Open Settings")) {
                guard let url = URL(string: UIApplication.openSettingsURLString) else { return }
                UIApplication.shared.open(url)
            }
            .buttonStyle(.borderedProminent)
            .controlSize(.large)
            .tint(Brand.primary)
        }
    }

    private var recordingContent: some View {
        VStack(spacing: 20) {
            Text(state.isPaused ? String(localized: "Paused") : String(localized: "Recording"))
                .font(.subheadline.weight(.medium))
                .foregroundStyle(state.isPaused ? Color.secondary : Brand.primary)
            Text(state.elapsedTime)
                .font(.system(size: 44, weight: .semibold))
                .monospacedDigit()
            WaveformBar(levels: state.waveform.map { $0.floatValue })
                .frame(height: 56)
            if let mic = state.externalMicName {
                Label(mic, systemImage: "headphones")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            HStack(spacing: 32) {
                Button {
                    state.isPaused ? viewModel.onResumeRecording() : viewModel.onPauseRecording()
                } label: {
                    Image(systemName: state.isPaused ? "play.fill" : "pause.fill")
                        .font(.title2)
                        .frame(width: 64, height: 64)
                        .background(Color(.secondarySystemFill), in: Circle())
                }
                .buttonStyle(.plain)
                Button {
                    viewModel.onStopRecording()
                } label: {
                    Image(systemName: "stop.fill")
                        .font(.title2)
                        .foregroundStyle(.white)
                        .frame(width: 80, height: 80)
                        .background(Brand.primary, in: Circle())
                }
                .buttonStyle(.plain)
            }
            Button(String(localized: "Discard"), role: .destructive) {
                isDiscardConfirmationVisible = true
            }
            .font(.subheadline)
        }
    }

    private var finishingContent: some View {
        VStack(spacing: 16) {
            ProgressView()
                .controlSize(.large)
            Text(String(localized: "Finishing transcription"))
                .font(.headline)
            Text(String(localized: "Please keep Offhand open while your note is prepared."))
                .font(.footnote)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
        }
    }

    private var failedContent: some View {
        VStack(spacing: 16) {
            Image(systemName: "exclamationmark.triangle")
                .font(.largeTitle)
                .foregroundStyle(.orange)
            Text(state.failureMessage ?? String(localized: "Recording failed"))
                .multilineTextAlignment(.center)
            Button(String(localized: "Try again")) {
                viewModel.onStartRecording()
            }
            .buttonStyle(.borderedProminent)
            .tint(Brand.primary)
        }
    }

    private func requestMicThenStart() {
        AVAudioApplication.requestRecordPermission { granted in
            Task { @MainActor in
                isPermissionDenied = !granted
                guard granted else { return }
                viewModel.onStartRecording()
            }
        }
    }
}

private struct WaveformBar: View {
    let levels: [Float]

    var body: some View {
        HStack(alignment: .center, spacing: 3) {
            ForEach(Array(levels.suffix(40).enumerated()), id: \.offset) { _, level in
                Capsule()
                    .fill(Brand.primary)
                    .frame(width: 4, height: max(6, CGFloat(level) * 56))
            }
        }
        .frame(maxWidth: .infinity)
        .animation(.linear(duration: 0.1), value: levels)
    }
}
