package com.company.grc.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "gstr7_filing_details",
        uniqueConstraints = @UniqueConstraint(columnNames = {"gstin", "return_period"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Gstr7FilingDetailEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "gstin", length = 15, nullable = false)
    private String gstin;

    @Column(name = "return_period", length = 7, nullable = false)
    private String returnPeriod; // YYYY-MM

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "date_of_filing")
    private LocalDate dateOfFiling;

    @Column(name = "status", length = 50)
    private String status; // Regular / Delayed / Missed

    @Column(name = "delay_days")
    private Integer delayDays;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
