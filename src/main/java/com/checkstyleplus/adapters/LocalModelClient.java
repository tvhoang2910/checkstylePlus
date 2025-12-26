package com.checkstyleplus.adapters;

import java.net.http.*;
import java.net.URI;
import java.util.*;
import com.fasterxml.jackson.databind.*;

/**
 * Adapter for local OpenAI-compatible models (e.g., Ollama, vLLM, LM Studio).
 * These usually run on localhost:11434 or similar and accept
 * standard OpenAI chat completion format.
 */
public class LocalModelClient implements LlmClient {
    private final String endpoint;
    private final String model;
    private final Double temperature;
    private final Integer maxTokens;
    private static final ObjectMapper M = new ObjectMapper();

    public LocalModelClient(String endpoint, String model, Double temperature, Integer maxTokens) {
        this.endpoint = endpoint;
        this.model = (model != null) ? model : "llama3";
        this.temperature = (temperature != null) ? temperature : 1.0;
        this.maxTokens = maxTokens;
    }

    @Override
    public String generateResponse(String prompt) throws Exception {
        HttpClient client = HttpClient.newHttpClient();

        // OpenAI-compatible request payload
        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("temperature", temperature);
        if (maxTokens != null) body.put("max_tokens", maxTokens);
        body.put("messages", List.of(Map.of("role", "user", "content", prompt)));

        String json = M.writeValueAsString(body);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(endpoint))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() / 100 != 2) {
            System.err.println("Local model API error: " + response.statusCode() + " - " + response.body());
            return null;
        }

        JsonNode root = M.readTree(response.body());
        JsonNode choices = root.path("choices");
        if (choices.isArray() && choices.size() > 0) {
            return choices.get(0).path("message").path("content").asText("").trim();
        }
        return null;
    }
}
