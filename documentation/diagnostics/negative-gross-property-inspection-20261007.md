# Gross property values and wealth-distribution checks — 7 October 2026

## 1. What was inspected

The analysis reads all 2021–2070 rows in `BenefitUnit.csv` for the current baseline and no-ageing runs, and both archived `simulation outcomes bequest v3` runs. Amounts are unscaled simulated-sample values in 2015-price pounds. A negative gross value is `wealthPrptyValue < -0.000001`; negative **net equity** is a separate measure and can be valid when mortgage debt exceeds a positive property value.

The script is [inspect_negative_gross_property.py](inspect_negative_gross_property.py). It writes annual denominators and counts, every affected observation, a history of the worst baseline unit, and overall 2070 Gini and top-10% wealth shares. It does not change the simulation code, source outputs, study README or slides.

## 2. Frequency in the current runs

| Measure | Baseline | No ageing |
| --- | ---: | ---: |
| All benefit-unit observations, 2021–2070 | 1,564,670 | 1,544,451 |
| Observations with negative gross property | 737 | 711 |
| Share of all observations | 0.0471% | 0.0460% |
| Distinct affected benefit-unit IDs | 636 | 624 |
| Negative gross observations with a positive mortgage | 1 | 1 |
| Gross property below −£1,000 | 5 | 4 |
| Gross property below −£10,000 | 1 | 1 |
| Affected units in 2070 | 4 of 33,978 | 8 of 32,994 |
| Share of units in 2070 | 0.0118% | 0.0242% |
| Most negative gross property, 2070 | −£177 | −£456 |
| Sum of negative gross values, 2070 | −£311 | −£778 |

The all-year counts are **unit-year observations**: a unit affected in two years is counted twice. The distinct-ID row counts each affected unit once over the export; this is not a fraction of the initial population, because benefit units form, exit and are cloned throughout the simulation.

There are no negative gross property observations in the starting year, 2021. Both scenarios have 49 in 2022, including the same large outlier. Almost all negative observations have zero mortgage debt; they therefore represent negative predicted equity being assigned directly to gross property. This is not floating-point roundoff: most negative values are tens of pounds, and there is also a large outlier.

The archived V3 runs have 694 and 806 negative observations, respectively (0.0444% and 0.0522%). They contain the same 2022 outlier. This confirms that the issue predates the latest inheritance corrections and is not peculiar to the no-ageing target.

## 3. Source in the housing projection

The current housing code follows this sequence:

1. Predict homeownership.
2. Predict transformed **net housing equity**, then set equity to `sinh(score + shock)` in [WealthHousing.java:96](D:/GitHub/SimPaths/src/main/java/simpaths/model/benefitunit/WealthHousing.java:96). This can be negative, which by itself is legitimate.
3. Predict mortgage incidence and a positive mortgage balance separately.
4. Set **gross property = net equity + mortgage debt** in [WealthHousing.java:142](D:/GitHub/SimPaths/src/main/java/simpaths/model/benefitunit/WealthHousing.java:142).

The code checks whether predicted amounts are finite, but does not check their joint feasibility. Valid property accounting requires:

**Net equity ≥ −mortgage debt**, equivalently **gross property ≥ 0**.

If equity is negative and there is no mortgage, gross property becomes negative immediately. If equity is more negative than the mortgage balance, gross property is negative even for a mortgage holder. Both situations occur in these outputs. The missing constraint is present in the repository's current `HEAD`, and `WealthHousing.java` has no working-tree diff from that version.

Initial persistent housing residuals are reconstructed from starting net equity in [WealthNonPension.java:117](D:/GitHub/SimPaths/src/main/java/simpaths/model/benefitunit/WealthNonPension.java:117). Optional initial residual caps are disabled by default in [WealthModule.java:27](D:/GitHub/SimPaths/src/main/java/simpaths/model/benefitunit/WealthModule.java:27). Such caps do not impose the required annual equity–mortgage constraint. The output does not export each regression score and innovation, so this inspection does not separately quantify their contributions to the extreme prediction.

## 4. The large case and its accounting consequences

Benefit unit **1992** has the following exported history. It is the same in all four runs for the large 2022 observation.

| Year | Gross property | Mortgage debt | Net housing equity | Investment income |
| --- | ---: | ---: | ---: | ---: |
| 2021 | £126,722 | £156,107 | −£29,385 | Not exported |
| 2022 | −£963,984 | £158,673 | −£1,122,657 | £3,031 |
| 2023 | £0 | £0 | £0 | £51,843 |

The starting-year negative equity is possible: the house is worth less than the mortgage. The projected 2022 **negative gross property** is impossible. In 2022 the unit has £47,545 total wealth and £50,160 pension wealth; with net housing equity of −£1,122,657, the residual financial balance is approximately **£1,120,042**.

This is not an immediate £964k reduction in total wealth. Total non-pension wealth is projected first from opening wealth and budget flows. Financial wealth is then the residual after housing equity in [WealthNonPension.java:82](D:/GitHub/SimPaths/src/main/java/simpaths/model/benefitunit/WealthNonPension.java:82). Consequently, the negative housing prediction creates an offsetting positive financial balance within the same annual total.

That allocation can affect later outcomes:

- Homeownership is defined as `gross property > 0` in [WealthHousing.java:178](D:/GitHub/SimPaths/src/main/java/simpaths/model/benefitunit/WealthHousing.java:178). The unit is therefore recorded as a non-homeowner in 2022, despite having positive mortgage debt.
- Annual investment income is generated from lagged financial balances in [BenefitUnit.java:5388](D:/GitHub/SimPaths/src/main/java/simpaths/model/BenefitUnit.java:5388), using the return calculation in [WealthFinancial.java:64](D:/GitHub/SimPaths/src/main/java/simpaths/model/benefitunit/WealthFinancial.java:64). The following year's £51,843 investment income is consistent with the large residual financial balance.
- Housing ownership transitions, future returns and subsequently accumulated wealth can therefore be affected. Small counts and small negative amounts in 2070 do not establish that the cumulative effect is negligible.

This inspection establishes a real housing consistency defect. It does **not** quantify its contribution to the final scenario wealth gap. Fixing the constraint and rerunning would be needed for that comparison; deleting affected rows from the final export would not repair their histories.

## 5. Recommended correction

Enforce joint consistency after the mortgage prediction, preserving legitimate negative equity where mortgage debt exceeds positive property value. Do not simply forbid all negative net equity. A minimal bound is `netEquity = max(predictedNetEquity, -mortgageDebt)`; the treatment of a zero-valued property and its homeowner flag should be specified together. If predicted homeowners must retain positive property, a positive floor or a documented resampling rule is needed instead.

Recompute the financial residual using the corrected equity, preserving the model's already-projected non-pension total for that year, and audit how often and by how much the feasibility correction binds. Focused tests should cover a mortgage-free owner, a mortgage holder with feasible negative equity, and a mortgage holder with infeasible predicted equity. No such correction has been applied as part of this inspection.

## 6. What the wealth-distribution results currently support

| Overall 2070 measure | Baseline | No ageing |
| --- | ---: | ---: |
| Mean net wealth per benefit unit | £1.495m | £1.546m |
| Median net wealth per benefit unit | £572.5k | £518.2k |
| Net-wealth Gini | 0.6382 | 0.6581 |
| Top 10% share of net wealth | 40.84% | 42.95% |

The mean–median comparison alone does not establish which distribution is more equal. The directly calculated Gini and top-10% share both indicate less **overall relative wealth inequality across benefit units** in the current baseline. This is not a measure of inequality between generations and does not show that every age group benefits from ageing. These are descriptive results from the current single-seed runs, subject to the housing issue above.

The top-share calculation uses equally weighted benefit units with a fractional boundary unit so that exactly 10% of the unit count is included. All units, including those without an exported reference-person age, are included.

## 7. What the estate-audit qualification means

The death and payment ledgers agree on death causes and recipients, and closing wealth agrees with the benefit-unit export. These checks have **not detected an estate accounting failure**. However, the export does not separately record every estate's asset and debt components immediately before settlement. Internal snapshots are taken in [InheritanceModule.java:243](D:/GitHub/SimPaths/src/main/java/simpaths/model/InheritanceModule.java:243), while the exported ledgers are written in [InheritanceModule.java:615](D:/GitHub/SimPaths/src/main/java/simpaths/model/InheritanceModule.java:615).

For example, seeing £100,000 paid to heirs does not independently establish that the deceased estate available for distribution was £100,000. A complete independent conservation check would also need the source estate amount before settlement and its disposition into cash, minor trusts, retained survivor wealth and any separately recorded out-of-sample amounts or writeoffs. This is a limit of what the saved exports can verify, rather than evidence that the bequest results are wrong. The housing defect identified above is a separate, observed issue.

## 8. Reproduction

Run the supplied script with pandas and NumPy installed:

```powershell
python inspect_negative_gross_property.py --study-dir 'D:\BaiduSyncdisk\Job CeMPA\CeMPA papers\ageing_and_inequality\step1_wealth_income_inequality' --output-dir '.\negative_property_outputs'
```

Outputs include `negative_property_inspection.json`, four `*_negative_property_rows.csv` files, and `worst_baseline_unit_history.csv`. They contain a complete annual frequency series and the observations behind this report.
