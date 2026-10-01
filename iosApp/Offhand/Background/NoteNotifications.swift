import Foundation
import UIKit
import UserNotifications

// Android pings the user when a note finishes and opens that note when the
// notification is tapped. This is the iOS half of that loop. Nothing is posted
// while the app is in front: the note itself shows what happened there.
final class NoteNotifications: NSObject, ObservableObject, UNUserNotificationCenterDelegate {

    static let shared = NoteNotifications()

    @Published var noteIdToOpen: Int64?

    private static let noteIdKey = "noteId"

    func register() {
        UNUserNotificationCenter.current().delegate = self
    }

    func requestPermission(completion: @escaping () -> Void) {
        UNUserNotificationCenter.current()
            .requestAuthorization(options: [.alert, .sound]) { _, _ in
                DispatchQueue.main.async { completion() }
            }
    }

    @MainActor
    func noteReady(noteId: Int64) {
        post(
            noteId: noteId,
            title: String(localized: "Your note is ready"),
            body: String(localized: "Tap to read it.")
        )
    }

    @MainActor
    func noteFailed(noteId: Int64) {
        post(
            noteId: noteId,
            title: String(localized: "Your note didn't finish"),
            body: String(localized: "Tap to open it and try again.")
        )
    }

    @MainActor
    func notePaused(noteId: Int64) {
        post(
            noteId: noteId,
            title: String(localized: "Your note is paused"),
            body: String(localized: "Open Offhand and it will finish.")
        )
    }

    // Shown the moment the app leaves with unfinished work, the way Threads
    // asks to stay open while a post uploads; cleared on the next activation.
    @MainActor
    func remindToKeepOpen() {
        let center = UNUserNotificationCenter.current()
        center.requestAuthorization(options: [.alert, .sound]) { granted, _ in
            guard granted else { return }
            let content = UNMutableNotificationContent()
            content.title = String(localized: "Your note isn't finished")
            content.body = String(localized: "Open Offhand and keep it open until it's done.")
            let trigger = UNTimeIntervalNotificationTrigger(timeInterval: 3, repeats: false)
            center.add(UNNotificationRequest(identifier: Self.pendingIdentifier, content: content, trigger: trigger))
        }
    }

    @MainActor
    func clearKeepOpenReminder() {
        let center = UNUserNotificationCenter.current()
        center.removePendingNotificationRequests(withIdentifiers: [Self.pendingIdentifier])
        center.removeDeliveredNotifications(withIdentifiers: [Self.pendingIdentifier])
    }

    static let pendingIdentifier = "note-pending"

    @MainActor
    private func post(noteId: Int64, title: String, body: String) {
        guard UIApplication.shared.applicationState != .active else { return }
        let center = UNUserNotificationCenter.current()
        center.requestAuthorization(options: [.alert, .sound]) { granted, _ in
            guard granted else { return }
            let content = UNMutableNotificationContent()
            content.title = title
            content.body = body
            content.userInfo = [Self.noteIdKey: NSNumber(value: noteId)]
            // Deliver immediately: a triggered request stays pending, and pending
            // requests get cleared when the app next becomes active.
            center.add(
                UNNotificationRequest(
                    identifier: "note-ready-\(noteId)",
                    content: content,
                    trigger: nil
                )
            )
        }
    }

    nonisolated func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        let noteId = (response.notification.request.content.userInfo[Self.noteIdKey] as? NSNumber)?
            .int64Value
        Task { @MainActor in
            if let noteId {
                Self.shared.noteIdToOpen = noteId
            }
            completionHandler()
        }
    }

    // A notification that lands while the app is in front has nothing to add,
    // the note on screen shows the same thing. One that fires during the
    // hand-off to the background (the keep-open reminder) still has to show.
    nonisolated func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        Task { @MainActor in
            let isInFront = UIApplication.shared.applicationState == .active
            completionHandler(isInFront ? [] : [.banner, .list, .sound])
        }
    }
}
