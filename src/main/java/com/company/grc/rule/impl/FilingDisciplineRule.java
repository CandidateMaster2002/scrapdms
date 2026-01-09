package com.company.grc.rule.impl;

import com.company.grc.entity.GstDetailsEntity;
import com.company.grc.rule.GrcRule;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class FilingDisciplineRule implements GrcRule {

    @Override
    public BigDecimal apply(GstDetailsEntity details) {
        if (details.getReturns() == null || details.getReturns().isEmpty()) {
            return BigDecimal.ZERO;
        }

        // Deduct points for missing returns
        long missingCount = details.getReturns().stream()
                .filter(r -> "Missing".equalsIgnoreCase(r.getStatus()))
                .count();

        // Base score for filing, penalized by missing returns
        BigDecimal ruleScore = new BigDecimal("20.0");
        BigDecimal penalty = new BigDecimal(missingCount).multiply(new BigDecimal("5.0"));
        
        BigDecimal finalScore = ruleScore.subtract(penalty);
        
        return finalScore.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : finalScore;
    }

    @Override
    public String getRuleName() {
        return "Filing Discipline Scoring";
    }
}
