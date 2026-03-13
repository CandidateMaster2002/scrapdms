package com.company.grc.rule.impl;

import com.company.grc.entity.GstDetailsEntity;
import com.company.grc.rule.GrcRule;
import com.company.grc.service.GrcRuleConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.util.Map;

@Component
public class GstTenureRule implements GrcRule {

    private final GrcRuleConfigService configService;

    @Autowired
    public GstTenureRule(GrcRuleConfigService configService) {
        this.configService = configService;
    }

    @Override
    public String getRuleName() { return "Registration Date"; }

    @Override
    public BigDecimal apply(GstDetailsEntity entity) {
        if (entity.getRegistrationDate() == null) return BigDecimal.ZERO;
        Map<String, Double> cfg = configService.getConfigMap();
        double maxScore = cfg.getOrDefault("REG_MAX", 10.0);
        int years = Period.between(entity.getRegistrationDate(), LocalDate.now()).getYears();
        double multiplier;
        if (years < 1)       multiplier = cfg.getOrDefault("REG_LT1_MULT",  1.0);
        else if (years < 3)  multiplier = cfg.getOrDefault("REG_1TO3_MULT", 0.75);
        else if (years <= 5) multiplier = cfg.getOrDefault("REG_3TO5_MULT", 0.5);
        else                 multiplier = cfg.getOrDefault("REG_GT5_MULT",  0.0);
        return BigDecimal.valueOf(maxScore * multiplier);
    }
}
