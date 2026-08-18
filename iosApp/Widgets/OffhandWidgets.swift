import ActivityKit
import SwiftUI
import WidgetKit

@main
struct OffhandWidgetsBundle: WidgetBundle {
    var body: some Widget {
        NoteActivityWidget()
    }
}

struct NoteActivityWidget: Widget {
    var body: some WidgetConfiguration {
        ActivityConfiguration(for: NoteActivityAttributes.self) { context in
            LockScreenActivityView(state: context.state)
                .padding()
                .activityBackgroundTint(Color.black.opacity(0.55))
                .activitySystemActionForegroundColor(.white)
        } dynamicIsland: { context in
            DynamicIsland {
                DynamicIslandExpandedRegion(.leading) {
                    PhaseGlyph(state: context.state)
                        .font(.title2)
                }
                DynamicIslandExpandedRegion(.trailing) {
                    TrailingStatus(state: context.state)
                        .font(.title3.monospacedDigit())
                }
                DynamicIslandExpandedRegion(.bottom) {
                    ExpandedBottom(state: context.state)
                }
            } compactLeading: {
                PhaseGlyph(state: context.state)
            } compactTrailing: {
                TrailingStatus(state: context.state)
                    .frame(maxWidth: 60)
            } minimal: {
                PhaseGlyph(state: context.state)
            }
        }
    }
}

private struct PhaseGlyph: View {
    let state: NoteActivityAttributes.ContentState

    var body: some View {
        switch state.phase {
        case .recording:
            Image(systemName: "mic.fill").foregroundStyle(.red)
        case .paused:
            Image(systemName: "pause.fill").foregroundStyle(.orange)
        case .processing:
            Image(systemName: "waveform").foregroundStyle(.blue)
        case .finished:
            Image(systemName: "checkmark.circle.fill").foregroundStyle(.green)
        case .openApp:
            Image(systemName: "exclamationmark.circle.fill").foregroundStyle(.orange)
        }
    }
}

private struct TrailingStatus: View {
    let state: NoteActivityAttributes.ContentState

    var body: some View {
        switch state.phase {
        case .recording:
            Text(timerInterval: state.startedAt...state.startedAt.addingTimeInterval(6 * 60 * 60), countsDown: false)
                .multilineTextAlignment(.trailing)
        case .paused:
            Text(String(localized: "Paused"))
        case .processing:
            if let percent = state.progressPercent {
                Text("\(percent)%")
            } else {
                ProgressView().tint(.white)
            }
        case .finished:
            Text(String(localized: "Done"))
        case .openApp:
            EmptyView()
        }
    }
}

private struct ExpandedBottom: View {
    let state: NoteActivityAttributes.ContentState

    var body: some View {
        switch state.phase {
        case .recording, .paused:
            Text(String(localized: "Recording a note"))
                .font(.subheadline)
                .foregroundStyle(.secondary)
        case .processing:
            VStack(alignment: .leading, spacing: 6) {
                Text(String(localized: "Preparing your note"))
                    .font(.subheadline)
                ProgressView(value: Double(state.progressPercent ?? 0), total: 100)
                    .tint(.blue)
            }
        case .finished:
            Text(String(localized: "Your note is ready"))
                .font(.subheadline)
        case .openApp:
            Text(String(localized: "Open Offhand to finish preparing your note"))
                .font(.subheadline)
        }
    }
}

private struct LockScreenActivityView: View {
    let state: NoteActivityAttributes.ContentState

    var body: some View {
        HStack(spacing: 12) {
            PhaseGlyph(state: state)
                .font(.title2)
            VStack(alignment: .leading, spacing: 4) {
                switch state.phase {
                case .recording, .paused:
                    Text(String(localized: "Recording a note"))
                        .font(.headline)
                case .processing:
                    Text(String(localized: "Preparing your note"))
                        .font(.headline)
                    ProgressView(value: Double(state.progressPercent ?? 0), total: 100)
                        .tint(.blue)
                case .finished:
                    Text(String(localized: "Your note is ready"))
                        .font(.headline)
                case .openApp:
                    Text(String(localized: "Open Offhand to finish preparing your note"))
                        .font(.headline)
                }
            }
            Spacer()
            TrailingStatus(state: state)
                .font(.title3.monospacedDigit())
        }
        .foregroundStyle(.white)
    }
}
