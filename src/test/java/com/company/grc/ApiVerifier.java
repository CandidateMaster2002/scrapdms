package com.company.grc;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;

public class ApiVerifier {

    @Test
    public void verifyApi() {
        RestTemplate restTemplate = new RestTemplate();
        String url = "https://core.kashidigitalapis.com/gst-advance";
        String accessToken = "3403a7a2dc2770f8231bcc507264540d:a834dcefe7c77edc26f342bb87f61810";
        String payload = "{\"gst\": \"20AAAAI0686D1ZA\"}"; // Using the one from prompt example

        HttpHeaders headers = new HttpHeaders();
        headers.set("accessToken", accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        HttpEntity<String> entity = new HttpEntity<>(payload, headers);

        try {
            System.out.println("Sending request to: " + url);
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            System.out.println("Response Code: " + response.getStatusCode());
            // Write to file to see raw output
            java.nio.file.Files.writeString(java.nio.file.Path.of("api_result_advance.json"), response.getBody());

            // Allow mapping test
            // ObjectMapper mapper = new ObjectMapper();
            // ExternalGstDto.ApiResponse dto = mapper.readValue(response.getBody(),
            // ExternalGstDto.ApiResponse.class);
            // System.out.println("Mapped DTO Data Present: " + (dto.getData() != null));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
