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
        if (details.getRegistrationDate() == null) return BigDecimal.ZERO;

        Period period = Period.between(details.getRegistrationDate(), LocalDate.now());
        int years = period.getYears();

        if (years >= 10) {
            return new BigDecimal("20.0");
        } else if (years >= 5) {
            return new BigDecimal("15.0");
        } else if (years >= 2) {
            return new BigDecimal("10.0");
        }
        
        return new BigDecimal("5.0");
    }

    @Override
    public String getRuleName() {
        return "GST Tenure Scoring";
    }
}
