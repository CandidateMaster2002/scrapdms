package com.company.grc.dto;


import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PanGstr7DataResponse {

    private String panNumber;
    private String companyName;
    private Long categoryId;
    private String categoryName;
    private List<String> hsnCodes;
    private Boolean isApplicable;
    private List<GstinData> gstins;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GstinData {
        private String gstin;
        private String gstr7Status;
        private Integer gstr7DelayCount;
        private Integer gstr7MissedCount;
        private LocalDateTime gstr7LastUpdated;
        private String gstType;
        private String gstdNo;
        private String tradeName;
        private String legalName;
        private LocalDateTime createdAt;
    }
}
