package com.company.grc.rule.impl;

import com.company.grc.entity.GstDetailsEntity;
import com.company.grc.rule.GrcRule;
import com.company.grc.service.GrcRuleConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

@Component
public class GstTypeRule implements GrcRule {

    private final GrcRuleConfigService configService;

    @Autowired
    public GstTypeRule(GrcRuleConfigService configService) {
        this.configService = configService;
    }

    @Override
    public String getRuleName() { return "GST Type"; }

    @Override
    public BigDecimal apply(GstDetailsEntity entity) {
        Map<String, Double> cfg = configService.getConfigMap();
        double maxScore = cfg.getOrDefault("TYPE_MAX", 10.0);
        String type = entity.getGstType() == null ? "" : entity.getGstType().toLowerCase();

        double multiplier;
        if (type.contains("public")) {
            multiplier = cfg.getOrDefault("TYPE_PUBLIC_MULT", 0.0);
        } else if (type.contains("private")) {
            multiplier = cfg.getOrDefault("TYPE_PRIVATE_MULT", 0.25);
        } else if (type.contains("partner")) {
            multiplier = cfg.getOrDefault("TYPE_PARTNER_MULT", 0.5);
        } else if (type.contains("proprietor")) {
            multiplier = cfg.getOrDefault("TYPE_PROPR_MULT", 1.0);
        } else if (type.contains("other")) {
            multiplier = cfg.getOrDefault("TYPE_OTHER_MULT", 1.0);
        } else {
            // Default to Proprietorship/High risk if unknown
            multiplier = cfg.getOrDefault("TYPE_PROPR_MULT", 1.0);
        }

        return BigDecimal.valueOf(maxScore * multiplier);
    }
}
