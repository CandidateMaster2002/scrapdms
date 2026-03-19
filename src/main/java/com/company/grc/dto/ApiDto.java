package com.company.grc.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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
        private LocalDateTime calculatedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GstAppDetailsResponse {
        private String gstin;
        private String gstType;
        private String tradeName;
        private String legalName;
        private java.time.LocalDate registrationDate;
        private String gstStatus;
        private String address;
        private LocalDateTime lastApiSync;
        private String aggregateTurnover;
        private Integer delayCountGstr1;
        private Integer delayCountGstr3b;

        private Integer grcScore;
        private LocalDateTime scoreCalculatedAt;
        private java.util.Map<String, java.math.BigDecimal> scoreBreakdown;
        private String updatedBy;
        private String source;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GstDetailsUpdateRequest {
        private String gstType;
        private String tradeName;
        private String legalName;
        private java.time.LocalDate registrationDate;
        private String gstStatus;
        private String address;
        private String aggregateTurnover;
        private Integer delayCountGstr1;
        private Integer delayCountGstr3b;
        private String updatedBy;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GrcScoreOverrideRequest {
        private Integer newScore;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GstFetchRequest {
        private List<String> gstins;
    }
}
