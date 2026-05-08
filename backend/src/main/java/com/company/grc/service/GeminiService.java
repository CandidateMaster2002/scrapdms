package com.company.grc.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=";

    private static final String PROMPT_TEMPLATE = """
            You are a GST filing data extractor. Parse the following GSTR-7 filing table copied from the GST government portal.

            The table may have columns like: Financial Year, Month, Date of Filing, Status.
            Or it may have variations like: Period, Due Date, Date of Filing, Status.

            Return ONLY a valid JSON array — no markdown, no explanation, no extra text.

            Each element must have exactly these fields:
            - "returnPeriod": tax period as "YYYY-MM" (e.g. "2025-03" for March 2025, "2025-04" for April 2025)
            - "dateOfFiling": actual filing date as "YYYY-MM-DD", or null if not filed/pending/missing

            Rules:
            - Financial year like "2025-2026" + Month "April" through "December" → returnPeriod = "2025-MM"
            - Financial year like "2025-2026" + Month "January" through "March" → returnPeriod = "2026-MM"
            - If status is "Not Filed", "Pending", "-", or blank → set dateOfFiling to null
            - Date format in input may be DD/MM/YYYY — convert to YYYY-MM-DD
            - Include ALL periods found in the text, not just filed ones

            Example output:
            [{"returnPeriod":"2025-03","dateOfFiling":"2026-04-27"},{"returnPeriod":"2025-02","dateOfFiling":"2026-03-18"}]

            Table text to parse:
            """;

    public record ParsedRecord(String returnPeriod, String dateOfFiling) {}

    public List<ParsedRecord> parseFilingTable(String tableText) {
        String prompt = PROMPT_TEMPLATE + tableText;

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
                "generationConfig", Map.of("temperature", 0)
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    GEMINI_URL + apiKey, request, String.class);

            String body = response.getBody();
            String jsonText = extractJsonFromGeminiResponse(body);
            return parseJsonArray(jsonText);
        } catch (Exception e) {
            log.error("Gemini API call failed", e);
            throw new RuntimeException("AI parsing failed: " + e.getMessage());
        }
    }

    private String extractJsonFromGeminiResponse(String responseBody) throws JsonProcessingException {
        JsonNode root = objectMapper.readTree(responseBody);
        String text = root.path("candidates").get(0)
                .path("content").path("parts").get(0)
                .path("text").asText();

        // Strip markdown code fences if present
        text = text.replaceAll("(?s)```json\\s*", "").replaceAll("(?s)```\\s*", "").trim();

        // If still not a JSON array, extract first [...] block
        if (!text.startsWith("[")) {
            Pattern pattern = Pattern.compile("\\[.*]", Pattern.DOTALL);
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                text = matcher.group();
            }
        }
        return text;
    }

    private List<ParsedRecord> parseJsonArray(String json) throws JsonProcessingException {
        JsonNode array = objectMapper.readTree(json);
        List<ParsedRecord> result = new ArrayList<>();
        for (JsonNode node : array) {
            String period = node.path("returnPeriod").asText(null);
            String filing = node.path("dateOfFiling").isNull() ? null : node.path("dateOfFiling").asText(null);
            if (period != null && !period.isBlank()) {
                result.add(new ParsedRecord(period, filing));
            }
        }
        return result;
    }
}
