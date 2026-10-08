# How the revised ageing runs address the colleague comments

## 1. Which implementation is being assessed?

This uses the current `simulation outcomes/baseline/csv` and
`simulation outcomes/counterfactual_no_ageing/csv` exports, with natural and
population-alignment deaths triggering bequests, and emigrants excluded.
The run flags confirm pension transfers are enabled. It supersedes the
earlier inspection that excluded alignment deaths.

The refreshed presentation counts current benefit units with a living adult
aged 18+. Wealth is for the whole unit; age is its reference person's age.
Amounts are simulated, unscaled 2015-price pounds. One run per scenario is
available, and the negative-gross-property defect remains unresolved.

## 2. Are bequests visible in younger groups without ageing?

Yes. Mean 2070 wealth for units aged 24–54 is approximately **£512k baseline
versus £700k no ageing**. For ages 55–74 it is **£2.310m versus £2.903m**.

| Receipts over 2027–2070 | Baseline | No ageing |
| --- | ---: | ---: |
| All-age immediate cash | £27.59bn | £33.58bn |
| Cash to units aged 24–54 | £7.82bn | £13.83bn |
| Average receipts per unit per year, 24–54 | £12.84k | £22.37k |
| Average receipts per unit per year, 55–74 | £51.21k | £53.74k |

The period results and the 2070 snapshot are different. At ages 55–74,
receipts averaged over the period are slightly higher without ageing, while
the baseline is higher in 2070 alone. Means include units receiving nothing.
These observations support the proposed redistribution channel, but the
scenario comparison does not estimate the causal contribution of bequests
to subsequent wealth.

## 3. Does counting receipts separate timing from estate size?

It helps distinguish receipt incidence from cash value, but does not isolate
timing by itself. For example:

| Recipient-unit observations, 2027–2070 | Baseline | No ageing |
| --- | ---: | ---: |
| Unit-years with positive cash receipts, 24–54 | 10,338 | 17,547 |
| Share of all unit-years in that age range | 1.70% | 2.84% |
| Unit-years with positive cash receipts, 55–74 | 24,222 | 25,341 |
| Share of all unit-years in that age range | 7.18% | 7.51% |

These count one receiving unit once in each year, even if it receives
multiple payments. They do not count unique estates, separate bequest
events, or unique lifetime recipients. The same unit can appear in several
years. No ageing has higher younger receipt incidence, but does not show
lower incidence at 55–74 on this period measure. Death counts, family links,
synthetic matching and the population at risk also affect receipt counts.
A same-person lifetime comparison would be needed to establish earlier
inheritance for particular recipients.

## 4. Does earlier death preserve pension pots for beneficiaries?

The model transfers a remaining unannuitised pot at its owner's death. At
pension access, the pot funds a lump sum and annuity and becomes zero;
there is no remaining fund to transfer at a later death.

| Pension transfers, 2027–2070 | Baseline | No ageing |
| --- | ---: | ---: |
| Immediate unused-pot cash transfers | £895.69m | £1,574.30m |
| New pension allocations to minor trusts | £5.24m | £6.96m |

Immediate pension cash is **£678.61m, or 75.8%, higher without ageing**.
This is consistent with the proposed pension-bequest channel. It does not
establish that earlier death alone causes the difference: pot sizes, death
counts and retirement status also matter. The remaining pot and future
annuity payments are distinct accounting objects; pensions in payment are
not included as a capitalised stock in these model projections.

## 5. Are younger units saving less, and are retirees net savers?

| 2070 recurring budget measure | Baseline | No ageing |
| --- | ---: | ---: |
| Disposable income less discretionary consumption, ages 18–64 | £12.24k | £13.98k |
| Above surplus / disposable income, ages 18–64 | 30.1% | 30.7% |
| Disposable income less discretionary consumption, ages 65+ | £41.91k | £42.98k |
| Above surplus / disposable income, ages 65+ | 43.3% | 41.6% |

Ratios are ratios of sums, rather than averages of individual ratios.
Younger no-ageing units consume more per unit, but have more disposable
income; this does **not** support a lower younger saving propensity on this
budget measure. Older units retain a larger proportion of disposable income
than younger units in both runs.

For a retirement check independent of age, restrict to units whose **all
living adults aged 18+ are recorded as retired**:

| 2070 retired-unit measure | Baseline | No ageing |
| --- | ---: | ---: |
| Units | 10,262 | 7,658 |
| Mean disposable income | £97.05k | £103.06k |
| Mean discretionary consumption | £54.91k | £60.48k |
| Mean recurring surplus | £42.14k | £42.59k |
| Units with a positive surplus | 89.8% | 89.1% |
| Mean investment income | £96.30k | £109.41k |
| Mean private pension income | £21.05k | £19.20k |

Retired units are therefore net savers on the model's recurring budget
measure. This excludes separate childcare/social-care spending and is not
a comprehensive saving rate. Positive retiree saving is possible, but the
large modelled surpluses and investment incomes require validation; these
outputs alone do not establish their empirical plausibility through 2070.

Investment income is already in disposable income. It must not be added
again when describing saving. The positive surplus is supported by the
whole budget, rather than pension annuities alone. The baseline has more
older units, but that does not show the same person receives an annuity for
longer. Lifetime pension receipts have not been isolated in this analysis.

## 6. What becomes of the headline explanation?

Total simulated wealth in 2070 is similar: **£50.79bn baseline versus
£50.99bn no ageing**. The no-ageing mean is higher (**£1.588m versus
£1.525m**), while the baseline median is higher (**£608k versus £566k**).
There are fewer current units without ageing. The former headline that
ageing substantially raises mean wealth is superseded.

Redistribution alone creates no new wealth. Later income, consumption,
returns, annuitisation, asset allocation, entrants and exits can affect the
stock. At recorded reference ages, replacing no-ageing age shares with
baseline age shares raises mean wealth by £209.6k, while within-age
differences offset it by £275.8k. This describes opposing terms; it does not
causally split saving, returns and inheritance.

## 7. Evidence and reproduction

- Presentation tables: `matlab/figures/income_consumption_broad_age_2070.csv`,
  `wealth_by_age_24plus.csv`, `income_financial_age_mix_5year_2070.csv`.
- Receipt amounts and incidence:
  `diagnostics_20261007_recipient_age/period_bequests_unit_age_24_54_55_74.csv`.
- Pension and retired-unit check: run
  `documentation/diagnostics/inspect_saving_pension_channels.py` with the
  current study directory and `--year 2070`.
- Saved current check:
  `target/analysis_refresh_20261007/current_saving_pension_channels.json`.

The current payment checks found no inactive recipients, unmatched cash
joins or emigration donors. They do not independently reconcile every
estate's pre-settlement source components. That is an audit limitation,
not a detected payment failure.
