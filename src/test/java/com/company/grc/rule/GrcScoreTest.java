package com.company.grc.rule;

import com.company.grc.entity.GstDetailsEntity;
import com.company.grc.rule.impl.*;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class GrcScoreTest {

    @Test
    public void testStrictScoreCalculation() {
        // Setup Rules
        List<GrcRule> rules = Arrays.asList(
                new GstTypeRule(),
                new GstTenureRule(),
                new TurnoverRule(),
                new GstStatusRule(),
                new FilingDelayRule());

        GrcRuleEngine engine = new GrcRuleEngine(rules);

        // Scenario 1: A risky entity
        // Type: Proprietor (+13)
        // Age: < 1 year (+9.75)
        // Turnover: < 5 Cr (+6.5)
        // Status: Cancelled/Other (+13)
        // Delay GSTR1: > 0 (+13)
        // Delay GSTR3B: > 0 (+9.75)
        // Base Score = 13 + 9.75 + 6.5 + 13 + 13 + 9.75 = 65.0
        // Final Score = 65.0 * 1.53 = 99.45

        GstDetailsEntity risky = GstDetailsEntity.builder()
                .gstType("Proprietorship")
                .registrationDate(LocalDate.now().minusMonths(6))
                .aggregateTurnover("Slab: Rs. 0 Cr. to 5 Cr.") // Should parse < 5
                .gstStatus("Cancelled")
                .delayCountGstr1(1)
                .delayCountGstr3b(1)
                .build();

        BigDecimal score = engine.calculateScore(risky);
        assertEquals(0, new BigDecimal("99.45").compareTo(score), "Risky Score mismatch: " + score);

        // Scenario 2: A safe entity
        // Type: Public Limited (Not in risky list -> 0? Wait, rule: "Society... -> 2",
        // "Private -> 4", "Proprietor -> 13". Else 0?)
        // Let's check GstTypeRule logic.
        // It returns 0 if no match. "Public Limited" contains "public" which is not
        // matched. So 0.
        // Age: > 5 years (+1)
        // Turnover: > 100 Cr (+1)
        // Status: Active (+0)
        // Delay GSTR1: 0 (+0)
        // Delay GSTR3B: 0 (+0)
        // Base Score = 0 + 1 + 1 + 0 + 0 + 0 = 2.0
        // Final Score = 2.0 * 1.53 = 3.06

        GstDetailsEntity safe = GstDetailsEntity.builder()
                .gstType("Public Limited Company")
                .registrationDate(LocalDate.now().minusYears(10))
                .aggregateTurnover("Slab: Rs. 500 Cr. and above")
                .gstStatus("Active")
                .delayCountGstr1(0)
                .delayCountGstr3b(0)
                .build();

        BigDecimal safeScore = engine.calculateScore(safe);
        assertEquals(0, new BigDecimal("3.06").compareTo(safeScore), "Safe Score mismatch: " + safeScore);
    }
}
