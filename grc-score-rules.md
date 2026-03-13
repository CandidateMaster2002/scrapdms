# GRC Score Calculation Rules

> **Version**: 2.0  
> **Max possible score**: 100  
> **High score = higher risk**

All weights are defined in [`GrcScoreConfig.java`](src/main/java/com/company/grc/config/GrcScoreConfig.java).  
Change any value in that file to update weights without touching rule logic.

---

## Scoring Formula

```
GRC Score = Type Score + Registration Score + Turnover Score
           + GSTN Score + GSTR1 Score + GSTR3B Score
```

---

## Rule 1 — GST Type

| Business Type | Multiplier | Score |
|---|---|---|
| Private or Public Company | 0.0 | **0** |
| Proprietorship | 1.0 | **10** |

> Max Score: **10**  
> Config keys: `TYPE_MAX_SCORE`, `TYPE_PROPRIETORSHIP_MULTIPLIER`, `TYPE_COMPANY_MULTIPLIER`

---

## Rule 2 — Registration Date (Business Age)

| Age | Multiplier | Score |
|---|---|---|
| < 1 year | 1.00 | **10** |
| 1 – 3 years | 0.75 | **7.5** |
| 3 – 5 years | 0.50 | **5** |
| > 5 years | 0.00 | **0** |

> Max Score: **10**  
> Config keys: `REGISTRATION_MAX_SCORE`, `REG_LESS_THAN_1_YEAR_MULTIPLIER`, etc.

---

## Rule 3 — Aggregate Turnover

| Turnover | Multiplier | Score |
|---|---|---|
| < 50 Cr | 1.0 | **5** |
| 50 – 100 Cr | 0.5 | **2.5** |
| > 100 Cr | 0.0 | **0** |

> Max Score: **5**  
> Config keys: `TURNOVER_MAX_SCORE`, `TURNOVER_LESS_THAN_50CR_MULTIPLIER`, etc.

---

## Rule 4 — GSTN Status

| Status | Multiplier | Score |
|---|---|---|
| Active | 0.0 | **0** |
| Cancelled (or any other) | 1.0 | **35** |

> Max Score: **35**  
> Config keys: `GST_STATUS_MAX_SCORE`, `GST_STATUS_ACTIVE_MULTIPLIER`, `GST_STATUS_CANCELLED_MULTIPLIER`

---

## Rule 5 — GSTR1 Filing Consistency

| Condition | Multiplier | Score |
|---|---|---|
| delay_count ≤ 1 | 0.0 | **0** |
| delay_count > 1 | 1.0 | **20** |

> Max Score: **20**  
> Config keys: `GSTR1_MAX_SCORE`, `GSTR1_DELAY_THRESHOLD`, `GSTR1_DELAYED_MULTIPLIER`

---

## Rule 6 — GSTR3B Filing Consistency

| Condition | Multiplier | Score |
|---|---|---|
| delay_count ≤ 1 | 0.0 | **0** |
| delay_count > 1 | 1.0 | **20** |

> Max Score: **20**  
> Config keys: `GSTR3B_MAX_SCORE`, `GSTR3B_DELAY_THRESHOLD`, `GSTR3B_DELAYED_MULTIPLIER`

---

## Score Summary

| Rule | Max Score |
|---|---|
| 1. GST Type | 10 |
| 2. Registration Date | 10 |
| 3. Turnover | 5 |
| 4. GSTN Status | 35 |
| 5. GSTR1 Consistency | 20 |
| 6. GSTR3B Consistency | 20 |
| **Total** | **100** |

---

## New GSTIN Handling

When a new GST number is submitted:

1. System checks if it already exists in the GRC score database.
2. If **not found** → creates an empty record in `gst_details` and assigns:
   - `score = 15`
   - `version = "DUMMY_VALUE"`
3. When the user **updates GST details** via the admin interface, the GRC score is automatically recalculated using the 6 rules above.

---

## Modifying Weights

To change any score weight:
1. Open [`GrcScoreConfig.java`](src/main/java/com/company/grc/config/GrcScoreConfig.java)
2. Update the relevant constant (e.g., `TYPE_MAX_SCORE`, `GST_STATUS_MAX_SCORE`)
3. Rebuild and deploy the backend

> Future: weights can be moved to a database table for live admin UI configuration without redeployment.
