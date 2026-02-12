package com.company.grc.rule.impl;

import com.company.grc.entity.GstDetailsEntity;
import com.company.grc.rule.GrcRule;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
public class FilingDelayRule implements GrcRule {

    @Override
    public BigDecimal apply(GstDetailsEntity details) {
        BigDecimal score = BigDecimal.ZERO;

        // GSTR-1 Risk Score
        // 0 or 1 -> add 0, >1 -> add 13.5
        int gstr1Delay = details.getDelayCountGstr1() != null ? details.getDelayCountGstr1() : 0;
        if (gstr1Delay > 1) {
            score = score.add(new BigDecimal("13.0"));
        }

        // GSTR-3B Risk Score
        // 0 or 1 -> add 0, >1 -> add 13.5
        int gstr3bDelay = details.getDelayCountGstr3b() != null ? details.getDelayCountGstr3b() : 0;
        if (gstr3bDelay > 1) {
            score = score.add(new BigDecimal("9.75"));
        }

        return score;
    }

    @Override
    public String getRuleName() {
        return "Filing Delay Scoring";
    }
}
