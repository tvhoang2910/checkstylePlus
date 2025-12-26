package com.checkstyleplus.adapters;

/**
 * Common interface for all LLM API clients.
 * Each client should implement how to send a prompt
 * and return a text response from its specific provider.
 */
public interface LlmClient {
    String generateResponse(String prompt) throws Exception;
}
