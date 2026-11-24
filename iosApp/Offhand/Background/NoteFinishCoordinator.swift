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
    private static let resumeTaskIdentifier = "\(bundleId).finish.resume"
    private static let logger = os.Logger(subsystem: bundleId, category: "FinishTask")

    // Set by the root view: ends the app's own Live Activity in its paused state.
    static var onExpired: (() -> Void)?
    // How many continued-processing tasks the system is running right now.
    // While one runs, the work goes on in the background and nobody has to
    // be asked to come back; the reminder is for when it does not.
    private static var runningTaskCount = 0
    static var isContinuingInBackground: Bool { runningTaskCount > 0 }
    private var registeredIdentifiers: Set<String> = []
    private var submittedNoteIds: Set<Int64> = []
    private var legacyTaskId: UIBackgroundTaskIdentifier = .invalid

    func isManaging(noteId: Int64) -> Bool {
        submittedNoteIds.contains(noteId)
    }

    func processingStarted(noteId: Int64) {
        if #available(iOS 26.0, *), submitContinuedTask(noteId: noteId) { return }
        coverBackgroundStart()
    }

    // Work that begins while the app is already away, such as a recording
    // stopped from the lock screen, cannot get a continued-processing task:
    // the system only grants those from the foreground. It gets the classic
    // grace period and the later resume instead of nothing at all.
    private func coverBackgroundStart() {
        guard UIApplication.shared.applicationState == .background else { return }
        appEnteredBackgroundWhileProcessing()
        Self.scheduleResume()
    }

    @available(iOS 26.0, *)
    private func submitContinuedTask(noteId: Int64) -> Bool {
        if submittedNoteIds.contains(noteId) { return true }
        guard UIApplication.shared.applicationState != .background else { return false }
        let identifier = "\(Self.finishTaskPrefix).note-\(noteId)"
        guard registerIfNeeded(identifier: identifier) else { return false }
        submittedNoteIds.insert(noteId)
        let request = BGContinuedProcessingTaskRequest(
            identifier: identifier,
            title: String(localized: "Processing your note"),
            subtitle: String(localized: "Open Offhand to finish it faster")
        )
        request.strategy = .queue
        do {
            try BGTaskScheduler.shared.submit(request)
        } catch {
            Self.logger.error("Continued processing submit failed: \(error)")
            submittedNoteIds.remove(noteId)
            return false
        }
        return true
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
        runningTaskCount += 1
        defer { runningTaskCount -= 1 }
        NoteNotifications.shared.clearKeepOpenReminder()
        task.progress.totalUnitCount = TaskProgress.total
        let expired = ExpirationFlag()
        // The system shows a task ended with success: false as failed, and the
        // note is only paused, so the task is re-titled and closed quietly; the
        // app's own notification and Live Activity carry the paused state.
        task.expirationHandler = {
            expired.raise()
            task.updateTitle(String(localized: "Your note is paused"), subtitle: String(localized: "Open Offhand to finish it"))
            scheduleReopenReminder()
            scheduleResume()
            Task { @MainActor in onExpired?() }
            task.setTaskCompleted(success: true)
        }
        // Progress is also how the system knows the task is alive, and it ends
        // one that reports nothing. The note's own percent only moves between
        // steps, and one step of a long note can take minutes, so the task is
        // read on a timer and creeps forward a little while a step is running.
        var progress = TaskProgress()
        while !expired.isRaised {
            if (sessionManager.processingNoteIds.value as NSSet).count == 0 { break }
            // The bridged Kotlin Map<Long, Int> arrives with plain-NSNumber keys
            // (NSDictionary copies keys), so iterating it as [KotlinLong: KotlinInt]
            // force-casts each key and aborts. Only untyped access is safe.
            let percents = (sessionManager.noteProgress.value as NSDictionary)
                .compactMap { ($0.value as? NSNumber)?.intValue }
            task.progress.completedUnitCount = progress.advance(toPercent: percents.min() ?? 0)
            try? await Task.sleep(for: TaskProgress.interval)
        }
        if !expired.isRaised {
            task.progress.completedUnitCount = TaskProgress.total
            task.setTaskCompleted(success: true)
        }
    }

    func appEnteredBackgroundWhileProcessing() {
        if #available(iOS 26.0, *), !submittedNoteIds.isEmpty { return }
        guard legacyTaskId == .invalid else { return }
        legacyTaskId = UIApplication.shared.beginBackgroundTask(withName: "finish-note") { [weak self] in
            scheduleReopenReminder()
            Self.scheduleResume()
            Self.onExpired?()
            self?.endLegacyTask()
        }
        if legacyTaskId == .invalid {
            scheduleReopenReminder()
            Self.scheduleResume()
            Self.onExpired?()
        }
    }

    // Safety net for the system, not the user, ending the work: a processing
    // task runs the resume step later, when the device is idle and unlocked.
    // A force quit still blocks it, iOS never relaunches a force-quit app.
    func appEnteredBackgroundWithPendingWork() {
        Self.scheduleResume()
    }

    // Must run before the app finishes launching, as BGTaskScheduler requires.
    static func registerResumeTask() {
        let registered = BGTaskScheduler.shared.register(forTaskWithIdentifier: resumeTaskIdentifier, using: .main) { task in
            guard let task = task as? BGProcessingTask else {
                task.setTaskCompleted(success: false)
                return
            }
            Task { @MainActor in
                await driveResume(task: task)
            }
        }
        if !registered {
            logger.error("Resume task registration was rejected")
        }
    }

    private static func scheduleResume() {
        let request = BGProcessingTaskRequest(identifier: resumeTaskIdentifier)
        request.requiresNetworkConnectivity = false
        request.requiresExternalPower = false
        do {
            try BGTaskScheduler.shared.submit(request)
        } catch {
            logger.error("Resume task submit failed: \(error)")
        }
    }

    private static func cancelResume() {
        BGTaskScheduler.shared.cancel(taskRequestWithIdentifier: resumeTaskIdentifier)
    }

    // Notes and audio are unreadable while the device is locked (complete-unless-open
    // protection), so a locked run is handed back for a later attempt rather
    // than failing the note.
    private static func driveResume(task: BGProcessingTask) async {
        guard UIApplication.shared.isProtectedDataAvailable else {
            scheduleResume()
            task.setTaskCompleted(success: false)
            return
        }
        let sessionManager = SharedGraph.shared.sessionManager()
        let expired = ExpirationFlag()
        task.expirationHandler = {
            expired.raise()
            scheduleResume()
            task.setTaskCompleted(success: false)
        }
        do {
            try await SharedGraph.shared.resumeInterruptedNotes()
        } catch {
            logger.error("Background resume failed: \(error)")
            task.setTaskCompleted(success: false)
            return
        }
        for await ids in sessionManager.processingNoteIds {
            if expired.isRaised { return }
            if ids.isEmpty { break }
        }
        if !expired.isRaised {
            task.setTaskCompleted(success: true)
        }
    }

    func appBecameActive() {
        endLegacyTask()
    }

    func allProcessingFinished() {
        submittedNoteIds.removeAll()
        endLegacyTask()
        Self.cancelResume()
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

// The system ending the task is the pause the user will notice, so it is
// announced as one; the note is not failed and continues on the next return.
@MainActor
func scheduleReopenReminder() {
    let center = UNUserNotificationCenter.current()
    center.requestAuthorization(options: [.alert, .sound]) { granted, _ in
        guard granted else { return }
        let content = UNMutableNotificationContent()
        content.title = String(localized: "Your note is paused")
        content.body = String(localized: "Open Offhand and it will finish.")
        let trigger = UNTimeIntervalNotificationTrigger(timeInterval: 2, repeats: false)
        center.add(UNNotificationRequest(identifier: NoteNotifications.pendingIdentifier, content: content, trigger: trigger))
    }
}

// What the continued-processing task reports. It follows the note's percent
// and, while that stands still, moves one small unit per reading for up to nine
// percent (half an hour), so a long step still reads as work in progress and never as done.
struct TaskProgress {
    static let total: Int64 = 10_000
    static let interval: Duration = .seconds(2)
    private static let unitsPerPercent: Int64 = 100
    private static let maxCreepUnits: Int64 = 900
    private static let ceiling: Int64 = 9_900

    private var reported: Int64 = 0

    mutating func advance(toPercent percent: Int) -> Int64 {
        let real = Int64(max(0, min(99, percent))) * Self.unitsPerPercent
        let crept = min(reported + 1, real + Self.maxCreepUnits)
        reported = min(Self.ceiling, max(reported, real, crept, 1))
        return reported
    }
}
