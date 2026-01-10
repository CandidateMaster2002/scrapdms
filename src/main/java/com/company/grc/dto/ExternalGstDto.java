package com.company.grc.dto;

import lombok.Data;
import java.util.List;

public class ExternalGstDto {

    @Data
    public static class ApiResponse {
        private int code;
        private String message;
        private String requestId;
        private DataPayload data;
    }

    @Data
    public static class DataPayload {
        private TaxpayerDetails taxpayerDetails;
        private TaxpayerReturnDetails taxpayerReturnDetails;
        private BusinessPlaces business_places;
        private GoodsService goods_service;
    }

    @Data
    public static class TaxpayerDetails {
        private String gstin;
        private String lgnm;
        private String tradeNam;
        private String ctb; // Legal Name of Business
        private String rgdt;
        private String sts;
        private String aggreTurnOver; // New field
        private Address contacted; // Using map for simplicity or struct? JSON says "contacted" object.
        // There is 'pradr' in business_places, 'contacted' in taxpayerDetails but
        // prompt says pradr logic used address?
        // Old Code used "pradr" inside taxpayerDetails, but new JSON has it in
        // business_places.
        // Wait, looking at JSON:
        // taxpayerDetails has { ..., "contacted": {...}, ... }
        // business_places has { "pradr": { "adr": ... } }
        // Let's align structure exactly.
    }

    @Data
    public static class Address {
        private String adr;
    }

    @Data
    public static class BusinessPlaces {
        private Address pradr; // { "adr": ... }
    }

    @Data
    public static class GoodsService {
        // ... if needed
    }

    @Data
    public static class TaxpayerReturnDetails {
        private List<FilingStatus> filingStatus;
        private GstFilingDelaySummary gst_filing_delay_summary;
    }

    @Data
    public static class GstFilingDelaySummary {
        private Integer gst_delay_count_GSTR1;
        private Integer gst_delay_count_GSTR3B;
    }

    @Data
    public static class FilingStatus {
        private String fy;
        private String taxp;
        private String rtntype;
        private String status;
        private boolean is_delayed;
        private String dof;
    }
}
