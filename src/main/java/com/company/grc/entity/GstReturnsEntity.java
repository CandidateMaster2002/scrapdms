package com.company.grc.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.ToString;

@Entity
@Table(name = "gst_returns")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GstReturnsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "gstin", nullable = false)
    @ToString.Exclude
    private GstDetailsEntity gstDetails;

    @Column(name = "return_type")
    private String returnType; // GSTR1, GSTR3B

    @Column(name = "financial_year")
    private String financialYear;

    @Column(name = "tax_period")
    private String taxPeriod;

    private String status; // Filed, Missing

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
