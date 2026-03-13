package com.company.grc.rule.impl;

import com.company.grc.entity.GstDetailsEntity;
import com.company.grc.rule.GrcRule;
import com.company.grc.service.GrcRuleConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

@Component
public class GstStatusRule implements GrcRule {

    private final GrcRuleConfigService configService;

    @Autowired
    public GstStatusRule(GrcRuleConfigService configService) {
        this.configService = configService;
    }

    @Override
    public String getRuleName() { return "GSTN Status"; }

    @Override
    public BigDecimal apply(GstDetailsEntity entity) {
        Map<String, Double> cfg = configService.getConfigMap();
        double maxScore = cfg.getOrDefault("STS_MAX", 35.0);
        String status = entity.getGstStatus() == null ? "" : entity.getGstStatus().toLowerCase();
        double multiplier = "active".equals(status)
                ? cfg.getOrDefault("STS_ACTIVE_MULT", 0.0)
                : cfg.getOrDefault("STS_CANCEL_MULT", 1.0);
        return BigDecimal.valueOf(maxScore * multiplier);
    }
}
