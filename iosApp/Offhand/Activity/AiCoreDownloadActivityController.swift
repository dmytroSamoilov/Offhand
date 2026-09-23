import ActivityKit
import Foundation
import os

// Progress only reaches the activity while the app runs; the background
// URLSession keeps downloading after a suspend, so each update carries a
// stale date and the widget says so instead of showing a frozen percentage.
@MainActor
final class AiCoreDownloadActivityController {
    private static let staleAfter: TimeInterval = 5 * 60
    private let logger = Logger(subsystem: "com.dmytrosamoilov.offhand", category: "LiveActivity")
    private var activity: Activity<AiCoreDownloadActivityAttributes>?

    func progressed(percent: Int) {
        let state = AiCoreDownloadActivityAttributes.ContentState(phase: .downloading, progressPercent: percent)
        if let activity = activity ?? adoptExistingActivity() {
            Task { await activity.update(content(state)) }
            return
        }
        guard ActivityAuthorizationInfo().areActivitiesEnabled else { return }
        do {
            activity = try Activity.request(attributes: AiCoreDownloadActivityAttributes(), content: content(state))
        } catch {
            logger.error("Download Live Activity request failed: \(error.localizedDescription)")
        }
    }

    func finished(isReady: Bool) {
        let state = AiCoreDownloadActivityAttributes.ContentState(
            phase: isReady ? .ready : .paused,
            progressPercent: isReady ? 100 : 0
        )
        let dismissAfter: TimeInterval = isReady ? 4 : 15 * 60
        activity = nil
        for activity in Activity<AiCoreDownloadActivityAttributes>.activities {
            Task {
                await activity.end(
                    ActivityContent(state: state, staleDate: nil),
                    dismissalPolicy: .after(Date().addingTimeInterval(dismissAfter))
                )
            }
        }
    }

    // A download that outlived its process still owns the activity it started.
    private func adoptExistingActivity() -> Activity<AiCoreDownloadActivityAttributes>? {
        activity = Activity<AiCoreDownloadActivityAttributes>.activities.first
        return activity
    }

    private func content(
        _ state: AiCoreDownloadActivityAttributes.ContentState
    ) -> ActivityContent<AiCoreDownloadActivityAttributes.ContentState> {
        ActivityContent(state: state, staleDate: Date().addingTimeInterval(Self.staleAfter))
    }
}
