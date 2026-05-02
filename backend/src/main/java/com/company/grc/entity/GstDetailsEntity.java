package com.company.grc.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Entity
@Table(name = "gst_details")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GstDetailsEntity {

    @Id
    @Column(length = 15)
    private String gstin;

    @Column(name = "gst_type", length = 100)
    private String gstType; // Public / Private / Proprietorship

    @Column(name = "trade_name", length = 500)
    private String tradeName;

    @Column(name = "legal_name", length = 500)
    private String legalName;

    @Column(name = "registration_date")
    private LocalDate registrationDate;

    @Column(name = "gst_status", length = 50)
    private String gstStatus; // Active / Inactive

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(name = "last_api_sync")
    private LocalDateTime lastApiSync;

    @Column(name = "aggregate_turnover", length = 100)
    private String aggregateTurnover;

    @Column(name = "delay_count_gstr1")
    private Integer delayCountGstr1;

    @Column(name = "delay_count_gstr3b")
    private Integer delayCountGstr3b;

    @Column(name = "source", length = 50)
    private String source;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // ── GSTR7 fields ──────────────────────────────────────────────────────────

    @Column(name = "gstd_no", length = 15)
    private String gstdNo;

    @Column(name = "gstr7_status", length = 50)
    private String gstr7Status;

    @Column(name = "gstr7_delay_count")
    private Integer gstr7DelayCount = 0;

    @Column(name = "gstr7_missed_count")
    private Integer gstr7MissedCount = 0;

    @Column(name = "gstr7_last_updated")
    private LocalDateTime gstr7LastUpdated;

    // ── Deepvue API fields ────────────────────────────────────────────────────

    @Column(name = "mobile", length = 20)
    private String mobile;

    @Column(name = "email", length = 200)
    private String email;

    @Column(name = "api_error")
    private Boolean apiError;

    @Column(name = "data_source", length = 20)
    private String dataSource; // "API", "Manual", "Error", "Pending"

    @Column(columnDefinition = "TEXT")
    private String promoters;

    @Column(name = "pan_number", length = 15)
    private String panNumber;

}
