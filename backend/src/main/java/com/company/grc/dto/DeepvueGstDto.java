package com.company.grc.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DeepvueGstDto {

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ApiResponse {
        private int code;
        private String message;
        @JsonProperty("sub_code")
        private String subCode;
        private DataPayload data;
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DataPayload {
        private String gstin;
        @JsonProperty("pan_number")
        private String panNumber;
        @JsonProperty("business_name")
        private String businessName;
        @JsonProperty("legal_name")
        private String legalName;
        @JsonProperty("constitution_of_business")
        private String constitutionOfBusiness;
        @JsonProperty("taxpayer_type")
        private String taxpayerType;
        @JsonProperty("gstin_status")
        private String gstinStatus;
        @JsonProperty("date_of_registration")
        private String dateOfRegistration;
        @JsonProperty("date_of_cancellation")
        private String dateOfCancellation;
        @JsonProperty("annual_turnover")
        private String annualTurnover;
        @JsonProperty("annual_turnover_fy")
        private String annualTurnoverFy;
        @JsonProperty("nature_of_core_business_activity_description")
        private String natureOfCoreBusinessActivityDescription;
        private List<String> promoters;
        @JsonProperty("filing_status")
        private List<List<FilingEntry>> filingStatus;
        @JsonProperty("contact_details")
        private ContactDetails contactDetails;
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ContactDetails {
        private ContactEntry principal;
        private List<ContactEntry> additional;
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ContactEntry {
        private String address;
        private String email;
        private String mobile;
        @JsonProperty("nature_of_business")
        private String natureOfBusiness;
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FilingEntry {
        @JsonProperty("return_type")
        private String returnType;
        @JsonProperty("financial_year")
        private String financialYear;
        @JsonProperty("tax_period")
        private String taxPeriod;
        @JsonProperty("date_of_filing")
        private String dateOfFiling;
        private String status;
        @JsonProperty("mode_of_filing")
        private String modeOfFiling;
    }
}
