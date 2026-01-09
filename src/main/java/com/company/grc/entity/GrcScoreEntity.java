package com.company.grc.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Entity
@Table(name = "grc_score")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GrcScoreEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "gstin", nullable = false)
    private String gstin; // Storing GSTIN directly vs FK relationship based on use case; keeping loose coupling for history might be better, but strict FK is safer. Schema uses FK.

    @Column(columnDefinition = "DECIMAL(5,2)")
    private BigDecimal score;

    @Column(name = "score_version")
    private String scoreVersion;

    @Column(name = "calculated_at")
    private LocalDateTime calculatedAt;
}
