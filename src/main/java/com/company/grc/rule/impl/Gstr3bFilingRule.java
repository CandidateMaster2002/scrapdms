package com.company.grc.rule.impl;

import com.company.grc.entity.GstDetailsEntity;
import com.company.grc.rule.GrcRule;
import com.company.grc.service.GrcRuleConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

@Component
public class Gstr3bFilingRule implements GrcRule {

    private final GrcRuleConfigService configService;

    @Autowired
    public Gstr3bFilingRule(GrcRuleConfigService configService) {
        this.configService = configService;
    }

    @Override
    public String getRuleName() { return "GSTR-3B Filing"; }

    @Override
    public BigDecimal apply(GstDetailsEntity entity) {
        Map<String, Double> cfg = configService.getConfigMap();
        double maxScore = cfg.getOrDefault("G3B_MAX", 20.0);
        double threshold = cfg.getOrDefault("G3B_THRESHOLD", 1.0);
        double okMult = cfg.getOrDefault("G3B_OK_MULT", 0.0);
        double delayMult = cfg.getOrDefault("G3B_DELAY_MULT", 1.0);
        
        int delays = entity.getDelayCountGstr3b() == null ? 0 : entity.getDelayCountGstr3b();
        double score = (delays > threshold) ? maxScore * delayMult : maxScore * okMult;
        
        return BigDecimal.valueOf(score);
    }
}
