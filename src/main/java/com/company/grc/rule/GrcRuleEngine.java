package com.company.grc.rule;

import com.company.grc.entity.GstDetailsEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class GrcRuleEngine {

    private final List<GrcRule> rules;
    
    // Configurable multiplier could be moved to properties
    private static final BigDecimal FINAL_MULTIPLIER = new BigDecimal("1.53"); 

    @Autowired
    public GrcRuleEngine(List<GrcRule> rules) {
        this.rules = rules;
    }

    public BigDecimal calculateScore(GstDetailsEntity details) {
        BigDecimal totalScore = BigDecimal.ZERO;

        for (GrcRule rule : rules) {
            BigDecimal ruleScore = rule.apply(details);
            totalScore = totalScore.add(ruleScore);
        }

        // Apply final multiplier
        return totalScore.multiply(FINAL_MULTIPLIER);
    }
}
