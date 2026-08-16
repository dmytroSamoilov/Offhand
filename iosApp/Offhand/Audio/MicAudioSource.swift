import AVFoundation
import Foundation
import OffhandShared

final class MicAudioSource: NSObject, IosAudioSource {
    private let audioEngine = AVAudioEngine()
    private var pendingSamples: [Int16] = []
    private let frameSamples = 800
    private var interruptionObserver: NSObjectProtocol?

    private func observeInterruptions() {
        interruptionObserver = NotificationCenter.default.addObserver(
            forName: AVAudioSession.interruptionNotification,
            object: AVAudioSession.sharedInstance(),
            queue: .main
        ) { [weak self] notification in
            guard let info = notification.userInfo,
                  let rawType = info[AVAudioSessionInterruptionTypeKey] as? UInt,
                  let type = AVAudioSession.InterruptionType(rawValue: rawType) else { return }
            if type == .began {
                self?.audioEngine.pause()
            } else if type == .ended {
                try? self?.audioEngine.start()
            }
        }
    }

    private func stopObservingInterruptions() {
        if let interruptionObserver {
            NotificationCenter.default.removeObserver(interruptionObserver)
        }
        interruptionObserver = nil
    }

    func hasPermission() -> Bool {
        AVAudioApplication.shared.recordPermission == .granted
    }

    func start(onFrame: @escaping (KotlinShortArray) -> Void) -> Bool {
        if Thread.isMainThread {
            return startOnMainThread(onFrame: onFrame)
        }
        var started = false
        DispatchQueue.main.sync {
            started = self.startOnMainThread(onFrame: onFrame)
        }
        return started
    }

    private func startOnMainThread(onFrame: @escaping (KotlinShortArray) -> Void) -> Bool {
        let session = AVAudioSession.sharedInstance()
        do {
            try session.setCategory(.playAndRecord, mode: .default, options: [.defaultToSpeaker])
            try session.setActive(true)
        } catch {
            return false
        }
        let inputNode = audioEngine.inputNode
        let inputFormat = inputNode.outputFormat(forBus: 0)
        guard let targetFormat = AVAudioFormat(
            commonFormat: .pcmFormatInt16,
            sampleRate: 16000,
            channels: 1,
            interleaved: true
        ), let converter = AVAudioConverter(from: inputFormat, to: targetFormat) else {
            return false
        }
        pendingSamples.removeAll()
        inputNode.installTap(onBus: 0, bufferSize: 4096, format: inputFormat) { [weak self] buffer, _ in
            self?.convertAndDeliver(buffer: buffer, converter: converter, targetFormat: targetFormat, onFrame: onFrame)
        }
        do {
            try audioEngine.start()
        } catch {
            inputNode.removeTap(onBus: 0)
            return false
        }
        observeInterruptions()
        return true
    }

    func stop() {
        stopObservingInterruptions()
        audioEngine.inputNode.removeTap(onBus: 0)
        audioEngine.stop()
        try? AVAudioSession.sharedInstance().setActive(false, options: .notifyOthersOnDeactivation)
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
