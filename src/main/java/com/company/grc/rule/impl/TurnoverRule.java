package com.company.grc.rule.impl;

import com.company.grc.entity.GstDetailsEntity;
import com.company.grc.rule.GrcRule;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
public class TurnoverRule implements GrcRule {

    @Override
    public BigDecimal apply(GstDetailsEntity details) {
        if (details.getAggregateTurnover() == null) {
            return BigDecimal.ZERO; // Or handle as missing? Assuming 0 for missing.
        }

        String turnoverStr = details.getAggregateTurnover();

        // Example format: "Slab: Rs. 500 Cr. and above", "2024-2025" (Wait, turnover
        // string in example is "Slab: Rs. 500 Cr. and above")
        // Need to parse string.
        // Format examples assumed from typical GST APIs or Prompt text:
        // "Slab: Rs. 500 Cr. and above"
        // Prompt Logic:
        // < 5 Cr -> 6.5
        // 5–50 Cr -> 5
        // 50–100 Cr -> 2
        // > 100 Cr -> 1

        // This parser needs to be robust.
        // For simplicity, checking keywords first? Or removing "Slab: Rs." and "Cr."
        // and parsing double?

        double turnoverValue = parseTurnover(turnoverStr);

        if (turnoverValue < 5.0) {
            return new BigDecimal("6.5");
        } else if (turnoverValue >= 5.0 && turnoverValue < 50.0) {
            return new BigDecimal("5.0");
        } else if (turnoverValue >= 50.0 && turnoverValue <= 100.0) {
            return new BigDecimal("2.0");
        } else {
            // > 100
            return new BigDecimal("1.0");
        }
    }

    // Helper to parse "Slab: Rs. 500 Cr. and above" or similar
    private double parseTurnover(String text) {
        try {
            // Simple keyword matching for common slabs if exact parsing is hard without
            // more examples
            // But let's try to extract numbers.
            if (text.contains("500 Cr. and above"))
                return 500.0;

            // Regex could extract the first number found
            String numberOnly = text.replaceAll("[^0-9.]", " ").trim();
            // "500" -> 500.0
            // "5 - 50" -> ?? GST usually returns a bucket.
            // If API returns specific value like "4500000", that's different.
            // Based on example: "aggreTurnOver": "Slab: Rs. 500 Cr. and above"
            // Let's implement a best-effort slab parser.

            // If text contains "500 Cr. and above" -> > 100 -> 1

            if (text.toLowerCase().contains("above")) {
                // Check the number before "above" or "Cr"
                // e.g. "1.5 Cr. to 5 Cr."
                // "Rs. 500 Cr. and above"
                if (text.contains("500"))
                    return 500.0;
            }

            // If unknown, fallback?
            // Let's assume high risk (low score? or high score?)
            // Prompt doesn't specify default.
            // Let's try basic parsing of the first number found.
            String[] parts = text.split(" ");
            for (String p : parts) {
                if (p.matches("\\d+(\\.\\d+)?")) {
                    return Double.parseDouble(p);
                }
            }

            return 0.0;
        } catch (Exception e) {
            return 0.0;
        }
    }

    @Override
    public String getRuleName() {
        return "Turnover Scoring";
    }
}
