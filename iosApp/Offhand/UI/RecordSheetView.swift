import AVFoundation
import OffhandShared
import SwiftUI
import UIKit

struct RecordSheetView: View {
    var autoStart = false
    var folderId: Int64?
    var onNoteSaved: (Int64) -> Void = { _ in }
    @Environment(\.dismiss) private var dismiss
    private let viewModel = AppViewModels.recording
    @State private var state = RecordingUiState(
        phase: .idle,
        isPaused: false,
        elapsedTime: "00:00",
        elapsedMs: 0,
        waveform: [],
        isSilent: true,
        chunks: [],
        failureMessage: nil,
        savedNoteId: nil,
        isDeveloperMode: false,
        externalMicName: nil,
        isNotificationPromptRequested: false,
        isLiveTranscriptShown: false,
        liveTranscript: "",
        folderName: nil
    )
    @State private var isPermissionDenied = false
    @State private var isDiscardConfirmationVisible = false
    @State private var hasSavedNote = false
    @State private var wasSessionActive = false

    var body: some View {
        content
            .padding(.horizontal, OffhandSpacing.screen)
            .padding(.top, OffhandSpacing.card)
            .padding(.bottom, 28)
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .foregroundStyle(OffhandColor.ink)
            .offhandDialog(isPresented: $isDiscardConfirmationVisible) { discardDialog }
            .environment(\.colorScheme, .dark)
            .presentationDetents([.large])
            .presentationDragIndicator(.hidden)
            .presentationCornerRadius(OffhandRadius.sheet)
            .presentationBackground { OffhandColor.ground.environment(\.colorScheme, .dark) }
            .interactiveDismissDisabled(state.phase == .recording)
            .onAppear {
                viewModel.onSheetOpened()
                if autoStart { requestMicThenStart() }
            }
            .onDisappear { viewModel.onSheetClosed() }
            .onChange(of: state.savedNoteId) {
                guard wasSessionActive, !hasSavedNote, let noteId = state.savedNoteId?.int64Value else { return }
                hasSavedNote = true
                onNoteSaved(noteId)
                dismiss()
            }
            .onChange(of: state.phase) {
                switch state.phase {
                case .recording, .finishingTranscription:
                    wasSessionActive = true
                case .idle where wasSessionActive && !hasSavedNote:
                    dismiss()
                default:
                    break
                }
            }
            .task {
                for await newState in viewModel.uiState {
                    state = newState
                }
            }
    }

    @ViewBuilder
    private var content: some View {
        if hasSavedNote {
            Color.clear
        } else {
            switch state.phase {
            case .idle: centered { idleContent }
            case .recording: recordingContent
            case .finishingTranscription: centered { finishingContent }
            case .failed: centered { failedContent }
            }
        }
    }

    private func centered<Content: View>(@ViewBuilder _ content: () -> Content) -> some View {
        VStack(spacing: 0) {
            Spacer()
            content()
            Spacer()
        }
    }

    private var discardDialog: some View {
        OffhandDialog(
            title: String(localized: "Discard this recording?"),
            message: String(
                format: String(localized: "%@ of audio and its transcript will be deleted. This cannot be undone."),
                RecordingDuration.spoken(milliseconds: state.elapsedMs)
            ),
            confirmLabel: String(localized: "Keep recording"),
            onConfirm: { isDiscardConfirmationVisible = false },
            destructiveLabel: String(localized: "Discard"),
            onDestructive: discard,
            onDismiss: { isDiscardConfirmationVisible = false }
        )
        .environment(\.colorScheme, .dark)
    }

    private func discard() {
        Haptics.warning()
        isDiscardConfirmationVisible = false
        viewModel.onDiscardRecording()
        dismiss()
    }

    private func askForNotificationsIfPending() {
        guard viewModel.isNotificationPromptPending else { return }
        NoteNotifications.shared.requestPermissionIfUndetermined { granted in
            if let granted {
                viewModel.onNotificationPromptAnswered(granted: granted)
            } else {
                viewModel.onNotificationPromptSkipped()
            }
        }
    }

    @ViewBuilder
    private var idleContent: some View {
        if isPermissionDenied {
            micPermissionFallback
        } else {
            VStack(spacing: 20) {
                Text(String(localized: "Record a note"))
                    .offhandText(.heading)
                RecordPill(String(localized: "Start recording"), action: requestMicThenStart)
                privacyLine
            }
        }
    }

    private var micPermissionFallback: some View {
        VStack(spacing: 14) {
            Image(systemName: "mic.slash")
                .font(.system(size: 32, weight: .regular))
                .foregroundStyle(OffhandColor.ink2)
            Text(String(localized: "Offhand needs the microphone"))
                .offhandText(.heading)
            Text(String(localized: "Turn on microphone access to record a note. Audio still never leaves your iPhone."))
                .offhandText(.dialogBody)
                .foregroundStyle(OffhandColor.ink2)
            PrimaryButton(String(localized: "Open Settings"), fillsWidth: true) {
                guard let url = URL(string: UIApplication.openSettingsURLString) else { return }
                UIApplication.shared.open(url)
            }
            .padding(.top, 10)
        }
        .multilineTextAlignment(.center)
    }

    private var recordingContent: some View {
        VStack(spacing: 0) {
            header
            VStack(spacing: 12) {
                Text(state.elapsedTime)
                    .offhandText(.timer)
                    .lineLimit(1)
                    .minimumScaleFactor(0.5)
                privacyLine
            }
            .padding(.top, 48)
            RecordingWaveform(levels: state.waveform.map { CGFloat($0.floatValue) })
                .frame(height: 120)
                .padding(.top, 40)
            if state.isLiveTranscriptShown {
                liveTranscript.padding(.top, 36)
            }
            if state.isDeveloperMode && !state.chunks.isEmpty {
                ChunkPills(chunks: state.chunks).padding(.top, 12)
            }
            if let mic = state.externalMicName {
                Label(mic, systemImage: "headphones")
                    .offhandText(.caption)
                    .foregroundStyle(OffhandColor.ink2)
                    .padding(.top, 12)
            }
            Spacer(minLength: 24)
            footer
        }
    }

    private var header: some View {
        HStack(spacing: 12) {
            HStack(spacing: 10) {
                Circle()
                    .fill(state.isPaused ? OffhandColor.ink2 : OffhandColor.record)
                    .frame(width: 8, height: 8)
                    .accessibilityHidden(true)
                Text(state.isPaused ? String(localized: "Paused") : String(localized: "Recording"))
                    .offhandText(.label)
                    .foregroundStyle(OffhandColor.ink)
            }
            Spacer(minLength: 0)
            if let folderName = state.folderName {
                Label(folderName, systemImage: "folder")
                    .offhandText(.caption, face: .interMedium)
                    .foregroundStyle(OffhandColor.ink2)
                    .lineLimit(1)
            }
        }
        .frame(minHeight: OffhandSpacing.touchTarget)
        .animation(OffhandMotion.state, value: state.isPaused)
    }

    private var privacyLine: some View {
        Label(String(localized: "Stays on your iPhone"), systemImage: "lock")
            .offhandText(.caption)
            .foregroundStyle(OffhandColor.ink2)
    }

    private var liveTranscript: some View {
        VStack(alignment: .leading, spacing: 10) {
            SectionLabel(String(localized: "Live transcript"))
            Text(state.liveTranscript.isEmpty ? String(localized: "Your words appear here as you speak.") : state.liveTranscript)
                .offhandText(.body)
                .foregroundStyle(state.liveTranscript.isEmpty ? OffhandColor.ink3 : OffhandColor.ink)
                .lineLimit(3, reservesSpace: true)
                .frame(maxWidth: .infinity, alignment: .leading)
        }
    }

    private var footer: some View {
        VStack(spacing: 8) {
            HStack(alignment: .top, spacing: 16) {
                pauseResumeButton
                PrimaryButton(String(localized: "Save note"), systemImage: "checkmark", fillsWidth: true, height: 64) {
                    Haptics.confirm()
                    viewModel.onStopRecording()
                    askForNotificationsIfPending()
                }
            }
            PlainTextButton(String(localized: "Discard recording"), tone: .danger) {
                isDiscardConfirmationVisible = true
            }
        }
    }

    private var pauseResumeButton: some View {
        Button {
            Haptics.tap()
            state.isPaused ? viewModel.onResumeRecording() : viewModel.onPauseRecording()
        } label: {
            VStack(spacing: 6) {
                Image(systemName: state.isPaused ? "play.fill" : "pause.fill")
                    .font(.system(size: 22, weight: .regular))
                    .foregroundStyle(OffhandColor.ink)
                    .frame(width: 64, height: 64)
                    .overlay { Circle().strokeBorder(OffhandColor.wave, lineWidth: 1.5) }
                    .contentShape(Circle())
                Text(state.isPaused ? String(localized: "Resume") : String(localized: "Pause"))
                    .offhandText(.caption, face: .interMedium)
                    .foregroundStyle(OffhandColor.ink2)
            }
        }
        .buttonStyle(.offhandPress)
        .accessibilityLabel(state.isPaused ? String(localized: "Resume recording") : String(localized: "Pause recording"))
    }

    private var finishingContent: some View {
        VStack(spacing: 14) {
            Text(String(localized: "Finishing transcription"))
                .offhandText(.heading)
            ProgressLine(progress: nil, thickness: 3)
                .frame(maxWidth: 200)
            Text(String(localized: "Please keep Offhand open while your note is prepared."))
                .offhandText(.dialogBody)
                .foregroundStyle(OffhandColor.ink2)
            if state.isDeveloperMode && !state.chunks.isEmpty {
                ChunkPills(chunks: state.chunks)
            }
        }
        .multilineTextAlignment(.center)
    }

    private var failedContent: some View {
        VStack(spacing: 14) {
            Text(state.failureMessage ?? String(localized: "Recording failed"))
                .offhandText(.heading)
                .multilineTextAlignment(.center)
            PrimaryButton(String(localized: "Try again"), fillsWidth: true) {
                viewModel.onStartRecording(folderId: folderId.map { KotlinLong(value: $0) })
            }
            .padding(.top, 10)
        }
    }

    private func requestMicThenStart() {
        AVAudioApplication.requestRecordPermission { granted in
            Task { @MainActor in
                isPermissionDenied = !granted
                guard granted else { return }
                viewModel.onStartRecording(folderId: folderId.map { KotlinLong(value: $0) })
            }
        }
    }
}

private enum RecordingDuration {
    static func spoken(milliseconds: Int64) -> String {
        let seconds = Int(milliseconds / 1000)
        let formatter = DateComponentsFormatter()
        formatter.unitsStyle = .full
        formatter.allowedUnits = seconds >= 3600 ? [.hour, .minute, .second] : [.minute, .second]
        formatter.zeroFormattingBehavior = .dropLeading
        return formatter.string(from: TimeInterval(seconds)) ?? ""
    }
}

private struct RecordingWaveform: View {
    let levels: [CGFloat]

    private static let step: CGFloat = 8
    private static let lineWidth: CGFloat = 3
    private static let recentBars = 12
    private static let minLevel: CGFloat = 0.06

    var body: some View {
        Canvas { context, size in
            let count = Int(size.width / Self.step)
            let shown = Array(levels.suffix(count))
            let offset = count - shown.count
            for (index, level) in shown.enumerated() {
                let x = CGFloat(offset + index) * Self.step + Self.step / 2
                let height = max(Self.minLevel, min(level, 1)) * size.height
                var path = Path()
                path.move(to: CGPoint(x: x, y: (size.height - height) / 2))
                path.addLine(to: CGPoint(x: x, y: (size.height + height) / 2))
                let isRecent = index >= shown.count - Self.recentBars
                context.stroke(
                    path,
                    with: .color(isRecent ? OffhandColor.ink : OffhandColor.wave),
                    style: StrokeStyle(lineWidth: Self.lineWidth, lineCap: .round)
                )
            }
        }
        .accessibilityHidden(true)
    }
}

private struct ChunkPills: View {
    let chunks: [ChunkUi]

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 6) {
                ForEach(chunks, id: \.id) { chunk in
                    Text(verbatim: "#\(chunk.id)")
                        .font(.offhandMonoSmall)
                        .padding(.horizontal, 7)
                        .padding(.vertical, 3)
                        .background(background(for: chunk.state), in: Capsule())
                        .foregroundStyle(foreground(for: chunk.state))
                }
            }
            .padding(.horizontal, 2)
        }
    }

    private func background(for state: ChunkStateUi) -> Color {
        switch state {
        case .done, .transcribing: return OffhandColor.accentSoft
        case .failed: return OffhandColor.danger.opacity(0.18)
        default: return OffhandColor.well
        }
    }

    private func foreground(for state: ChunkStateUi) -> Color {
        switch state {
        case .done: return OffhandColor.accent
        case .failed: return OffhandColor.danger
        case .transcribing: return OffhandColor.ink
        default: return OffhandColor.ink2
        }
    }
}
