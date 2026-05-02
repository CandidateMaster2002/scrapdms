package com.company.grc.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;

import java.time.LocalDateTime;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.UpdateTimestamp;

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

    @Column(name = "hsn_code", length = 10)
    private String hsnCode;

    @Column(name = "is_applicable")
    private Boolean isApplicable; // true only if hsn_code = 7204

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by")
    private String updatedBy;



}
