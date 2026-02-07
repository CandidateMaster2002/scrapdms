package com.company.grc.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ApiDto {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GrcRequest {
        private String gstin;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GrcResponse {
        private String gstin;
        private Integer grcScore;
        private String scoreVersion;
        private LocalDateTime calculatedAt;
    }
}
