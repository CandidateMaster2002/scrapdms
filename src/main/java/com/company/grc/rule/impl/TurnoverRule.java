package com.company.grc.rule.impl;

import com.company.grc.entity.GstDetailsEntity;
import com.company.grc.rule.GrcRule;
import com.company.grc.service.GrcRuleConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

@Component
public class TurnoverRule implements GrcRule {

    private final GrcRuleConfigService configService;

    @Autowired
    public TurnoverRule(GrcRuleConfigService configService) {
        this.configService = configService;
    }

    @Override
    public String getRuleName() { return "Turnover"; }

    @Override
    public BigDecimal apply(GstDetailsEntity entity) {
        Map<String, Double> cfg = configService.getConfigMap();
        double maxScore = cfg.getOrDefault("TRN_MAX", 5.0);
        String raw = entity.getAggregateTurnover();
        if (raw == null || raw.isBlank()) return BigDecimal.valueOf(maxScore);
        double turnoverCr = parseTurnoverCr(raw);
        double multiplier;
        if (turnoverCr > 100)      multiplier = cfg.getOrDefault("TRN_GT100_MULT",   0.0);
        else if (turnoverCr >= 50) multiplier = cfg.getOrDefault("TRN_50TO100_MULT", 0.5);
        else                       multiplier = cfg.getOrDefault("TRN_LT50_MULT",    1.0);
        return BigDecimal.valueOf(maxScore * multiplier);
    }

    private double parseTurnoverCr(String raw) {
        String[] tokens = raw.replaceAll("[^0-9.]", " ").trim().split("\\s+");
        double max = 0;
        for (String t : tokens) {
            try { max = Math.max(max, Double.parseDouble(t)); } catch (NumberFormatException ignored) {}
        }
        return max;
    }
}
