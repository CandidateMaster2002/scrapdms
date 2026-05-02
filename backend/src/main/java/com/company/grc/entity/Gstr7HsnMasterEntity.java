package com.company.grc.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDateTime;
import org.hibernate.annotations.UpdateTimestamp;

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

    @UpdateTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "created_by")
    private String createdBy;
}
