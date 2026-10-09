import AVFoundation
import Foundation
import OffhandShared

final class MicAudioSource: NSObject, IosAudioSource {
    private var audioEngine = AVAudioEngine()
    private var pendingSamples: [Int16] = []
    private let frameSamples = 800
    private var converter: AVAudioConverter?
    private static let targetFormat = AVAudioFormat(
        commonFormat: .pcmFormatInt16,
        sampleRate: 16000,
        channels: 1,
        interleaved: true
    )

    private var interruptionObserver: NSObjectProtocol?
    private var routeChangeObserver: NSObjectProtocol?
    private var configurationObserver: NSObjectProtocol?

    private var frameSink: ((KotlinShortArray) -> Void)?
    private var inputNameSink: ((String?) -> Void)?
    private var failureSink: ((String) -> Void)?
    private var isCapturing = false
    private var isRestarting = false

    private let categoryOptions: AVAudioSession.CategoryOptions = [
        .defaultToSpeaker,
        .allowBluetoothHFP,
    ]

    // A Bluetooth headset that just dropped off keeps the input unusable for
    // a second or two while the system tears the route down.
    private static let restartAttempts = 10
    private static let restartRetryDelay: TimeInterval = 0.2

    func hasPermission() -> Bool {
        AVAudioApplication.shared.recordPermission == .granted
    }

    func start(
        onFrame: @escaping (KotlinShortArray) -> Void,
        onInputChanged: @escaping (String?) -> Void,
        onFailure: @escaping (String) -> Void
    ) -> Bool {
        if Thread.isMainThread {
            return startOnMainThread(onFrame: onFrame, onInputChanged: onInputChanged, onFailure: onFailure)
        }
        var started = false
        DispatchQueue.main.sync {
            started = self.startOnMainThread(
                onFrame: onFrame,
                onInputChanged: onInputChanged,
                onFailure: onFailure
            )
        }
        return started
    }

    private func startOnMainThread(
        onFrame: @escaping (KotlinShortArray) -> Void,
        onInputChanged: @escaping (String?) -> Void,
        onFailure: @escaping (String) -> Void
    ) -> Bool {
        // stop() runs when the previous recordStream collection closes, which is
        // asynchronous, so a stop/start in quick succession can arrive here with
        // the old capture still live. Starting on top of it would install a
        // second tap on bus 0 and double-register the session observers.
        if isCapturing { stopOnMainThread() }
        frameSink = onFrame
        inputNameSink = onInputChanged
        failureSink = onFailure
        pendingSamples.removeAll()
        isCapturing = true
        observeSessionEvents()
        // Hitting record right after a headset disconnected can land while the
        // old input is still going away: the session or the engine refuses for
        // a moment, so the open is retried like a route change instead of
        // failing the recording outright.
        if reopenCapture() {
            publishInputName()
        } else {
            restartCapture(failureMessage: startFailureMessage)
        }
        return true
    }

    func stop() {
        if Thread.isMainThread {
            stopOnMainThread()
            return
        }
        DispatchQueue.main.sync { self.stopOnMainThread() }
    }

    private func stopOnMainThread() {
        isCapturing = false
        isRestarting = false
        teardown()
        clearSinks()
    }

    private func activateSession() throws {
        let session = AVAudioSession.sharedInstance()
        try session.setCategory(.playAndRecord, mode: .default, options: categoryOptions)
        try session.setActive(true)
    }

    private func deactivateSession() {
        try? AVAudioSession.sharedInstance().setActive(false, options: .notifyOthersOnDeactivation)
    }

    private func installTap() -> Bool {
        guard let frameSink else { return false }
        let inputNode = audioEngine.inputNode
        // installTap raises an Objective-C exception if bus 0 already carries a
        // tap, and Swift cannot catch that — it aborts the process. Removing
        // first is the only way to make installing safe.
        inputNode.removeTap(onBus: 0)
        let inputFormat = inputNode.outputFormat(forBus: 0)
        // Immediately after a route change the input node can report an unusable
        // format, and after a Bluetooth route change it can report a stale one.
        // installTap raises an uncatchable Objective-C exception when the format
        // passed in disagrees with the hardware, so the tap takes no explicit
        // format and the converter is built from the buffers actually delivered.
        guard inputFormat.sampleRate > 0, inputFormat.channelCount > 0 else { return false }
        inputNode.installTap(onBus: 0, bufferSize: 4096, format: nil) { [weak self] buffer, _ in
            self?.convertAndDeliver(buffer: buffer, onFrame: frameSink)
        }
        return true
    }

    private func startEngine() -> Bool {
        do {
            try audioEngine.start()
            return true
        } catch {
            audioEngine.inputNode.removeTap(onBus: 0)
            return false
        }
    }

    private func observeSessionEvents() {
        let center = NotificationCenter.default
        let session = AVAudioSession.sharedInstance()
        interruptionObserver = center.addObserver(
            forName: AVAudioSession.interruptionNotification,
            object: session,
            queue: .main
        ) { [weak self] notification in
            self?.handleInterruption(notification)
        }
        routeChangeObserver = center.addObserver(
            forName: AVAudioSession.routeChangeNotification,
            object: session,
            queue: .main
        ) { [weak self] notification in
            self?.handleRouteChange(notification)
        }
        observeConfigurationChanges()
    }

    private func observeConfigurationChanges() {
        let center = NotificationCenter.default
        configurationObserver.map(center.removeObserver)
        configurationObserver = center.addObserver(
            forName: .AVAudioEngineConfigurationChange,
            object: audioEngine,
            queue: .main
        ) { [weak self] _ in
            self?.handleConfigurationChange()
        }
    }

    private func stopObservingSessionEvents() {
        let center = NotificationCenter.default
        [interruptionObserver, routeChangeObserver, configurationObserver]
            .compactMap { $0 }
            .forEach(center.removeObserver)
        interruptionObserver = nil
        routeChangeObserver = nil
        configurationObserver = nil
    }

    private func handleInterruption(_ notification: Notification) {
        guard isCapturing, !isRestarting,
              let info = notification.userInfo,
              let rawType = info[AVAudioSessionInterruptionTypeKey] as? UInt,
              let type = AVAudioSession.InterruptionType(rawValue: rawType) else { return }
        switch type {
        case .began:
            audioEngine.pause()
        case .ended:
            // Deliberately not gated on .shouldResume: a recording that cannot be
            // resumed has to surface as a failure rather than stall silently.
            restartCapture(
                failureMessage: String(localized: "Recording stopped because another app took over the microphone.")
            )
        @unknown default:
            break
        }
    }

    private func handleRouteChange(_ notification: Notification) {
        guard isCapturing, !isRestarting else { return }
        publishInputName()
        // When the input that was feeding the tap disappears, the engine can keep
        // reporting itself as running while no buffers arrive any more, so the
        // capture is reopened on the input that replaced it.
        let inputGone = notification.routeChangeReason == .oldDeviceUnavailable
        guard inputGone || !audioEngine.isRunning else { return }
        restartCapture(failureMessage: routeFailureMessage)
    }

    private func handleConfigurationChange() {
        guard isCapturing, !isRestarting else { return }
        restartCapture(failureMessage: routeFailureMessage)
    }

    private var routeFailureMessage: String {
        String(localized: "Recording stopped because the audio input changed.")
    }

    private var startFailureMessage: String {
        String(localized: "Recording could not start. Try again in a moment.")
    }

    private func restartCapture(
        failureMessage: String,
        attemptsRemaining: Int = MicAudioSource.restartAttempts
    ) {
        guard isCapturing else { return }
        isRestarting = true
        audioEngine.inputNode.removeTap(onBus: 0)
        audioEngine.stop()
        deactivateSession()
        if reopenCapture() {
            isRestarting = false
            publishInputName()
            return
        }
        guard attemptsRemaining > 0 else {
            isRestarting = false
            reportFailure(failureMessage)
            return
        }
        // A route transition can leave the input unusable for a moment, so let it
        // settle before giving up on a recording that is still in progress.
        DispatchQueue.main.asyncAfter(deadline: .now() + MicAudioSource.restartRetryDelay) { [weak self] in
            self?.restartCapture(
                failureMessage: failureMessage,
                attemptsRemaining: attemptsRemaining - 1
            )
        }
    }

    private func reopenCapture() -> Bool {
        do {
            try activateSession()
        } catch {
            return false
        }
        replaceEngine()
        return installTap() && startEngine()
    }

    // The input node keeps the hardware format it saw when it was first used.
    // After a headset leaves, the old engine still asks for the headset's
    // 44.1 kHz on the 48 kHz built-in microphone and fails to start with
    // kAudioUnitErr_FormatNotSupported on every attempt until the process
    // restarts; a fresh engine reads the current format.
    private func replaceEngine() {
        audioEngine.inputNode.removeTap(onBus: 0)
        audioEngine.stop()
        audioEngine = AVAudioEngine()
        converter = nil
        if isCapturing { observeConfigurationChanges() }
    }

    private func publishInputName() {
        guard let port = AVAudioSession.sharedInstance().currentRoute.inputs.first,
              port.portType != .builtInMic else {
            inputNameSink?(nil)
            return
        }
        inputNameSink?(port.portName)
    }

    private func reportFailure(_ message: String) {
        guard isCapturing else { return }
        isCapturing = false
        let failure = failureSink
        teardown()
        clearSinks()
        failure?(message)
    }

    private func teardown() {
        stopObservingSessionEvents()
        audioEngine.inputNode.removeTap(onBus: 0)
        audioEngine.stop()
        deactivateSession()
    }

    private func clearSinks() {
        frameSink = nil
        inputNameSink = nil
        failureSink = nil
    }

    private func converter(from inputFormat: AVAudioFormat, to targetFormat: AVAudioFormat) -> AVAudioConverter? {
        if let converter, converter.inputFormat == inputFormat { return converter }
        let created = AVAudioConverter(from: inputFormat, to: targetFormat)
        converter = created
        return created
    }

    private func convertAndDeliver(buffer: AVAudioPCMBuffer, onFrame: (KotlinShortArray) -> Void) {
        guard let targetFormat = MicAudioSource.targetFormat,
              let converter = converter(from: buffer.format, to: targetFormat) else { return }
        let ratio = targetFormat.sampleRate / buffer.format.sampleRate
        let capacity = AVAudioFrameCount(Double(buffer.frameLength) * ratio) + 16
        guard let converted = AVAudioPCMBuffer(pcmFormat: targetFormat, frameCapacity: capacity) else { return }
        var consumed = false
        converter.convert(to: converted, error: nil) { _, outStatus in
            if consumed {
                outStatus.pointee = .noDataNow
                return nil
            }
            consumed = true
            outStatus.pointee = .haveData
            return buffer
        }
        guard let channel = converted.int16ChannelData?.pointee else { return }
        let count = Int(converted.frameLength)
        pendingSamples.append(contentsOf: UnsafeBufferPointer(start: channel, count: count))
        while pendingSamples.count >= frameSamples {
            let frame = Array(pendingSamples.prefix(frameSamples))
            pendingSamples.removeFirst(frameSamples)
            let kotlinFrame = KotlinShortArray(size: Int32(frameSamples))
            for (index, sample) in frame.enumerated() {
                kotlinFrame.set(index: Int32(index), value: sample)
            }
            onFrame(kotlinFrame)
        }
    }
}

private extension Notification {
    var routeChangeReason: AVAudioSession.RouteChangeReason? {
        guard let raw = userInfo?[AVAudioSessionRouteChangeReasonKey] as? UInt else { return nil }
        return AVAudioSession.RouteChangeReason(rawValue: raw)
    }
}
