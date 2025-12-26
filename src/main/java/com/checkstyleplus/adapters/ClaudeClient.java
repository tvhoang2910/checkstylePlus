package com.checkstyleplus.adapters;

import java.net.http.*;
import java.net.URI;
import java.util.*;
import com.fasterxml.jackson.databind.*;

/**
 * Adapter for Anthropic Claude API.
 */
public class ClaudeClient implements LlmClient {
    private final String apiKey;
    private final String endpoint;
    private final String model;
    private final Double temperature;
    private final Integer maxTokens;
    private static final ObjectMapper M = new ObjectMapper();

    public ClaudeClient(String apiKey, String endpoint, String model,
                        Double temperature, Integer maxTokens) {
        this.apiKey = apiKey;
        this.endpoint = endpoint;
        this.model = (model != null) ? model : "claude-3";
        this.temperature = temperature;
        this.maxTokens = (maxTokens != null) ? maxTokens : 2048;
    }

    @Override
    public String generateResponse(String prompt) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("max_tokens", maxTokens);
        body.put("temperature", temperature);
        body.put("messages", List.of(Map.of("role", "user", "content", prompt)));

        String json = M.writeValueAsString(body);
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(endpoint))
            .header("Content-Type", "application/json")
            .header("x-api-key", apiKey)
            .header("anthropic-version", "2023-06-01")
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() / 100 != 2) {
            System.err.println("Claude API error: " + response.statusCode() + " - " + response.body());
            return null;
        }

        JsonNode root = M.readTree(response.body());
        return root.path("content").get(0).path("text").asText("").trim();
    }
}
