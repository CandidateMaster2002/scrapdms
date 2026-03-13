package com.company.grc.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Stores a single configurable rule parameter as a key-value pair.
 *
 * Keys follow the naming convention: RULE_PARAM
 * e.g.  TYPE_MAX, TYPE_PROPR_MULT, REG_MAX, REG_LT1_MULT ...
 */
@Entity
@Table(name = "grc_rule_config")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GrcRuleConfigEntity {

    /** Unique key identifying this config parameter (max 40 chars). */
    @Id
    @Column(name = "config_key", length = 40, nullable = false)
    private String configKey;

    /** The numeric value for this parameter. */
    @Column(name = "config_value", nullable = false)
    private Double configValue;

    /** Human-readable description shown in the Settings UI. */
    @Column(name = "description", length = 200)
    private String description;
}
