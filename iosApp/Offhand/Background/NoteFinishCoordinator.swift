import BackgroundTasks
import Foundation
import OffhandShared
import os
import UIKit
import UserNotifications

@MainActor
final class NoteFinishCoordinator {
    // Derived from the bundle id so each flavor keeps its own task namespace and
    // stays within BGTaskSchedulerPermittedIdentifiers.
    private static let bundleId = Bundle.main.bundleIdentifier ?? "com.dmytrosamoilov.offhand"
    private static let finishTaskPrefix = "\(bundleId).finish"
    private static let logger = os.Logger(subsystem: bundleId, category: "FinishTask")

    var onLegacyExpired: (() -> Void)?
    private var registeredIdentifiers: Set<String> = []
    private var submittedNoteIds: Set<Int64> = []
    private var legacyTaskId: UIBackgroundTaskIdentifier = .invalid

    func isManaging(noteId: Int64) -> Bool {
        submittedNoteIds.contains(noteId)
    }

    func processingStarted(noteId: Int64) {
        guard #available(iOS 26.0, *) else { return }
        guard !submittedNoteIds.contains(noteId) else { return }
        let identifier = "\(Self.finishTaskPrefix).note-\(noteId)"
        guard registerIfNeeded(identifier: identifier) else { return }
        submittedNoteIds.insert(noteId)
        let request = BGContinuedProcessingTaskRequest(
            identifier: identifier,
            title: String(localized: "Preparing your note"),
            subtitle: String(localized: "Transcribing and structuring on this device")
        )
        request.strategy = .queue
        do {
            try BGTaskScheduler.shared.submit(request)
        } catch {
            Self.logger.error("Continued processing submit failed: \(error)")
            submittedNoteIds.remove(noteId)
        }
    }

    func processingEnded(noteId: Int64) {
        submittedNoteIds.remove(noteId)
    }

    @available(iOS 26.0, *)
    private func registerIfNeeded(identifier: String) -> Bool {
        guard !registeredIdentifiers.contains(identifier) else { return true }
        let registered = BGTaskScheduler.shared.register(
            forTaskWithIdentifier: identifier,
            using: .main
        ) { task in
            guard let task = task as? BGContinuedProcessingTask else {
                task.setTaskCompleted(success: false)
                return
            }
            Task { @MainActor in
                await Self.drive(task: task)
            }
        }
        if registered {
            registeredIdentifiers.insert(identifier)
        } else {
            Self.logger.error("Task registration was rejected for \(identifier)")
        }
        return registered
    }

    @available(iOS 26.0, *)
    private static func drive(task: BGContinuedProcessingTask) async {
        let sessionManager = SharedGraph.shared.sessionManager()
        task.progress.totalUnitCount = 100
        let expired = ExpirationFlag()
        task.expirationHandler = {
            expired.raise()
            scheduleReopenReminder()
            task.setTaskCompleted(success: false)
        }
        for await ids in sessionManager.processingNoteIds {
            if expired.isRaised { return }
            if ids.isEmpty { break }
            let percents = sessionManager.noteProgress.value
                .compactMap { entry -> Int? in (entry.value as? KotlinInt)?.intValue }
            let percent = percents.min() ?? 0
            task.progress.completedUnitCount = Int64(max(1, min(99, percent)))
        }
        if !expired.isRaised {
            task.progress.completedUnitCount = 100
            task.setTaskCompleted(success: true)
        }
    }

    func appEnteredBackgroundWhileProcessing() {
        if #available(iOS 26.0, *), !submittedNoteIds.isEmpty { return }
        guard legacyTaskId == .invalid else { return }
        legacyTaskId = UIApplication.shared.beginBackgroundTask(withName: "finish-note") { [weak self] in
            scheduleReopenReminder()
            self?.onLegacyExpired?()
            self?.endLegacyTask()
        }
        if legacyTaskId == .invalid {
            scheduleReopenReminder()
            onLegacyExpired?()
        }
    }

    func appBecameActive() {
        endLegacyTask()
    }

    func allProcessingFinished() {
        submittedNoteIds.removeAll()
        endLegacyTask()
    }

    private func endLegacyTask() {
        guard legacyTaskId != .invalid else { return }
        UIApplication.shared.endBackgroundTask(legacyTaskId)
        legacyTaskId = .invalid
    }
}

private final class ExpirationFlag: @unchecked Sendable {
    private let lock = NSLock()
    private var raised = false

    var isRaised: Bool {
        lock.lock()
        defer { lock.unlock() }
        return raised
    }

    func raise() {
        lock.lock()
        defer { lock.unlock() }
        raised = true
    }
}

@MainActor
func scheduleReopenReminder() {
    let center = UNUserNotificationCenter.current()
    center.requestAuthorization(options: [.alert, .sound]) { granted, _ in
        guard granted else { return }
        let content = UNMutableNotificationContent()
        content.title = String(localized: "Your note isn't finished")
        content.body = String(localized: "Open Offhand to finish preparing your note.")
        let trigger = UNTimeIntervalNotificationTrigger(timeInterval: 2, repeats: false)
        center.add(UNNotificationRequest(identifier: "note-pending", content: content, trigger: trigger))
    }
}
