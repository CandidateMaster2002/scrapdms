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

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

}
