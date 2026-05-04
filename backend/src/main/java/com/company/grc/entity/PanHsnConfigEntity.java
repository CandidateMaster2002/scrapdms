package com.company.grc.entity;


import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "pan_hsn_config")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PanHsnConfigEntity {

    @Id
    @Column(length = 10)
    private String pan;

    @Column(name = "category_id")
    private Long categoryId;

    @Column(name = "is_applicable")
    private Boolean isApplicable;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by")
    private String updatedBy;
}
