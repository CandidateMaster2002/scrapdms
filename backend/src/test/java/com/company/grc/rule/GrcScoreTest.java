package com.company.grc.rule;

import com.company.grc.entity.GstDetailsEntity;
import com.company.grc.rule.impl.*;
import com.company.grc.service.GrcRuleConfigService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for the 6-rule GRC scoring system (max = 100).
 * Uses a mocked GrcRuleConfigService returning default values.
 */
public class GrcScoreTest {

    /** Build a config service mock that returns the standard defaults. */
    private GrcRuleConfigService mockConfig() {
        Map<String, Double> defaults = new HashMap<>();
        GrcRuleConfigService.DEFAULTS.forEach((k, v) -> defaults.put(k, (Double) v[0]));
        GrcRuleConfigService svc = Mockito.mock(GrcRuleConfigService.class);
        Mockito.when(svc.getConfigMap()).thenReturn(defaults);
        return svc;
    }

    private List<GrcRule> buildRules(GrcRuleConfigService svc) {
        return Arrays.asList(
                new GstTypeRule(svc),
                new GstTenureRule(svc),
                new TurnoverRule(svc),
                new GstStatusRule(svc),
                new Gstr1FilingRule(svc),
                new Gstr3bFilingRule(svc));
    }

    @Test
    public void testMaxRiskScore() {
        // Proprietorship (10) + <1yr (10) + <50Cr (5) + Cancelled (35) +
        // GSTR1 delay>1 (20) + GSTR3B delay>1 (20) = 100
        GrcRuleConfigService svc = mockConfig();
        GrcRuleEngine engine = new GrcRuleEngine(buildRules(svc));

        GstDetailsEntity risky = GstDetailsEntity.builder()
                .gstType("Proprietorship")
                .registrationDate(LocalDate.now().minusMonths(6))
                .aggregateTurnover("5")
                .gstStatus("Cancelled")
                .delayCountGstr1(5)
                .delayCountGstr3b(5)
                .build();

        BigDecimal score = engine.calculateScore(risky);
        assertEquals(0, new BigDecimal("100").compareTo(score), "Max risk mismatch: " + score);
    }

    @Test
    public void testMinRiskScore() {
        // Private company (0) + >5yr (0) + >100Cr (0) + Active (0) + delays<=1 (0) = 0
        GrcRuleConfigService svc = mockConfig();
        GrcRuleEngine engine = new GrcRuleEngine(buildRules(svc));

        GstDetailsEntity safe = GstDetailsEntity.builder()
                .gstType("Private Limited Company")
                .registrationDate(LocalDate.now().minusYears(10))
                .aggregateTurnover("500")
                .gstStatus("Active")
                .delayCountGstr1(0)
                .delayCountGstr3b(0)
                .build();

        BigDecimal score = engine.calculateScore(safe);
        assertEquals(0, new BigDecimal("0").compareTo(score), "Min risk mismatch: " + score);
    }

    @Test
    public void testPartialRiskScore() {
        // Proprietorship (10) + >5yr (0) + >100Cr (0) + Active (0) + GSTR1 delay>1 (20) + GSTR3B ok (0) = 30
        GrcRuleConfigService svc = mockConfig();
        GrcRuleEngine engine = new GrcRuleEngine(buildRules(svc));

        GstDetailsEntity partial = GstDetailsEntity.builder()
                .gstType("Proprietorship")
                .registrationDate(LocalDate.now().minusYears(10))
                .aggregateTurnover("500")
                .gstStatus("Active")
                .delayCountGstr1(5)
                .delayCountGstr3b(1)
                .build();

        BigDecimal score = engine.calculateScore(partial);
        assertEquals(0, new BigDecimal("30").compareTo(score), "Partial risk mismatch: " + score);
    }
}
