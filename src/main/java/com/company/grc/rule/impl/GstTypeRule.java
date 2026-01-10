package com.company.grc.rule.impl;

import com.company.grc.entity.GstDetailsEntity;
import com.company.grc.rule.GrcRule;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
public class GstTypeRule implements GrcRule {

    @Override
    public BigDecimal apply(GstDetailsEntity details) {
        if (details.getGstType() == null)
            return BigDecimal.ZERO;

        String type = details.getGstType(); // Case sensitive check not specified, but usually safe to ignore case or
                                            // normalized

        // Exact Rules:
        // "Society", "Trust", "AOP", "Government" -> 2
        // "Private" -> 4
        // "Proprietor" -> 13

        // Standardizing for contain check
        String typeCheck = type.toLowerCase();

        if (typeCheck.contains("society") || typeCheck.contains("trust") ||
                typeCheck.contains("aop") || typeCheck.contains("government") ||
                typeCheck.contains("club")) { // "Society/ Club/ Trust/ AOP" in example
            return new BigDecimal("2.0");
        }

        if (typeCheck.contains("private")) {
            return new BigDecimal("4.0");
        }

        if (typeCheck.contains("proprietor")) {
            return new BigDecimal("13.0");
        }

        return BigDecimal.ZERO; // No match
    }

    @Override
    public String getRuleName() {
        return "GST Type Scoring";
    }
}
