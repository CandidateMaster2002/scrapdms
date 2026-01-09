package com.company.grc.integration;

import com.company.grc.dto.ExternalGstDto;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;

@Service
public class GstApiClient {

    private final RestTemplate restTemplate;

    public GstApiClient() {
        this.restTemplate = new RestTemplate();
    }

    public ExternalGstDto.ApiResponse fetchTaxpayerDetails(String gstin) {
        String url = "https://core.kashidigitalapis.com/gst-basic";
        String accessToken = "3403a7a2dc2770f8231bcc507264540d:a834dcefe7c77edc26f342bb87f61810";

        // Payload
        String payload = "{\"gst\": \"" + gstin + "\"}"; // Simple string building, safer to use ObjectNode but this is
                                                         // fine for strict input

        HttpHeaders headers = new HttpHeaders();
        headers.set("accessToken", accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        HttpEntity<String> entity = new HttpEntity<>(payload, headers);

        try {
            // We expect the structure to match ExternalGstDto.ApiResponse (or close enough)
            // If the external API structure is different, Jackson will fail or mapped
            // fields will be null.
            // Assumption: The JSON in prompt was "Use Exactly This", so we stick to that
            // DTO.

            // Note: If the real API returns "data" directly without "code"/"message"
            // wrapper, we might need adjustment.
            // But based on common Kashi Digital APIs, they often wrap.

            ResponseEntity<ExternalGstDto.ApiResponse> response = restTemplate.postForEntity(url, entity,
                    ExternalGstDto.ApiResponse.class);

            if (response.getBody() != null) {
                return response.getBody();
            } else {
                throw new RuntimeException("Empty response from API");
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch data from External GST API: " + e.getMessage(), e);
        }
    }
}
