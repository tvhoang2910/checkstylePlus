package com.checkstyleplus.adapters;

/**
 * Factory that detects the appropriate LLM adapter based on the endpoint.
 * It creates a concrete client (Gemini, OpenAI, Claude, etc.)
 * without exposing that logic to the caller.
 */
public class LlmClientFactory {

    public static LlmClient create(
            String apiKey,
            String endpoint,
            String model,
            Double temperature,
            Integer seed,
            Integer maxOutputTokens,
            Integer thinkingTokens
    ) {
        String lower = endpoint.toLowerCase();

        if (lower.contains("generativelanguage.googleapis.com")) {
            return new GeminiClient(apiKey, endpoint, model, temperature, seed, maxOutputTokens, thinkingTokens);

        } else if (lower.contains("api.openai.com") ||
                   lower.contains("mistral.ai") ||
                   lower.contains("localhost") ||
                   lower.contains("127.0.0.1")) {
            return new OpenAiClient(apiKey, endpoint, model, temperature, maxOutputTokens);

        } else if (lower.contains("anthropic.com")) {
            return new ClaudeClient(apiKey, endpoint, model, temperature, maxOutputTokens);

        } else if (lower.contains("localhost") || lower.contains("127.0.0.1")) {
            return new LocalModelClient(endpoint, model, temperature, maxOutputTokens);
            
        } else {
            throw new IllegalArgumentException("Unsupported LLM endpoint: " + endpoint);
        }
    }
}
