import ActivityKit
import SwiftUI
import WidgetKit

@main
struct OffhandWidgetsBundle: WidgetBundle {
    init() {
        OffhandFonts.register()
    }

    var body: some Widget {
        NoteActivityWidget()
        AiCoreDownloadActivityWidget()
        RecordWidget()
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
                        .font(.offhand(.monoRegular, size: 20, relativeTo: .title3))
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
            Image(systemName: "mic.fill").foregroundStyle(OffhandColor.record)
        case .paused:
            Image(systemName: "pause.fill").foregroundStyle(OffhandColor.accent)
        case .processing:
            Image(systemName: "waveform").foregroundStyle(OffhandColor.accent)
        case .finished:
            Image(systemName: "checkmark.circle.fill").foregroundStyle(OffhandColor.accent)
        case .pausedBySystem:
            Image(systemName: "pause.circle.fill").foregroundStyle(OffhandColor.accent)
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
        case .pausedBySystem:
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
                .offhandText(.bodySmall)
                .foregroundStyle(.secondary)
        case .processing:
            VStack(alignment: .leading, spacing: 6) {
                Text(String(localized: "Preparing your note"))
                    .offhandText(.bodySmall)
                ProgressView(value: Double(state.progressPercent ?? 0), total: 100)
                    .tint(OffhandColor.accent)
            }
        case .finished:
            Text(String(localized: "Your note is ready"))
                .offhandText(.bodySmall)
        case .pausedBySystem:
            VStack(alignment: .leading, spacing: 2) {
                Text(String(localized: "Your note is paused"))
                    .offhandText(.bodySmall)
                Text(String(localized: "Open Offhand to finish it"))
                    .offhandText(.caption)
                    .foregroundStyle(.secondary)
            }
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
                        .offhandText(.rowTitle)
                case .processing:
                    Text(String(localized: "Preparing your note"))
                        .offhandText(.rowTitle)
                    ProgressView(value: Double(state.progressPercent ?? 0), total: 100)
                        .tint(OffhandColor.accent)
                case .finished:
                    Text(String(localized: "Your note is ready"))
                        .offhandText(.rowTitle)
                case .pausedBySystem:
                    Text(String(localized: "Your note is paused"))
                        .offhandText(.rowTitle)
                    Text(String(localized: "Open Offhand to finish it"))
                        .offhandText(.bodySmall)
                        .foregroundStyle(.secondary)
                }
            }
            Spacer()
            TrailingStatus(state: state)
                .font(.offhand(.monoRegular, size: 20, relativeTo: .title3))
        }
        .foregroundStyle(.white)
    }
}

struct AiCoreDownloadActivityWidget: Widget {
    var body: some WidgetConfiguration {
        ActivityConfiguration(for: AiCoreDownloadActivityAttributes.self) { context in
            DownloadLockScreenView(state: context.state, isStale: context.isStale)
                .padding()
                .activityBackgroundTint(Color.black.opacity(0.55))
                .activitySystemActionForegroundColor(.white)
        } dynamicIsland: { context in
            DynamicIsland {
                DynamicIslandExpandedRegion(.leading) {
                    DownloadGlyph(state: context.state)
                        .font(.title2)
                }
                DynamicIslandExpandedRegion(.trailing) {
                    DownloadTrailing(state: context.state)
                        .font(.offhand(.monoRegular, size: 20, relativeTo: .title3))
                }
                DynamicIslandExpandedRegion(.bottom) {
                    DownloadExpandedBottom(state: context.state, isStale: context.isStale)
                }
            } compactLeading: {
                DownloadGlyph(state: context.state)
            } compactTrailing: {
                DownloadTrailing(state: context.state)
                    .frame(maxWidth: 60)
            } minimal: {
                DownloadGlyph(state: context.state)
            }
        }
    }
}

private struct DownloadGlyph: View {
    let state: AiCoreDownloadActivityAttributes.ContentState

    var body: some View {
        switch state.phase {
        case .downloading:
            Image(systemName: "arrow.down.circle.fill").foregroundStyle(OffhandColor.accent)
        case .ready:
            Image(systemName: "checkmark.circle.fill").foregroundStyle(OffhandColor.accent)
        case .paused:
            Image(systemName: "pause.circle.fill").foregroundStyle(OffhandColor.accent)
        }
    }
}

private struct DownloadTrailing: View {
    let state: AiCoreDownloadActivityAttributes.ContentState

    var body: some View {
        switch state.phase {
        case .downloading:
            Text("\(state.progressPercent)%")
        case .ready:
            Text(String(localized: "Done"))
        case .paused:
            EmptyView()
        }
    }
}

private struct DownloadTitle: View {
    let state: AiCoreDownloadActivityAttributes.ContentState

    var body: some View {
        switch state.phase {
        case .downloading:
            Text(String(localized: "Setting up your on-device AI"))
        case .ready:
            Text(String(localized: "Your on-device AI is ready"))
        case .paused:
            Text(String(localized: "Open Offhand to finish setting up your on-device AI"))
        }
    }
}

private struct DownloadProgressBar: View {
    let state: AiCoreDownloadActivityAttributes.ContentState
    let isStale: Bool

    var body: some View {
        if state.phase == .downloading {
            ProgressView(value: Double(state.progressPercent), total: 100)
                .tint(OffhandColor.accent)
            if isStale {
                Text(String(localized: "Continuing in the background"))
                    .offhandText(.caption)
                    .foregroundStyle(.secondary)
            }
        }
    }
}

private struct DownloadExpandedBottom: View {
    let state: AiCoreDownloadActivityAttributes.ContentState
    let isStale: Bool

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            DownloadTitle(state: state)
                .offhandText(.bodySmall)
            DownloadProgressBar(state: state, isStale: isStale)
        }
    }
}

private struct DownloadLockScreenView: View {
    let state: AiCoreDownloadActivityAttributes.ContentState
    let isStale: Bool

    var body: some View {
        HStack(spacing: 12) {
            DownloadGlyph(state: state)
                .font(.title2)
            VStack(alignment: .leading, spacing: 4) {
                DownloadTitle(state: state)
                    .offhandText(.rowTitle)
                DownloadProgressBar(state: state, isStale: isStale)
            }
            Spacer()
            DownloadTrailing(state: state)
                .font(.offhand(.monoRegular, size: 20, relativeTo: .title3))
        }
        .foregroundStyle(.white)
    }
}
