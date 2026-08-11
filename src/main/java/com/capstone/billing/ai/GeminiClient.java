package com.capstone.billing.ai;

import com.capstone.billing.config.AiProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Thin Google Gemini generateContent client (low-cost flash models).
 * API key comes from application.properties / GEMINI_API_KEY only.
 */
@Component
public class GeminiClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiClient.class);

    /** Tried in order when the configured model returns 404 (deprecated / closed to new users). */
    private static final List<String> FALLBACK_MODELS = List.of(
            "gemini-3.1-flash-lite",
            "gemini-3.5-flash-lite",
            "gemini-3.5-flash",
            "gemini-3.6-flash",
            "gemini-3-flash-preview",
            "gemini-flash-latest"
    );

    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public GeminiClient(AiProperties aiProperties, ObjectMapper objectMapper) {
        this.aiProperties = aiProperties;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder().build();
    }

    public boolean isAvailable() {
        return aiProperties.useGemini();
    }

    public Optional<String> generateText(String systemPrompt, String userPrompt) {
        if (!isAvailable()) {
            return Optional.empty();
        }

        String prompt = systemPrompt + "\n\n" + userPrompt;
        Map<String, Object> body = new HashMap<>();
        body.put("contents", List.of(Map.of(
                "parts", List.of(Map.of("text", prompt))
        )));
        body.put("generationConfig", Map.of(
                "temperature", 0.35,
                "maxOutputTokens", 1024
        ));

        for (String model : candidateModels()) {
            try {
                String url = aiProperties.getGemini().getBaseUrl()
                        + "/models/" + model
                        + ":generateContent?key=" + aiProperties.getGemini().getApiKey();

                String response = restClient.post()
                        .uri(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(body)
                        .retrieve()
                        .body(String.class);

                Optional<String> text = extractText(response);
                if (text.isPresent()) {
                    if (!model.equals(aiProperties.getGemini().getModel())) {
                        log.info("Gemini responded using fallback model {}", model);
                    }
                    return text;
                }
            } catch (RestClientResponseException ex) {
                int code = ex.getStatusCode().value();
                log.warn("Gemini API error {} for model {}: {}", code, model, truncate(ex.getResponseBodyAsString()));
                if (code == 404) {
                    continue; // try next model
                }
                return Optional.empty();
            } catch (Exception ex) {
                log.warn("Gemini call failed for model {}: {}", model, ex.getMessage());
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    public Optional<JsonNode> generateJson(String systemPrompt, String userPrompt) {
        String jsonInstruction = systemPrompt
                + "\nRespond with ONLY valid minified JSON. No markdown fences.";
        return generateText(jsonInstruction, userPrompt).flatMap(this::parseJson);
    }

    private List<String> candidateModels() {
        Set<String> models = new LinkedHashSet<>();
        String configured = aiProperties.getGemini().getModel();
        if (configured != null && !configured.isBlank()) {
            models.add(configured.trim());
        }
        models.addAll(FALLBACK_MODELS);
        return new ArrayList<>(models);
    }

    private Optional<String> extractText(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode text = root.path("candidates").path(0).path("content").path("parts").path(0).path("text");
            if (text.isMissingNode() || text.asText().isBlank()) {
                return Optional.empty();
            }
            return Optional.of(text.asText().trim());
        } catch (Exception ex) {
            log.warn("Could not parse Gemini response: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    private Optional<JsonNode> parseJson(String text) {
        try {
            String cleaned = text.trim();
            if (cleaned.startsWith("```")) {
                cleaned = cleaned.replaceAll("^```(?:json)?\\s*", "").replaceAll("\\s*```$", "");
            }
            int start = cleaned.indexOf('{');
            int end = cleaned.lastIndexOf('}');
            if (start >= 0 && end > start) {
                cleaned = cleaned.substring(start, end + 1);
            }
            return Optional.of(objectMapper.readTree(cleaned));
        } catch (Exception ex) {
            log.warn("Gemini JSON parse failed: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    private static String truncate(String body) {
        if (body == null) {
            return "";
        }
        return body.length() > 300 ? body.substring(0, 300) + "…" : body;
    }
}
