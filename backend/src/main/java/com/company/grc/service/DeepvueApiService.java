package com.company.grc.service;

import com.company.grc.dto.DeepvueGstDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;

@Service
public class DeepvueApiService {

    private static final String AUTHORIZE_URL = "https://production.deepvue.tech/v1/authorize";
    private static final String GSTIN_URL = "https://production.deepvue.tech/v1/verification/gstin-advanced?gstin_number=";

    @Value("${deepvue.client-id}")
    private String clientId;

    @Value("${deepvue.client-secret}")
    private String clientSecret;

    @Value("${deepvue.api-key}")
    private String apiKey;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private String cachedToken;
    private LocalDateTime tokenExpiresAt;

    public DeepvueApiService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public synchronized String getToken() {
        if (cachedToken != null && tokenExpiresAt != null && LocalDateTime.now().isBefore(tokenExpiresAt)) {
            return cachedToken;
        }
        // Fetch new token
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.set(HttpHeaders.USER_AGENT, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(form, headers);
        ResponseEntity<String> response;
        try {
            response = restTemplate.postForEntity(AUTHORIZE_URL, request, String.class);
        } catch (Exception e) {
            throw new DeepvueApiException("Failed to connect to Deepvue authorize endpoint: " + e.getMessage());
        }

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new DeepvueApiException("Failed to fetch Deepvue token: HTTP " + response.getStatusCode());
        }
        try {
            JsonNode root = objectMapper.readTree(response.getBody());
            String token = root.path("access_token").asText(null);
            if (token == null || token.isBlank()) {
                throw new DeepvueApiException("access_token missing in authorize response");
            }
            cachedToken = token;
            tokenExpiresAt = LocalDateTime.now().plusSeconds(3500); // safe margin before 3600s expiry
            return cachedToken;
        } catch (DeepvueApiException e) {
            throw e;
        } catch (Exception e) {
            throw new DeepvueApiException("Failed to parse token response: " + e.getMessage());
        }
    }

    public DeepvueGstDto.DataPayload fetchGstDetails(String gstin) {
        String token = getToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + token);
        headers.set("x-api-key", apiKey);
        headers.set(HttpHeaders.USER_AGENT, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");

        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<String> response;
        try {
            response = restTemplate.exchange(GSTIN_URL + gstin, HttpMethod.GET, request, String.class);
        } catch (Exception e) {
            throw new DeepvueApiException("HTTP error calling Deepvue GSTIN API for " + gstin + ": " + e.getMessage());
        }

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new DeepvueApiException("Deepvue API returned HTTP " + response.getStatusCode() + " for GSTIN: " + gstin);
        }

        try {
            DeepvueGstDto.ApiResponse apiResponse = objectMapper.readValue(response.getBody(), DeepvueGstDto.ApiResponse.class);
            if (!"SUCCESS".equalsIgnoreCase(apiResponse.getSubCode())) {
                throw new DeepvueApiException("Deepvue API error for " + gstin + ": " + apiResponse.getMessage());
            }
            if (apiResponse.getData() == null) {
                throw new DeepvueApiException("Deepvue API returned null data for GSTIN: " + gstin);
            }
            return apiResponse.getData();
        } catch (DeepvueApiException e) {
            throw e;
        } catch (Exception e) {
            throw new DeepvueApiException("Failed to parse Deepvue API response for " + gstin + ": " + e.getMessage());
        }
    }

    public static class DeepvueApiException extends RuntimeException {
        public DeepvueApiException(String message) {
            super(message);
        }
    }
}
