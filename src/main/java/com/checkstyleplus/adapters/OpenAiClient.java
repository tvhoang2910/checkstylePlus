package com.checkstyleplus.adapters;

import java.net.http.*;
import java.net.URI;
import java.util.*;
import com.fasterxml.jackson.databind.*;

/**
 * Adapter for OpenAI and OpenAI-compatible APIs (Mistral, Ollama, vLLM, local).
 */
public class OpenAiClient implements LlmClient {
    private final String apiKey;
    private final String endpoint;
    private final String model;
    private final Double temperature;
    private final Integer maxTokens;
    private static final ObjectMapper M = new ObjectMapper();

    public OpenAiClient(String apiKey, String endpoint, String model,
                        Double temperature, Integer maxTokens) {
        this.apiKey = apiKey;
        this.endpoint = endpoint;
        this.model = (model != null) ? model : "gpt-4";
        this.temperature = temperature;
        this.maxTokens = maxTokens;
    }

    @Override
    public String generateResponse(String prompt) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("temperature", temperature);
        if (maxTokens != null) body.put("max_tokens", maxTokens);
        body.put("messages", List.of(Map.of("role", "user", "content", prompt)));

        String json = M.writeValueAsString(body);
        HttpRequest.Builder builder = HttpRequest.newBuilder()
            .uri(URI.create(endpoint))
            .header("Content-Type", "application/json");

        if (!endpoint.contains("localhost") && !endpoint.contains("127.0.0.1")) {
            builder.header("Authorization", "Bearer " + apiKey);
        }

        HttpRequest request = builder.POST(HttpRequest.BodyPublishers.ofString(json)).build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() / 100 != 2) {
            System.err.println("OpenAI-compatible API error: " + response.statusCode() + " - " + response.body());
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
