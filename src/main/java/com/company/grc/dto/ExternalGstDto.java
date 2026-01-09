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
    }

    @Data
    public static class TaxpayerDetails {
        private String gstin;
        private String lgnm;
        private String tradeNam;
        private String ctb;
        private String rgdt;
        private String sts;
        private Address pradr;
    }

    @Data
    public static class Address {
        private String adr;
    }

    @Data
    public static class TaxpayerReturnDetails {
        private List<FilingStatus> filingStatus;
    }

    @Data
    public static class FilingStatus {
        private String fy;
        private String taxp;
        private String rtntype;
        private String status;
    }
}
