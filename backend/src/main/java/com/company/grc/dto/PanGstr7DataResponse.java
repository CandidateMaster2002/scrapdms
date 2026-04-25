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
    private String hsnCode; // Added to surface existing HSN
    private boolean isApplicable;
    private List<GstinData> gstins;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GstinData {
        private String gstin;
        private String gstr7Status;
        private Integer gstr7DelayCount;
        private LocalDateTime gstr7LastUpdated;
        private String gstType; // Added to know if GSTD
        private String gstdNo;
    }
}
