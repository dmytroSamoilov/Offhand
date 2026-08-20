import AVFoundation
import Foundation
import OffhandShared

final class MicAudioSource: NSObject, IosAudioSource {
    private let audioEngine = AVAudioEngine()
    private var pendingSamples: [Int16] = []
    private let frameSamples = 800

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

    private static let restartAttempts = 3
    private static let restartRetryDelay: TimeInterval = 0.15

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
        frameSink = onFrame
        inputNameSink = onInputChanged
        failureSink = onFailure
        pendingSamples.removeAll()
        do {
            try activateSession()
        } catch {
            clearSinks()
            return false
        }
        guard installTap(), startEngine() else {
            deactivateSession()
            clearSinks()
            return false
        }
        isCapturing = true
        observeSessionEvents()
        publishInputName()
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
        let inputFormat = inputNode.outputFormat(forBus: 0)
        // Immediately after a route change the input node can report an unusable
        // format; building a converter from it would trap.
        guard inputFormat.sampleRate > 0, inputFormat.channelCount > 0 else { return false }
        guard let targetFormat = AVAudioFormat(
            commonFormat: .pcmFormatInt16,
            sampleRate: 16000,
            channels: 1,
            interleaved: true
        ), let converter = AVAudioConverter(from: inputFormat, to: targetFormat) else {
            return false
        }
        inputNode.installTap(onBus: 0, bufferSize: 4096, format: inputFormat) { [weak self] buffer, _ in
            self?.convertAndDeliver(
                buffer: buffer,
                converter: converter,
                targetFormat: targetFormat,
                onFrame: frameSink
            )
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
        ) { [weak self] _ in
            self?.handleRouteChange()
        }
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

    private func handleRouteChange() {
        guard isCapturing, !isRestarting else { return }
        publishInputName()
        guard !audioEngine.isRunning else { return }
        restartCapture(failureMessage: routeFailureMessage)
    }

    private func handleConfigurationChange() {
        guard isCapturing, !isRestarting else { return }
        restartCapture(failureMessage: routeFailureMessage)
    }

    private var routeFailureMessage: String {
        String(localized: "Recording stopped because the audio input changed.")
    }

    private func restartCapture(
        failureMessage: String,
        attemptsRemaining: Int = MicAudioSource.restartAttempts
    ) {
        guard isCapturing else { return }
        isRestarting = true
        audioEngine.inputNode.removeTap(onBus: 0)
        audioEngine.stop()
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
        return installTap() && startEngine()
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

    private func convertAndDeliver(
        buffer: AVAudioPCMBuffer,
        converter: AVAudioConverter,
        targetFormat: AVAudioFormat,
        onFrame: (KotlinShortArray) -> Void
    ) {
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
