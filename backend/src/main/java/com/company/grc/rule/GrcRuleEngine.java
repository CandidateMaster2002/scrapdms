package com.company.grc.rule;

import com.company.grc.entity.GstDetailsEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Aggregates all GrcRule implementations and sums their scores.
 * Total possible score = 100 (no additional multiplier needed).
 */
@Component
public class GrcRuleEngine {

    private final List<GrcRule> rules;

    @Autowired
    public GrcRuleEngine(List<GrcRule> rules) {
        this.rules = rules;
    }

    public BigDecimal calculateScore(GstDetailsEntity details) {
        BigDecimal totalScore = BigDecimal.ZERO;
        for (GrcRule rule : rules) {
            totalScore = totalScore.add(rule.apply(details));
        }
        return totalScore;
    }

    public java.util.Map<String, BigDecimal> calculateBreakdown(GstDetailsEntity details) {
        java.util.Map<String, BigDecimal> breakdown = new java.util.LinkedHashMap<>();
        for (GrcRule rule : rules) {
            breakdown.put(rule.getRuleName(), rule.apply(details));
        }
        return breakdown;
    }
}
