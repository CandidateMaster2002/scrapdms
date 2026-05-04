package com.company.grc.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonBackReference;

@Entity
@Table(name = "gstr7_hsn_master")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Gstr7HsnMasterEntity {

    @Id
    @Column(length = 10)
    private String hsnCode;

    @Column(name = "description")
    private String description;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "created_by")
    private String createdBy;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JsonBackReference
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private HsnCategoryEntity category;
}
