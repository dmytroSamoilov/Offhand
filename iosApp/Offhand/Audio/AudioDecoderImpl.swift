import AVFoundation
import AudioToolbox
import Foundation
import OffhandShared

// Decodes whatever AVAudioFile opens (m4a, mp3, wav, caf, aiff, ...) and
// streams it as 16 kHz mono PCM16, the format the encrypted recordings use.
// A video container is read with AVAssetReader instead, which pulls the audio
// track and converts it on the way out, so a movie needs no extraction step.
final class AudioDecoderImpl: IosAudioDecoderBridge {
    private static let linearPcmSettings: [String: Any] = [
        AVFormatIDKey: kAudioFormatLinearPCM,
        AVSampleRateKey: 16000,
        AVNumberOfChannelsKey: 1,
        AVLinearPCMBitDepthKey: 16,
        AVLinearPCMIsFloatKey: false,
        AVLinearPCMIsBigEndianKey: false,
        AVLinearPCMIsNonInterleaved: false,
    ]
    private static let targetFormat = AVAudioFormat(
        commonFormat: .pcmFormatInt16,
        sampleRate: 16000,
        channels: 1,
        interleaved: true
    )!
    private static let framesPerRead: AVAudioFrameCount = 16_384
    private static let formatErrorCodes: Set<Int> = [
        Int(kAudioFileUnsupportedFileTypeError),
        Int(kAudioFileUnsupportedDataFormatError),
        Int(kAudioFileInvalidFileError),
        Int(kAudioFileUnsupportedPropertyError),
        Int(kAudioFormatUnsupportedDataFormatError),
    ]

    func probe(path: String) -> Int64 {
        do {
            let file = try AVAudioFile(forReading: URL(fileURLWithPath: path))
            let sampleRate = file.fileFormat.sampleRate
            guard sampleRate > 0 else { return IosAudioDecoderBridgeCompanion.shared.UNSUPPORTED }
            return Int64(Double(file.length) * 1000 / sampleRate)
        } catch {
            if let durationMs = probeAsset(path: path) { return durationMs }
            let code = (error as NSError).code
            return Self.formatErrorCodes.contains(code)
                ? IosAudioDecoderBridgeCompanion.shared.UNSUPPORTED
                : IosAudioDecoderBridgeCompanion.shared.UNREADABLE
        }
    }

    func decode(path: String, onPcm: @escaping (Data) -> Void, onProgress: @escaping (KotlinFloat) -> Void) -> Bool {
        guard let file = try? AVAudioFile(forReading: URL(fileURLWithPath: path)) else {
            return decodeAsset(path: path, onPcm: onPcm, onProgress: onProgress)
        }
        guard let converter = AVAudioConverter(from: file.processingFormat, to: Self.targetFormat),
              let input = AVAudioPCMBuffer(pcmFormat: file.processingFormat, frameCapacity: Self.framesPerRead),
              let output = AVAudioPCMBuffer(pcmFormat: Self.targetFormat, frameCapacity: Self.framesPerRead)
        else { return false }
        var convertError: NSError?
        var readError: Error?
        var isDrained = false
        while true {
            output.frameLength = 0
            let status = converter.convert(to: output, error: &convertError) { _, outStatus in
                if isDrained || file.framePosition >= file.length {
                    isDrained = true
                    outStatus.pointee = .endOfStream
                    return nil
                }
                do {
                    try file.read(into: input)
                } catch {
                    readError = error
                    outStatus.pointee = .endOfStream
                    return nil
                }
                outStatus.pointee = input.frameLength > 0 ? .haveData : .endOfStream
                return input.frameLength > 0 ? input : nil
            }
            if status == .error || convertError != nil || readError != nil { return false }
            emit(output, onPcm)
            onProgress(KotlinFloat(float: Float(file.framePosition) / Float(max(file.length, 1))))
            if status == .endOfStream || (status == .inputRanDry && isDrained) { return true }
        }
    }

    // nil when the file is not a container with an audio track either.
    private func probeAsset(path: String) -> Int64? {
        let asset = AVURLAsset(url: URL(fileURLWithPath: path))
        guard let tracks = loadSync({ try await asset.loadTracks(withMediaType: .audio) }), !tracks.isEmpty,
              let duration = loadSync({ try await asset.load(.duration) })
        else { return nil }
        return Int64(CMTimeGetSeconds(duration) * 1000)
    }

    private func decodeAsset(path: String, onPcm: (Data) -> Void, onProgress: (KotlinFloat) -> Void) -> Bool {
        let asset = AVURLAsset(url: URL(fileURLWithPath: path))
        guard let tracks = loadSync({ try await asset.loadTracks(withMediaType: .audio) }), !tracks.isEmpty,
              let duration = loadSync({ try await asset.load(.duration) }),
              let reader = try? AVAssetReader(asset: asset)
        else { return false }
        let output = AVAssetReaderAudioMixOutput(audioTracks: tracks, audioSettings: Self.linearPcmSettings)
        guard reader.canAdd(output) else { return false }
        reader.add(output)
        guard reader.startReading() else { return false }
        let totalSeconds = CMTimeGetSeconds(duration)
        while let sample = output.copyNextSampleBuffer() {
            if let data = pcmData(of: sample) { onPcm(data) }
            if totalSeconds > 0 {
                let seconds = CMTimeGetSeconds(CMSampleBufferGetPresentationTimeStamp(sample))
                onProgress(KotlinFloat(float: Float(min(seconds / totalSeconds, 1))))
            }
        }
        return reader.status == .completed
    }

    private func pcmData(of sample: CMSampleBuffer) -> Data? {
        guard let block = CMSampleBufferGetDataBuffer(sample) else { return nil }
        let length = CMBlockBufferGetDataLength(block)
        guard length > 0 else { return nil }
        var data = Data(count: length)
        let copied = data.withUnsafeMutableBytes { bytes in
            CMBlockBufferCopyDataBytes(block, atOffset: 0, dataLength: length, destination: bytes.baseAddress!)
        }
        return copied == kCMBlockBufferNoErr ? data : nil
    }

    // The bridge is synchronous (it runs on a Kotlin IO thread), AVFoundation's
    // loaders are async; this parks the thread until the value is in.
    private func loadSync<T>(_ load: @escaping @Sendable () async throws -> T) -> T? {
        let box = ResultBox<T>()
        let semaphore = DispatchSemaphore(value: 0)
        Task.detached {
            box.value = try? await load()
            semaphore.signal()
        }
        semaphore.wait()
        return box.value
    }

    private func emit(_ buffer: AVAudioPCMBuffer, _ onPcm: (Data) -> Void) {
        guard buffer.frameLength > 0, let channel = buffer.int16ChannelData else { return }
        onPcm(Data(bytes: channel[0], count: Int(buffer.frameLength) * MemoryLayout<Int16>.size))
    }
}

private final class ResultBox<T>: @unchecked Sendable {
    var value: T?
}
