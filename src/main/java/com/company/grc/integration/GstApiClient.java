package com.company.grc.integration;

import com.company.grc.dto.ExternalGstDto;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${gst.api.access-token}")
    private String accessToken;

    public GstApiClient() {
        this.restTemplate = new RestTemplate();
    }

    public ExternalGstDto.ApiResponse fetchTaxpayerDetails(String gstin) {
        String url = "https://core.kashidigitalapis.com/gst-advance";

        // Payload
        String payload = "{\"gst\": \"" + gstin + "\"}";

        HttpHeaders headers = new HttpHeaders();
        headers.set("accessToken", accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        HttpEntity<String> entity = new HttpEntity<>(payload, headers);

        try {
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
