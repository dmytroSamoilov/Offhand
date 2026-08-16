import Foundation
import OffhandShared

final class WhisperEngineImpl: NSObject, IosWhisperEngine {
    private var recognizer: SherpaOnnxOfflineRecognizer?

    func __prepare(encoderPath: String, decoderPath: String, tokensPath: String) async throws {
        let whisperConfig = sherpaOnnxOfflineWhisperModelConfig(
            encoder: encoderPath,
            decoder: decoderPath,
            language: "",
            task: "transcribe"
        )
        let modelConfig = sherpaOnnxOfflineModelConfig(
            tokens: tokensPath,
            whisper: whisperConfig,
            numThreads: min(4, ProcessInfo.processInfo.processorCount),
            modelType: "whisper"
        )
        var config = sherpaOnnxOfflineRecognizerConfig(
            featConfig: sherpaOnnxFeatureConfig(sampleRate: 16000, featureDim: 80),
            modelConfig: modelConfig
        )
        recognizer = SherpaOnnxOfflineRecognizer(config: &config)
    }

    func __transcribe(wavBytes: KotlinByteArray) async throws -> String {
        guard let recognizer else { throw OffhandEngineError.notLoaded }
        let samples = wavBytes.toFloatSamples()
        let result = recognizer.decode(samples: samples, sampleRate: 16000)
        return result.text
    }

    func releaseEngine() {
        recognizer = nil
    }
}

private extension KotlinByteArray {
    func toFloatSamples() -> [Float] {
        let headerBytes: Int32 = 44
        let total = size
        guard total > headerBytes else { return [] }
        let sampleCount = Int((total - headerBytes) / 2)
        var samples = [Float](repeating: 0, count: sampleCount)
        for index in 0..<sampleCount {
            let offset = headerBytes + Int32(index * 2)
            let low = UInt16(bitPattern: Int16(get(index: offset))) & 0xff
            let high = Int16(get(index: offset + 1))
            let value = Int16(truncatingIfNeeded: (Int(high) << 8) | Int(low))
            samples[index] = Float(value) / 32768.0
        }
        return samples
    }
}
