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
        private String gstr7FilingStatus;
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

        private String mobile;
        private String email;
        private LocalDateTime createdAt;

        // GSTR-7 specific fields
        private String gstdNo;
        private String gstr7Status;
        private Integer gstr7DelayCount;
        private Integer gstr7MissedCount;
        private LocalDateTime gstr7LastUpdated;
        private String categoryName;
        private String gstr7LastReturnPeriod;
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
        private String updatedBy;
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
