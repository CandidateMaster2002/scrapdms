package com.company.grc.rule.impl;

import com.company.grc.entity.GstDetailsEntity;
import com.company.grc.rule.GrcRule;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
public class GstStatusRule implements GrcRule {

    @Override
    public BigDecimal apply(GstDetailsEntity details) {
        if (details.getGstStatus() == null) {
            // Missing status -> Assume not active? Rule says "Any other status -> 13"
            return new BigDecimal("13.0");
        }

        // "Active" -> 0
        // Any other -> 13
        if ("Active".equalsIgnoreCase(details.getGstStatus())) {
            return BigDecimal.ZERO;
        } else {
            return new BigDecimal("13.0");
        }
    }

    @Override
    public String getRuleName() {
        return "GST Status Scoring";
    }
}
