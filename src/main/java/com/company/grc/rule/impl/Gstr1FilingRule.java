package com.company.grc.rule.impl;

import com.company.grc.entity.GstDetailsEntity;
import com.company.grc.rule.GrcRule;
import com.company.grc.service.GrcRuleConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

@Component
public class Gstr1FilingRule implements GrcRule {

    private final GrcRuleConfigService configService;

    @Autowired
    public Gstr1FilingRule(GrcRuleConfigService configService) {
        this.configService = configService;
    }

    @Override
    public String getRuleName() { return "GSTR-1 Filing"; }

    @Override
    public BigDecimal apply(GstDetailsEntity entity) {
        Map<String, Double> cfg = configService.getConfigMap();
        double maxScore = cfg.getOrDefault("G1_MAX", 20.0);
        double threshold = cfg.getOrDefault("G1_THRESHOLD", 1.0);
        double okMult = cfg.getOrDefault("G1_OK_MULT", 0.0);
        double delayMult = cfg.getOrDefault("G1_DELAY_MULT", 1.0);
        
        int delays = entity.getDelayCountGstr1() == null ? 0 : entity.getDelayCountGstr1();
        double score = (delays > threshold) ? maxScore * delayMult : maxScore * okMult;
        
        return BigDecimal.valueOf(score);
    }
}
