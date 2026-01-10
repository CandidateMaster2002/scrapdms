package com.company.grc.rule.impl;

import com.company.grc.entity.GstDetailsEntity;
import com.company.grc.rule.GrcRule;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
public class GstTypeRule implements GrcRule {

    @Override
    public BigDecimal apply(GstDetailsEntity details) {
        if (details.getGstType() == null || details.getGstType().isBlank()) {
            return BigDecimal.ZERO;
        }

        String type = details.getGstType().toLowerCase();

        // RULE PRIORITY MATTERS (highest risk first)

        // Proprietorship → 13
        if (type.contains("proprietor") || type.contains("partnership")) {
            return BigDecimal.valueOf(13);
        }

        // Private Limited / Private → 2
        if (type.contains("private")) {
            return BigDecimal.valueOf(2);
        }

        // Public Limited / Government / PSU → 2
        if (type.contains("public")
                || type.contains("government")
                || type.contains("psu")) {
            return BigDecimal.valueOf(2);
        }

        // Default → 0 (neutral / unknown)
        return BigDecimal.ZERO;
    }

    @Override
    public String getRuleName() {
        return "GST Type Scoring";
    }
}
