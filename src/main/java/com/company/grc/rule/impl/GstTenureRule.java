package com.company.grc.rule.impl;

import com.company.grc.entity.GstDetailsEntity;
import com.company.grc.rule.GrcRule;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;

@Component
public class GstTenureRule implements GrcRule {

    @Override
    public BigDecimal apply(GstDetailsEntity details) {
        if (details.getRegistrationDate() == null)
            return BigDecimal.ZERO;

        Period period = Period.between(details.getRegistrationDate(), LocalDate.now());
        int years = period.getYears();
        // Also consider days if strictly < 1 year means 0 years.
        // Logic says < 1 year. So if years == 0, it is < 1 year.

        if (years < 1) {
            return new BigDecimal("9.75");
        } else if (years >= 1 && years < 3) {
            return new BigDecimal("6.0");
        } else if (years >= 3 && years <= 5) { // 3–5 years -> add 3. Assuming inclusive: 3 <= x <= 5?
            // Prompt says "3-5 years -> add 3". >5 years -> add 1.
            // Usually [1, 3) is 6, [3, 5] is 3.
            return new BigDecimal("3.0");
        } else {
            // > 5 years
            return new BigDecimal("1.0");
        }
    }

    @Override
    public String getRuleName() {
        return "GST Tenure Scoring";
    }
}
