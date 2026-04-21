# GSTR Delay Count Rule

## Purpose

Defines how `delayCountGstr1` and `delayCountGstr3b` are calculated when GST data
is fetched or refreshed from the Deepvue API. These counts feed into the GRC score
via `Gstr1FilingRule` and `Gstr3bFilingRule`.

---

## Period to Evaluate

| Boundary      | Value                                                                  |
| ------------- | ---------------------------------------------------------------------- |
| Start (fixed) | January 2025 tax period                                                |
| End (dynamic) | Last completed month — the month **before** the current calendar month |

**Example:** If today is 10 Apr 2026, the range is Jan 2025 → Mar 2026 inclusive.

---

## Due Dates

### GSTR-1

| Tax Period | Due Date                        |
| ---------- | ------------------------------- |
| All months | **11th of the following month** |

Example: March tax period → due April 11.

### GSTR-3B

| Tax Period                  | Due Date                           |
| --------------------------- | ---------------------------------- |
| All months except September | **21st of the following month**    |
| **September**               | **25th of October** (special case) |

---

## Delay Condition (per period, per return type)

A period is counted as a **delay** when ALL three conditions are true:

1. **Due date has passed** — today's date is strictly after the due date for that
   period + return type combination.
   - If the due date has not yet passed, the period is **never** counted as a delay,
     regardless of whether the return has been filed.
   - This primarily affects the most recent completed month, whose due date may still
     be in the future at the time of checking.

2. **The return was not filed on time** — either:
   - No filing entry exists in `filing_status` for that period, **OR**
   - An entry exists but `date_of_filing` is strictly **after** the due date
     (late filing still counts as a delay).

### Decision table

| Due date passed? | Filed on or before due date? | Delay?  |
| ---------------- | ---------------------------- | ------- |
| No               | — (irrelevant)               | **No**  |
| Yes              | Yes                          | **No**  |
| Yes              | No (filed late or not filed) | **Yes** |

---

## Worked Examples

### Example 1 — Today is 10 Apr 2026, GSTR-1 for March 2026 not filed

- Due date: 11 Apr 2026
- Today (10 Apr) ≤ due date → **no delay** (due date not yet passed)

### Example 2 — Today is 13 Apr 2026, GSTR-1 for March 2026 filed on 12 Apr

- Due date: 11 Apr 2026
- Today (13 Apr) > due date → due date passed ✓
- Filed on 12 Apr > 11 Apr → filed late → **delay** ✓

### Example 3 — Today is 13 Apr 2026, GSTR-1 for March 2026 not filed at all

- Due date: 11 Apr 2026
- Today (13 Apr) > due date → due date passed ✓
- No entry found → not filed → **delay** ✓

### Example 4 — Today is 10 Apr 2026, GSTR-3B for March 2026 not filed

- Due date: 21 Apr 2026
- Today (10 Apr) ≤ due date → **no delay** (due date not yet passed)

### Example 5 — Today is 25 Oct 2026, GSTR-3B for September 2026 not filed

- Due date: 25 Oct 2026 (special September rule)
- Today (25 Oct) ≤ due date → **no delay** (due date not yet passed; 25th is the last allowed day)

### Example 6 — Today is 26 Oct 2026, GSTR-3B for September 2026 not filed

- Due date: 25 Oct 2026
- Today (26 Oct) > due date → due date passed ✓
- Not filed → **delay** ✓

### Example 7 — Any month before last month (e.g., Jan 2025, checked in Apr 2026)

- Due dates for Jan 2025 are Feb 11 2025 (GSTR-1) and Feb 21 2025 (GSTR-3B)
- Today (Apr 2026) is far past both due dates → due date always passed ✓
- Delay determined solely by whether filed on time

---

## Financial Year Mapping

Tax period month → financial year used to look up the filing entry:

| Calendar Month | Financial Year                  |
| -------------- | ------------------------------- |
| April – March  | The FY that contains that month |
| Jan 2025       | 2024-2025                       |
| April 2025     | 2025-2026                       |
| March 2026     | 2025-2026                       |

FY boundary: April 1 starts a new financial year.

---

## Implementation Location

`GstFetchService.java` → `mapApiDataToEntity()` method.

The existing simple loop that counts `status != "Filed"` entries is replaced by:

1. Build a lookup map: `(returnType, financialYear, taxPeriod) → FilingEntry`
2. Iterate every month from Jan 2025 to last completed month
3. For each month + return type, compute the due date
4. Skip if today ≤ due date (not yet due)
5. Look up the filing entry; count as delay if missing or filed after due date

The rule scoring files (`Gstr1FilingRule.java`, `Gstr3bFilingRule.java`) are
**not changed** — they continue to convert the count to a GRC score using
configurable thresholds and multipliers.

---

## Constants Summary

| Constant                   | Value              |
| -------------------------- | ------------------ |
| START_MONTH                | January 2025       |
| GSTR1_DUE_DAY              | 11                 |
| GSTR3B_DUE_DAY             | 21                 |
| GSTR3B_SEPTEMBER_DUE_DAY   | 25                 |
| GSTR3B_SEPTEMBER_DUE_MONTH | October (month 10) |
