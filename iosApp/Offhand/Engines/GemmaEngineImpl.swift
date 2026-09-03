import Foundation
import LiteRTLM
import OffhandShared

final class GemmaEngineImpl: NSObject, IosGemmaEngine {
    private var engine: Engine?
    private var conversationConfig: ConversationConfig?

    func __load(
        modelPath: String,
        useGpu: Bool,
        maxTokens: Int32,
        temperature: Float,
        topK: Int32,
        topP: Float
    ) async throws {
        let config = try EngineConfig(
            modelPath: modelPath,
            backend: useGpu ? .gpu : .cpu(),
            visionBackend: nil,
            audioBackend: nil,
            maxNumTokens: Int(maxTokens),
            cacheDir: NSTemporaryDirectory()
        )
        let engine = Engine(engineConfig: config)
        try await engine.initialize()
        self.engine = engine
        let sampler = try SamplerConfig(
            topK: Int(topK),
            topP: topP,
            temperature: temperature
        )
        conversationConfig = ConversationConfig(samplerConfig: sampler)
    }

    func __generate(systemPrompt: String, userText: String) async throws -> String {
        guard let engine, let conversationConfig else {
            throw OffhandEngineError.notLoaded
        }
        let conversation = try await engine.createConversation(
            with: ConversationConfig(
                systemMessage: Message(systemPrompt, role: .system),
                samplerConfig: conversationConfig.samplerConfig
            )
        )
        let response = try await conversation.sendMessage(Message(userText))
        return response.toString
    }

    func unload() {
        conversationConfig = nil
        engine = nil
    }
}

enum OffhandEngineError: Error {
    case notLoaded
}
