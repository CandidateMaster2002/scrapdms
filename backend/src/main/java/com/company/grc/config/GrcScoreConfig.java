package com.company.grc.config;

import org.springframework.stereotype.Component;

/**
 * Central configuration for GRC Score calculation weights.
 * All max scores and multipliers are stored here as variables.
 * Edit these values to adjust scoring rules — no need to touch rule logic.
 *
 * Total possible score = 10 + 10 + 5 + 35 + 20 + 20 = 100
 */
@Component
public class GrcScoreConfig {

    // ── Rule 1: GST Type ─────────────────────────────────────────────────────
    public final int TYPE_MAX_SCORE = 10;
    public final double TYPE_PROPRIETORSHIP_MULTIPLIER = 1.0;   // Proprietorship
    public final double TYPE_COMPANY_MULTIPLIER = 0.0;           // Private / Public company

    // ── Rule 2: Registration Date (business age) ──────────────────────────────
    public final int REGISTRATION_MAX_SCORE = 10;
    public final double REG_LESS_THAN_1_YEAR_MULTIPLIER  = 1.0;   // < 1 year
    public final double REG_1_TO_3_YEARS_MULTIPLIER      = 0.75;  // 1–3 years
    public final double REG_3_TO_5_YEARS_MULTIPLIER      = 0.5;   // 3–5 years
    public final double REG_MORE_THAN_5_YEARS_MULTIPLIER = 0.0;   // > 5 years

    // ── Rule 3: Aggregate Turnover ────────────────────────────────────────────
    public final int TURNOVER_MAX_SCORE = 5;
    public final double TURNOVER_LESS_THAN_50CR_MULTIPLIER  = 1.0;  // < 50 Cr
    public final double TURNOVER_50_TO_100CR_MULTIPLIER     = 0.5;  // 50–100 Cr
    public final double TURNOVER_MORE_THAN_100CR_MULTIPLIER = 0.0;  // > 100 Cr

    // ── Rule 4: GSTN Status ───────────────────────────────────────────────────
    public final int GST_STATUS_MAX_SCORE = 35;
    public final double GST_STATUS_ACTIVE_MULTIPLIER    = 0.0;  // Active
    public final double GST_STATUS_CANCELLED_MULTIPLIER = 1.0;  // Cancelled / any other

    // ── Rule 5: GSTR1 Filing Consistency ─────────────────────────────────────
    public final int GSTR1_MAX_SCORE = 20;
    public final int GSTR1_DELAY_THRESHOLD = 1;                 // delay_count > 1 is penalised
    public final double GSTR1_OK_MULTIPLIER      = 0.0;
    public final double GSTR1_DELAYED_MULTIPLIER = 1.0;

    // ── Rule 6: GSTR3B Filing Consistency ────────────────────────────────────
    public final int GSTR3B_MAX_SCORE = 20;
    public final int GSTR3B_DELAY_THRESHOLD = 1;                // delay_count > 1 is penalised
    public final double GSTR3B_OK_MULTIPLIER      = 0.0;
    public final double GSTR3B_DELAYED_MULTIPLIER = 1.0;

    // ── Dummy score for new GSTINs before details are filled in ──────────────
    public final int DUMMY_DEFAULT_SCORE = 15;
}
