package com.company.grc.rule;

import com.company.grc.entity.GstDetailsEntity;
import java.math.BigDecimal;

public interface GrcRule {
    /**
     * Apply the rule logic on the given GST context.
     * @param details The GST Details including nested collections like returns.
     * @return The partial score contribution of this rule.
     */
    BigDecimal apply(GstDetailsEntity details);
    
    /**
     * @return Determines the order of execution if needed, or just for logging.
     */
    String getRuleName();
}
