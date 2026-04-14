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

        // Deepvue API fields
        private Boolean apiError;
        private String dataSource; // "API", "Manual", "Error", "Pending"
        private String panNumber;
        private String promoters;

        // Admin-only fields (mobile & email — set to null for non-admin responses)
        private String mobile;
        private String email;
        private LocalDateTime createdAt;
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

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdminRefreshRequest {
        private List<String> gstins; // null or empty = refresh all non-error GSTINs
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
        // Admin-editable fields
        private String mobile;
        private String email;
        private String panNumber;
        private String promoters;
    }
}
