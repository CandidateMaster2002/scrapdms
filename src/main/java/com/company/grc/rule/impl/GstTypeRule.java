package com.company.grc.rule.impl;

import com.company.grc.entity.GstDetailsEntity;
import com.company.grc.rule.GrcRule;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
public class GstTypeRule implements GrcRule {

    @Override
    public BigDecimal apply(GstDetailsEntity details) {
        if (details.getGstType() == null) return BigDecimal.ZERO;

        String type = details.getGstType().toLowerCase();
        
        if (type.contains("public limited")) {
            return new BigDecimal("20.0");
        } else if (type.contains("private limited")) {
            return new BigDecimal("15.0");
        } else if (type.contains("proprietorship") || type.contains("partnership")) {
            return new BigDecimal("10.0");
        }
        
        return new BigDecimal("5.0");
    }

    @Override
    public String getRuleName() {
        return "GST Type Scoring";
    }
}
