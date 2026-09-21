import AVFoundation
import AudioToolbox
import Foundation
import OffhandShared

// Decodes whatever AVAudioFile opens (m4a, mp3, wav, caf, aiff, ...) and
// streams it as 16 kHz mono PCM16, the format the encrypted recordings use.
final class AudioDecoderImpl: IosAudioDecoderBridge {
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
            let code = (error as NSError).code
            return Self.formatErrorCodes.contains(code)
                ? IosAudioDecoderBridgeCompanion.shared.UNSUPPORTED
                : IosAudioDecoderBridgeCompanion.shared.UNREADABLE
        }
    }

    func decode(path: String, onPcm: @escaping (Data) -> Void, onProgress: @escaping (KotlinFloat) -> Void) -> Bool {
        guard let file = try? AVAudioFile(forReading: URL(fileURLWithPath: path)),
              let converter = AVAudioConverter(from: file.processingFormat, to: Self.targetFormat),
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

    private func emit(_ buffer: AVAudioPCMBuffer, _ onPcm: (Data) -> Void) {
        guard buffer.frameLength > 0, let channel = buffer.int16ChannelData else { return }
        onPcm(Data(bytes: channel[0], count: Int(buffer.frameLength) * MemoryLayout<Int16>.size))
    }
}
