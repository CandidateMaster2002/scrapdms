# GSTR Delay Count Rule

## Purpose
Defines how `delayCountGstr1` and `delayCountGstr3b` are calculated when GST data
is fetched or refreshed from the Deepvue API. These counts feed into the GRC score
via `Gstr1FilingRule` and `Gstr3bFilingRule`.

---

## Period to Evaluate

| Boundary | Value |
|---|---|
| Start (fixed) | January 2025 tax period |
| End (dynamic) | Last completed month — the month **before** the current calendar month |

**Example:** If today is 10 Apr 2026, the range is Jan 2025 → Mar 2026 inclusive.

---

## Due Dates

### GSTR-1
| Tax Period | Due Date |
|---|---|
| All months | **11th of the following month** |

Example: March tax period → due April 11.

### GSTR-3B
| Tax Period | Due Date |
|---|---|
| All months except September | **21st of the following month** |
| **September** | **25th of October** (special case) |

---

## Delay Condition (per period, per return type)

A period is counted as a **delay** ONLY when BOTH conditions are true:

1. **Due date has passed** — today's date is strictly after the due date for that
   period + return type combination.

2. **A filing entry exists but was filed late** — an entry exists in `filing_status`
   for that period AND `date_of_filing` is strictly **after** the due date.

> **Missing entries are NOT delays.** If no filing entry exists for a given month,
> that month is skipped entirely. The API may not return data for all months,
> so missing data should not penalize the entity.

### Decision table

| Due date passed? | Entry exists? | Filed on or before due date? | Delay? |
|---|---|---|---|
| No | — | — | **No** |
| Yes | No (missing) | — | **No** (skipped) |
| Yes | Yes | Yes | **No** |
| Yes | Yes | No (filed late) | **Yes** |

---

## Financial Year Mapping

Tax period month → financial year used to look up the filing entry:

| Calendar Month | Financial Year |
|---|---|
| April – March | The FY that contains that month |
| Jan 2025 | 2024-2025 |
| April 2025 | 2025-2026 |
| March 2026 | 2025-2026 |

FY boundary: April 1 starts a new financial year.

---

## Implementation Location

`GstFetchService.java` → `calculateDelayCounts()` method.

1. Build a lookup map: `(returnType, financialYear, taxPeriod) → FilingEntry`
2. Iterate every month from Jan 2025 to last completed month
3. For each month + return type, compute the due date
4. Skip if today ≤ due date (not yet due)
5. Look up the filing entry; count as delay **only if entry exists AND filed after due date**

The rule scoring files (`Gstr1FilingRule.java`, `Gstr3bFilingRule.java`) are
**not changed** — they continue to convert the count to a GRC score using
configurable thresholds and multipliers.

---

## Constants Summary

| Constant | Value |
|---|---|
| START_MONTH | January 2025 |
| GSTR1_DUE_DAY | 11 |
| GSTR3B_DUE_DAY | 21 |
| GSTR3B_SEPTEMBER_DUE_DAY | 25 |
| GSTR3B_SEPTEMBER_DUE_MONTH | October (month 10) |
