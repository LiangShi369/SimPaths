# Inspection of revised ageing runs — 7 October 2026

## 1. Inputs and scope

The new source folders are `simulation outcomes/baseline/csv` and `simulation outcomes/counterfactual_no_ageing/csv` within the ageing study. The archive is `simulation outcomes bequest v3`, whose baseline folder is named `benchmark`. Source microdata, presentation slides and the main study README were not changed.

Both copied YAML files retain 2021–2070, population size 50,000, seed 606, two synthetic recipients and pension transfers enabled. The population workbooks differ as intended. Both new method files record natural and alignment deaths triggering bequests, emigration excluded, and `pooled_estate_reference=highest_id_deceased_responsible_adult`.

All money below is **unscaled simulated-sample wealth in 2015-price pounds**. Age-group wealth/income uses the benefit unit's **recorded** reference-person age. There are 445 baseline and 596 no-ageing units without that age in 2070; they remain in all-unit totals but are excluded from the age-group means. This initial inspection does not reconstruct their ages from person records. The separately labelled recipient-age bequest results use recipients' own ages. Reported percentiles use NumPy's linear interpolation; full MATLAB refreshes may differ slightly because of percentile conventions.

## 2. Completed checks

1. Both benefit-unit exports cover all years 2021–2070, with unique unit-year keys and finite recorded wealth, pension, property, mortgage and unsecured-debt stocks. Gross pension and debt stocks are non-negative. A separate exception for gross property values is detailed in Section 7.
2. Both scenarios have identical annual mean wealth and benefit-unit counts through 2026, before target age shares diverge.
3. Every death-event entry matches a person-year exit. Settled deaths are unique, and both natural and alignment deaths are represented.
4. Every immediate cash recipient is present and living in the person export. Every transfer donor is a recorded death, with the correct recorded death cause.
5. No pooled-estate transfer uses a donor younger than 18. Responsible-adult selection is implemented in the source and regression-tested. The person export does not independently export the benefit unit's male/female responsible-adult roles. In particular, `demAdultChildFlag=True` is not an exclusion rule: an adult child living in a parental household can head a separate benefit unit.
6. No unused-pension cash/trust payment is attributed to a person recorded as retired at death.
7. No recorded synthetic allocation repeats the same recipient; actual recipient counts do not exceed intended counts.
8. No alignment-death estate is also recorded as an external death-alignment wealth outflow. Emigration and alignment-created entrants remain recorded separately.
9. Each exported 2070 benefit unit matches its closing inheritance balance exactly. Minor-trust stocks are finite and non-negative.

These checks support correct operation of the revised exit classification and payment logic. They are **not a complete independent estate-to-recipient conservation audit**: every source estate component immediately before settlement is not exported. Family-link completeness cannot be established from these outputs alone. Shared-child deduplication remains supported by its regression tests.

## 3. The headline comparison changes materially

| 2070 measure | New baseline | New no ageing | V3 baseline | V3 no ageing |
| --- | ---: | ---: | ---: | ---: |
| Mean net wealth per unit | £1.495m | £1.546m | £1.290m | £0.942m |
| Median net wealth per unit | £572.5k | £518.2k | £523.7k | £372.6k |
| Total simulated net wealth | £50.79bn | £50.99bn | £43.84bn | £31.05bn |
| Net-wealth Gini | 0.638 | 0.658 | 0.638 | 0.655 |
| Benefit units | 33,978 | 32,994 | 33,973 | 32,945 |

No ageing now has **3.4% higher mean wealth**, while total simulated wealth is only **0.41% higher**. It has fewer benefit units, so similar total wealth need not mean identical mean wealth per unit. The old narrative of substantially higher baseline mean wealth is superseded.

The baseline still has the higher median and lower overall wealth Gini. A higher no-ageing mean does not imply higher wealth throughout the distribution. Current baseline P10, P25 and median exceed no ageing; P75 and P90 are higher without ageing. The P90 values are £4.110m and £4.377m, respectively.

The versions differ in three code corrections. This comparison does not isolate each correction's causal contribution. The old 40% age-mix result must be recalculated before use.

## 4. Wealth by reference-person age in 2070

| Age | Baseline mean | No-ageing mean | No ageing minus baseline |
| --- | ---: | ---: | ---: |
| 24–34 | £168.1k | £207.4k | +£39.2k |
| 35–44 | £440.6k | £614.9k | +£174.3k |
| 45–54 | £1,009.7k | £1,421.8k | +£412.1k |
| 55–64 | £1,979.9k | £2,535.0k | +£555.1k |
| 65–74 | £2,653.2k | £3,259.7k | +£606.5k |
| 75–84 | £3,238.8k | £3,332.6k | +£93.8k |
| 85+ | £2,856.1k | £2,816.8k | −£39.3k |
| **24–54 combined** | **£507.7k** | **£687.9k** | **+£180.2k** |
| **55–74 combined** | **£2,296.6k** | **£2,853.1k** | **+£556.5k** |

No-ageing units have higher mean wealth through ages 75–84, with the largest absolute differences at ages 45–74. At 85+, the baseline is slightly higher. These are cross-sectional age means, not matched birth cohorts or typical-person gains.

Units with a recorded reference-person age of 65+ hold **67.1%** of all baseline wealth versus **54.9%** without ageing. These preliminary shares leave age-missing units unallocated; the established wealth-share analysis should reconstruct their reference ages when refreshed. Thus the recorded-age comparison still locates more wealth with older units in the baseline, despite higher no-ageing mean wealth within broad age groups. The age-known 65+/under-65 mean ratio is approximately **4.00** baseline and **3.42** no ageing. Age shares, age-group mean wealth and overall inequality describe different aspects of the distribution.

The 2070 net-financial balance excluding minor trusts averages about £1.026m baseline and £1.087m no ageing. Housing equity averages £397.6k versus £385.7k, and remaining pension pots £70.6k versus £71.9k. The mean-wealth reversal is mainly located in the financial balance; this is component accounting, not a causal attribution to inheritance or saving.

## 5. Bequests now include alignment deaths

| Deaths, 2021–2070 | Baseline | No ageing |
| --- | ---: | ---: |
| Natural deaths | 29,666 | 22,237 |
| Alignment deaths | 7,066 | 20,928 |

| Immediate cash receipts, 2027–2070 | Baseline | No ageing |
| --- | ---: | ---: |
| All cash | £27.59bn | £33.58bn |
| Unaccessed pension pots, included above | £0.896bn | £1.574bn |
| Cash attributed to natural deaths | £22.38bn | £16.74bn |
| Cash attributed to alignment deaths | £5.21bn | £16.84bn |

About half of no-ageing cash receipts now arise from estates attributed to alignment deaths. For a pooled estate involving both partners' deaths in the same year, the ledger uses its selected adult's cause. This attribution does not causally split the couple's estate between causes.

| Recipient's own age, 2027–2070 | Baseline cash | No-ageing cash | Baseline donor–recipient–year pairs | No-ageing pairs |
| --- | ---: | ---: | ---: | ---: |
| Under 24 | £0.14bn | £0.20bn | 313 | 453 |
| 24–54 | £7.06bn | £13.43bn | 9,537 | 17,370 |
| 55–74 | £19.00bn | £19.36bn | 28,164 | 28,868 |
| 75+ | £1.39bn | £0.60bn | 2,058 | 1,056 |

A pair combines cash components from the same deceased person to the same recipient in one year, avoiding treating pension and non-pension components as separate events. These are **counts, not receipt probabilities**: the age-group populations differ. They also do not classify receipts by the benefit unit reference person's age, as the earlier presentation table did.

Recipients aged 24–54 now receive substantially more cash without ageing, consistent with higher younger-unit wealth. This does not establish the amount of the accumulated wealth gap caused by bequests. Transfers redistribute existing stocks and interact with later consumption, saving and returns.

Over all 2021–2070, trust/other transfers recorded as out of sample comprise 58 rows (£3.64m) baseline and 158 rows (£16.53m) no ageing. These outcomes must remain visible in a complete conservation account; immediate cash alone is not the entire disposition of wealth.

## 6. Income, consumption and saving in 2070

| Annual mean per unit | Baseline under 65 | No ageing under 65 | Baseline 65+ | No ageing 65+ |
| --- | ---: | ---: | ---: | ---: |
| Disposable income | £40.65k | £45.65k | £96.29k | £102.47k |
| Investment income | £20.99k | £28.61k | £94.40k | £107.12k |
| Private pension income | £0.776k | £0.736k | £19.45k | £17.87k |
| Discretionary consumption | £28.40k | £31.65k | £54.28k | £59.31k |
| **Disposable income less discretionary consumption** | **£12.25k** | **£14.00k** | **£42.01k** | **£43.16k** |

No-ageing units have higher investment income, disposable income and consumption per unit in both broad age groups. Private pension income is higher in the baseline. Both groups retain a positive recurring budget surplus. These comparisons do not isolate pension income's contribution or represent an aggregate-demand/GDP effect.

A separate check identifies retired units conservatively: every living member aged 18+ must have `labC4=Retired`. Their mean recurring surplus is £42.14k baseline and £42.59k no ageing; ratios of total surplus to total disposable income are 43.4% and 41.3%. About 89.8% and 89.1% have positive recurring surpluses. Investment income remains substantial, so these figures should not be described as saving from annuities alone. The surplus is disposable income minus discretionary consumption; retirement lump sums, pension-stock changes, housing returns and bequests are separate. Separately recorded care expenditure requires reconciliation before calling this a complete saving rate.

## 7. Separate housing-stock issue found

| Negative gross property values | Baseline | No ageing |
| --- | ---: | ---: |
| Unit-year observations, 2021–2070 | 737 | 711 |
| Units in 2070 | 4 | 8 |
| Sum of their gross property values in 2070 | −£311.06 | −£778.25 |

Negative **net housing equity** can be legitimate. Negative **gross property value** cannot be interpreted as a property price. [WealthHousing.projectValues](D:/GitHub/SimPaths/src/main/java/simpaths/model/benefitunit/WealthHousing.java:66) predicts equity using an inverse-asinh equation, predicts mortgage debt separately, then sets gross property value to their sum without a non-negative price constraint. These predictions can be inconsistent.

The v3 archive also contains this issue: 694 and 806 unit-year observations. All four datasets contain the same extreme early observation, approximately −£964k in 2022, before the demographic scenarios diverge. This is a pre-existing housing projection issue, not a newly introduced bequest defect. The small negative sums in 2070 do not establish the effect of earlier invalid values on returns or ownership classification.

No housing code was changed in this inspection. Review a physically consistent constraint separately, preserving net-wealth accounting. These observations do not establish that the housing issue explains the scenario reversal.

## 8. Reproduction and presentation implications

The inspection folder contains:

- `check_inheritance_death_causes.py`: full death/recipient/cause audit; run with one scenario's CSV folder and save its JSON output.
- `inspect_revised_run_summaries.py`: current-versus-v3 annual coverage, stocks, age summaries and closing balances; run with `--output-json revised_run_inspection_20261007.json`.
- `inspect_saving_pension_channels.py`: 2070 recurring-surplus checks for age groups and retired units. It supports both baseline folder names and an `--outcomes-dir` archive argument.
- `revised_run_inspection_20261007.json`, `inheritance_death_audit_baseline_20261007.json` and `inheritance_death_audit_no_ageing_20261007.json`: results behind this note.

The summary programs require pandas/numpy; the death audit uses Python's standard library. The scripts read source data without modifying it. Their default study path points to the ageing study; use `--study-dir` if the study moves.

The existing MATLAB programs still refer to `benchmark`. Their input selection needs adapting to the new `baseline` folder before refreshing figures. The current study README, slide numbers and figures describe v3 and should not supply current numerical conclusions. Refresh wealth-by-age, distribution, components, income/consumption and bequest figures, then recalculate age-mix and same-age cohort results. Do not carry forward the old 40% age-mix explanation without recalculating it.
