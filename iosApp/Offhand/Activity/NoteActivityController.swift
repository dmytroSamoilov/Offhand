import ActivityKit
import Foundation
import os

@MainActor
final class NoteActivityController {
    private let logger = Logger(subsystem: "com.dmytrosamoilov.offhand", category: "LiveActivity")
    private var activity: Activity<NoteActivityAttributes>?
    private var recordingStartedAt = Date()

    func recordingStarted() {
        guard ActivityAuthorizationInfo().areActivitiesEnabled else {
            logger.error("Live Activities are disabled for this app")
            return
        }
        recordingStartedAt = Date()
        endActivity(dismissalPolicy: .immediate)
        let state = NoteActivityAttributes.ContentState(
            phase: .recording,
            startedAt: recordingStartedAt,
            progressPercent: nil
        )
        do {
            activity = try Activity.request(
                attributes: NoteActivityAttributes(),
                content: ActivityContent(state: state, staleDate: nil)
            )
        } catch {
            logger.error("Live Activity request failed: \(error.localizedDescription)")
        }
    }

    func recordingPaused(_ isPaused: Bool) {
        update(phase: isPaused ? .paused : .recording, progressPercent: nil)
    }

    func processingProgressed(percent: Int?) {
        update(phase: .processing, progressPercent: percent)
    }

    func finished() {
        endActivity(
            finalPhase: .finished,
            dismissalPolicy: .after(Date().addingTimeInterval(4))
        )
    }

    func suspendedWithPendingWork() {
        endActivity(
            finalPhase: .openApp,
            dismissalPolicy: .after(Date().addingTimeInterval(15 * 60))
        )
    }

    func cancelled() {
        endActivity(dismissalPolicy: .immediate)
    }

    private func update(phase: NoteActivityAttributes.ContentState.Phase, progressPercent: Int?) {
        guard let activity else { return }
        let state = NoteActivityAttributes.ContentState(
            phase: phase,
            startedAt: recordingStartedAt,
            progressPercent: progressPercent
        )
        Task { await activity.update(ActivityContent(state: state, staleDate: nil)) }
    }

    private func endActivity(
        finalPhase: NoteActivityAttributes.ContentState.Phase? = nil,
        dismissalPolicy: ActivityUIDismissalPolicy
    ) {
        guard let activity else { return }
        self.activity = nil
        let finalContent = finalPhase.map { phase in
            ActivityContent(
                state: NoteActivityAttributes.ContentState(
                    phase: phase,
                    startedAt: recordingStartedAt,
                    progressPercent: nil
                ),
                staleDate: nil
            )
        }
        Task { await activity.end(finalContent, dismissalPolicy: dismissalPolicy) }
    }
}
