import XCTest

// Runs on a real iPhone or iPad with the real on-device AI, started by
// testing/device-suite/run-ios.sh. The Mac plays a fixture clip through its
// speakers at PLAY_AT; the test is recording by then, saves once the clip has
// ended and hands the note's summary and transcript back as an attachment,
// which the runner scores against the fixture.
final class AcousticNoteTests: XCTestCase {

    private let app = XCUIApplication()
    private let springboard = XCUIApplication(bundleIdentifier: "com.apple.springboard")

    override func setUp() {
        continueAfterFailure = false
    }

    func testRecordedClipBecomesANote() throws {
        let run = try AcousticRun(environment: ProcessInfo.processInfo.environment)
        app.launch()
        skipOnboardingIfShown()
        XCTAssertTrue(recordButton.waitForExistence(timeout: 30), "The notes list did not open")
        XCTAssertFalse(
            app.staticTexts["Setting up your on-device AI"].exists,
            "The on-device AI is still downloading; open the app on Wi-Fi and let it finish first"
        )

        openRecordSheet()
        XCTAssertTrue(text(matching: "00:0[1-9]").waitForExistence(timeout: 20), "The recording did not start")
        XCTAssertLessThan(Date(), run.playAt, "The recording started after the clip began; raise the runner's lead time")

        let heard = liveTranscript(containingAnyOf: run.livePhrases)
        XCTAssertTrue(
            heard.waitForExistence(timeout: run.clipEndsAt.timeIntervalSinceNow + 60),
            "The live transcript never showed any of: \(run.livePhrases)"
        )
        wait(until: run.clipEndsAt.addingTimeInterval(3))

        app.buttons["Save note"].firstMatch.tap()
        let savedAt = Date()
        allowSystemPromptIfShown()

        XCTAssertTrue(summaryIsWritten.waitForExistence(timeout: 900), "The summary was not written within 15 minutes")
        let processingMs = Int(Date().timeIntervalSince(savedAt) * 1000)
        openTab(named: "Summary")
        XCTAssertTrue(app.buttons["Copy summary"].waitForExistence(timeout: 10), "The Summary tab did not open")
        let summary = labels(of: app.staticTexts.matching(identifier: "note_text_line"))
        openTab(named: "Transcript")
        XCTAssertTrue(app.buttons["Copy transcript"].waitForExistence(timeout: 10), "The Transcript tab did not open")
        let transcript = transcriptParagraphs()

        attach(NoteExport(summary: summary, transcript: transcript, processingMs: processingMs))
        XCTAssertFalse(summary.isEmpty, "The summary is empty")
        XCTAssertFalse(transcript.isEmpty, "The transcript is empty")
    }

    // On an iPad the hidden Settings pane keeps a bottom bar of its own in
    // the hierarchy, so the pill is the Record button that can be tapped.
    private var recordButton: XCUIElement {
        let pills = app.buttons.matching(NSPredicate(format: "label == %@", "Record a note"))
        let hittable = (0..<pills.count).map { pills.element(boundBy: $0) }.first { $0.isHittable }
        return hittable ?? pills.firstMatch
    }

    // The note opens on its Transcript tab, where a finished summary shows as
    // the dot on the Summary tab; that dot is a plain element, not a button.
    private var summaryIsWritten: XCUIElement {
        app.descendants(matching: .any)
            .matching(NSPredicate(format: "label IN %@", ["Summary is ready", "Copy summary"]))
            .firstMatch
    }

    // A tap that lands while the app is still settling after launch is lost,
    // so the pill is tapped until the sheet with its Save button is there.
    private func openRecordSheet() {
        let saveButton = app.buttons["Save note"].firstMatch
        for _ in 0..<4 where !saveButton.exists {
            recordButton.tap()
            allowSystemPromptIfShown()
            _ = saveButton.waitForExistence(timeout: 4)
        }
        XCTAssertTrue(saveButton.exists, "The record sheet did not open")
    }

    // Never resets the app: that would delete the downloaded on-device AI.
    private func skipOnboardingIfShown() {
        let lookAround = app.buttons["Not now, look around first"]
        let dontShare = app.buttons.matching(NSPredicate(format: "label MATCHES %@", "Don.t share")).firstMatch
        let deadline = Date().addingTimeInterval(8)
        while Date() < deadline && !recordButton.exists {
            if dontShare.exists { dontShare.tap() }
            if lookAround.exists { lookAround.tap() }
            usleep(300_000)
        }
    }

    // The microphone and notification questions are system alerts, outside the app.
    private func allowSystemPromptIfShown() {
        let allow = springboard.alerts.buttons.matching(NSPredicate(format: "label IN %@", ["Allow", "OK"])).firstMatch
        if allow.waitForExistence(timeout: 2) { allow.tap() }
    }

    private func text(matching pattern: String) -> XCUIElement {
        app.staticTexts.matching(NSPredicate(format: "label MATCHES %@", pattern)).firstMatch
    }

    private func liveTranscript(containingAnyOf phrases: [String]) -> XCUIElement {
        let anyPhrase = NSCompoundPredicate(
            orPredicateWithSubpredicates: phrases.map { NSPredicate(format: "label CONTAINS[c] %@", $0) }
        )
        return app.staticTexts.matching(anyPhrase).firstMatch
    }

    private func openTab(named name: String) {
        let tab = app.buttons.matching(NSPredicate(format: "label BEGINSWITH %@", name)).firstMatch
        XCTAssertTrue(tab.waitForExistence(timeout: 10), "No \(name) tab")
        tab.tap()
    }

    // A timed paragraph is one button whose label starts with its time mark.
    private func transcriptParagraphs() -> String {
        let timed = app.buttons.matching(identifier: "note_transcript_part")
        if timed.count == 0 { return labels(of: app.staticTexts.matching(identifier: "note_text_line")) }
        return (0..<timed.count)
            .map { timed.element(boundBy: $0).label }
            .map { $0.replacingOccurrences(of: "^[0-9:]+[,\\s]+", with: "", options: .regularExpression) }
            .joined(separator: "\n\n")
    }

    private func labels(of query: XCUIElementQuery) -> String {
        (0..<query.count).map { query.element(boundBy: $0).label }.joined(separator: "\n")
    }

    private func wait(until moment: Date) {
        let seconds = moment.timeIntervalSinceNow
        if seconds > 0 { Thread.sleep(forTimeInterval: seconds) }
    }

    private func attach(_ export: NoteExport) {
        guard let data = try? JSONEncoder().encode(export) else { return }
        let attachment = XCTAttachment(data: data, uniformTypeIdentifier: "public.json")
        attachment.name = NoteExport.attachmentName
        attachment.lifetime = .keepAlways
        add(attachment)
    }
}

private struct NoteExport: Encodable {
    static let attachmentName = "note-export"

    let summary: String
    let transcript: String
    let processingMs: Int
}

// What the runner passes through xcodebuild's TEST_RUNNER_ variables.
private struct AcousticRun {
    let livePhrases: [String]
    let playAt: Date
    let clipEndsAt: Date

    init(environment: [String: String]) throws {
        guard let phrases = environment["LIVE_PHRASE"], !phrases.isEmpty,
              let playAt = environment["PLAY_AT"].flatMap(Double.init),
              let clipMs = environment["CLIP_DURATION_MS"].flatMap(Double.init) else {
            throw XCTSkip("Start this test with testing/device-suite/run-ios.sh")
        }
        livePhrases = phrases.split(separator: "|").map { $0.trimmingCharacters(in: .whitespaces) }
        self.playAt = Date(timeIntervalSince1970: playAt)
        clipEndsAt = self.playAt.addingTimeInterval(clipMs / 1000)
    }
}
