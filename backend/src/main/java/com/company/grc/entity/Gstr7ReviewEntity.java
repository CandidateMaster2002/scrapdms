package com.company.grc.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "gstr7_reviews")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Gstr7ReviewEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "gstin", nullable = false)
    private String gstin;

    @Column(name = "submitted_by")
    private String submittedBy;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "parsed_data", columnDefinition = "TEXT")
    private String parsedData; // JSON array of ParsedRecord

    @Column(name = "status")
    private String status; // PENDING, APPROVED, REJECTED
}
