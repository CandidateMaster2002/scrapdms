# GRC Score Calculation Logic

This document outlines the current rules used to calculate a business's GRC (Governance, Risk, and Compliance) score and the specific delay logic for their GST returns filing.

## 1. GRC Score Calculation
The GRC Score is calculated by combining points assigned from specific rules evaluating GST returns and business turnover, followed by applying a multiplier. 

### Final Score Formula
1. **Total Raw Score** = `Filing Delay Score` + `Turnover Score`
2. **Calculated Score** = `Total Raw Score` \times `1.53`
3. The final result is rounded to the nearest whole integer using standard `HALF_UP` rounding.

---

## 2. Rule Details

### A. Filing Delay Rule
Points are added based on the number of late GST return filings.

*   **GSTR-1 Delay**: 
    *   If delayed more than 1 time (`> 1`): adds **`13.0`** points.
    *   If delayed 0 or 1 time: adds **`0`** points.
*   **GSTR-3B Delay**:
    *   If delayed more than 1 time (`> 1`): adds **`9.75`** points.
    *   If delayed 0 or 1 time: adds **`0`** points.

### B. Turnover Rule
Points are assigned based on the aggregate turnover amount (provided by the API in Crores).

*   **`< 5` Cr**: adds **`6.5`** points.
*   **`5` to `< 50` Cr**: adds **`5.0`** points.
*   **`50` to `100` Cr**: adds **`3.0`** points.
*   **`> 100` Cr**: adds **`1.0`** points.

---

## 3. GST Return Delay Calculation
The delay counts determining the "Filing Delay Rule" score are calculated dynamically when fetching Taxpayer Return Details using the GST API.

### Logic Flow for Delay Count:
1.  **Status Filter**: The calculation strictly filters return records where the filing status is `"Filed"` for either `"GSTR1"` or `"GSTR3B"`.
2.  **Due Date Definition**:
    *   **Target Day**: The due date is the `11th` of the following month for **GSTR-1**, and the `20th` of the following month for **GSTR-3B**.
    *   **Period Resolution**: Uses the tax period (`taxp`, like `"April"`) and the financial year (`fy`, like `"2024-2025"`). If the period month is January, February, or March, the system safely advances the calendar year logic (+1 year) properly into the 2nd half of that financial year.
3.  **Score Tallying**: The system compares the actual Date of Filing (`dof` from the API) against this synthesized absolute Due Date. If the filing date strictly exceeds the due date, it adds `1` to the delay tally for that Specific GST Return type.
4.  **Persistence**: The final counted delays (`delayCountGstr1` and `delayCountGstr3b`) are cached/saved with the GST Details Entity in local data to consistently provide input to the scoring logic engine.
