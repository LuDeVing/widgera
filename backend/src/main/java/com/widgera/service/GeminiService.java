package com.widgera.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.widgera.config.GeminiConfig;
import com.widgera.dto.FieldDefinition;
import com.widgera.exception.LlmException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiService {

    private final GeminiConfig geminiConfig;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s";

    public Map<String, Object> generateStructuredOutput(String prompt, List<FieldDefinition> fields, byte[] imageBytes) {
        log.info("Generating structured output with {} fields", fields.size());

        try {
            String url = String.format(GEMINI_API_URL, geminiConfig.getModel(), geminiConfig.getApiKey());

            // Build the request body
            Map<String, Object> requestBody = buildRequestBody(prompt, fields, imageBytes);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return parseResponse(response.getBody(), fields);
            } else {
                throw new LlmException("Failed to get response from Gemini API");
            }

        } catch (Exception e) {
            log.error("Error calling Gemini API", e);
            throw new LlmException("Failed to generate structured output: " + e.getMessage(), e);
        }
    }

    private Map<String, Object> buildRequestBody(String prompt, List<FieldDefinition> fields, byte[] imageBytes) {
        // Build JSON schema for structured output
        StringBuilder schemaDescription = new StringBuilder();
        schemaDescription.append("You must respond with a valid JSON object containing exactly these fields:\n");
        for (FieldDefinition field : fields) {
            schemaDescription.append(String.format("- \"%s\": %s\n", field.getName(),
                    field.getType().equals("number") ? "a number" : "a string"));
        }
        schemaDescription.append("\nRespond ONLY with the JSON object, no other text.");

        String fullPrompt = prompt + "\n\n" + schemaDescription;

        List<Map<String, Object>> parts = new ArrayList<>();

        // Add text part
        Map<String, Object> textPart = new HashMap<>();
        textPart.put("text", fullPrompt);
        parts.add(textPart);

        // Add image if provided
        if (imageBytes != null && imageBytes.length > 0) {
            Map<String, Object> imagePart = new HashMap<>();
            Map<String, Object> inlineData = new HashMap<>();
            inlineData.put("mimeType", "image/jpeg");
            inlineData.put("data", Base64.getEncoder().encodeToString(imageBytes));
            imagePart.put("inlineData", inlineData);
            parts.add(imagePart);
        }

        Map<String, Object> content = new HashMap<>();
        content.put("parts", parts);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("contents", List.of(content));

        // Add generation config for JSON output
        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("temperature", 0.1);
        generationConfig.put("topK", 1);
        generationConfig.put("topP", 1);
        generationConfig.put("maxOutputTokens", 2048);
        requestBody.put("generationConfig", generationConfig);

        return requestBody;
    }

    private Map<String, Object> parseResponse(String responseBody, List<FieldDefinition> fields) throws JsonProcessingException {
        JsonNode root = objectMapper.readTree(responseBody);

        // Navigate to the text content
        JsonNode candidates = root.path("candidates");
        if (candidates.isArray() && !candidates.isEmpty()) {
            JsonNode content = candidates.get(0).path("content");
            JsonNode parts = content.path("parts");
            if (parts.isArray() && !parts.isEmpty()) {
                String text = parts.get(0).path("text").asText();

                text = text.trim();
                if (text.startsWith("```json")) {
                    text = text.substring(7);
                }
                if (text.startsWith("```")) {
                    text = text.substring(3);
                }
                if (text.endsWith("```")) {
                    text = text.substring(0, text.length() - 3);
                }
                text = text.trim();

                // Parse the JSON response
                JsonNode jsonResponse = objectMapper.readTree(text);

                // Build result map with proper types
                Map<String, Object> result = new LinkedHashMap<>();
                for (FieldDefinition field : fields) {
                    JsonNode value = jsonResponse.path(field.getName());
                    if (field.getType().equals("number")) {
                        if (value.isNumber()) {
                            result.put(field.getName(), value.numberValue());
                        } else {
                            // Try to parse as number
                            try {
                                result.put(field.getName(), Double.parseDouble(value.asText()));
                            } catch (NumberFormatException e) {
                                result.put(field.getName(), 0);
                            }
                        }
                    } else {
                        result.put(field.getName(), value.asText());
                    }
                }

                log.info("Successfully parsed structured output: {}", result);
                return result;
            }
        }

        throw new LlmException("Could not parse Gemini API response");
    }
}
