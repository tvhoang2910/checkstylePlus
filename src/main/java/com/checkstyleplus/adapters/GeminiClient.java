package com.checkstyleplus.adapters;

import java.net.http.*;
import java.net.URI;
import java.util.*;
import com.fasterxml.jackson.databind.*;

/**
 * Adapter for Google's Gemini API.
 */
public class GeminiClient implements LlmClient {
    private final String apiKey;
    private final String endpoint;
    private final String model;
    private final Double temperature;
    private final Integer seed;
    private final Integer maxTokens;
    private final Integer thinkingTokens;
    private static final ObjectMapper M = new ObjectMapper();

    public GeminiClient(String apiKey, String endpoint, String model,
                        Double temperature, Integer seed, Integer maxTokens, Integer thinkingTokens) {
        this.apiKey = apiKey;
        this.endpoint = endpoint;
        this.model = model;
        this.temperature = temperature;
        this.seed = seed;
        this.maxTokens = maxTokens;
        this.thinkingTokens = thinkingTokens;
    }

    @Override
    public String generateResponse(String prompt) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        Map<String, Object> body = new HashMap<>();
        body.put("contents", List.of(Map.of(
            "role", "user",
            "parts", List.of(Map.of("text", prompt))
        )));

        Map<String, Object> genCfg = new HashMap<>();
        if (temperature != null) genCfg.put("temperature", temperature);
        if (seed != null) genCfg.put("seed", seed);
        if (maxTokens != null) genCfg.put("maxOutputTokens", maxTokens);
        if (thinkingTokens != null) genCfg.put("thinkingTokens", thinkingTokens);
        body.put("generationConfig", genCfg);

        String url = endpoint;
        if (model != null && !endpoint.contains(model)) {
            url = endpoint.replaceAll("models/[^:]+", "models/" + model);
        }

        String json = M.writeValueAsString(body);
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url + "?key=" + apiKey))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() / 100 != 2) {
            System.err.println("Gemini API error: " + response.statusCode() + " - " + response.body());
            return null;
        }

        JsonNode root = M.readTree(response.body());
        JsonNode candidates = root.path("candidates");
        if (candidates.isArray() && candidates.size() > 0) {
            JsonNode parts = candidates.get(0).path("content").path("parts");
            if (parts.isArray() && parts.size() > 0) {
                return parts.get(0).path("text").asText("").trim();
            }
        }
        return null;
    }
}
