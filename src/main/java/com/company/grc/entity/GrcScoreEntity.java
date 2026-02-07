package com.company.grc.entity;

import jakarta.persistence.Entity;

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
    @Column(name = "gstin", nullable = false, length = 15)
    private String gstin;

    @Column(name = "score")
    private Integer score;

    @Column(name = "score_version")
    private String scoreVersion;

    @Column(name = "calculated_at")
    private LocalDateTime calculatedAt;
}
