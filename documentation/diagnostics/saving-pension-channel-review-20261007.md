# Initial review of saving, pension and inheritance channels

## 1. Scope and source version

This review responds to the three colleague comments about the ageing experiment. It inspects current simulation code and reads the two saved runs under `D:/BaiduSyncdisk/Job CeMPA/CeMPA papers/ageing_and_inequality/step1_wealth_income_inequality/simulation outcomes`. It does not change simulation settings, source microdata or presentation slides.

**The saved results use the superseded inheritance treatment.** Both `inheritance_method.txt` files contain `alignment_exits_trigger_bequests=false`. The current code recognises `DeathAlignment` as death, separately from `EmigrationAlignment`; alignment deaths now apply the same inheritance rules as natural deaths. The old fault was in inheritance's treatment of exits, not a population-alignment algorithm that classified every removal as emigration. The population algorithm already distinguished emigration and death. The shared-child and responsible-adult reference corrections also require reruns before presentation results are refreshed.

## 2. Does the age of the wealth holder matter?

Redistributing a positive non-pension estate between households does not itself create additional wealth. Its subsequent effects depend on recipients' income, consumption, returns, taxes and asset holdings. The current model is also an open simulated population: emigrants take wealth out and alignment-created entrants bring wealth in. Unresolved/unmatched payments and the treatment of minor trusts require their own accounting. Conservation within one paid estate is different from a constant aggregate wealth stock over time.

Our plotted headline is **mean wealth per benefit unit**, not only total wealth. Its denominator can change when population and family composition change, even when the annual population totals are the same. A larger older-unit share can change the mean if older units hold more wealth. The age-mix calculation describes that association; it does not establish how the older units acquired their wealth.

In the active non-pension module, consumption is predicted by the FW1a equation, not by a universally fixed saving fraction. See [BenefitUnit.updateDiscretionaryConsumption](D:/GitHub/SimPaths/src/main/java/simpaths/model/BenefitUnit.java:5421). The simple fallback with zero saving for a retired reference person applies only when explicit non-pension wealth projection is off. Consequently, retired units can have positive saving in the active module.

The recurring budget surplus is annual disposable income minus discretionary consumption. The stock update additionally includes retirement lump sums and net housing returns; inheritance is added at annual settlement. Financial investment income is already part of disposable income and must not be added to that budget twice. See [BenefitUnit.updateNonPensionWealth](D:/GitHub/SimPaths/src/main/java/simpaths/model/BenefitUnit.java:509) and [WealthNonPension.projectWealth](D:/GitHub/SimPaths/src/main/java/simpaths/model/benefitunit/WealthNonPension.java:65).

## 3. Are older and retired units net savers in the saved runs?

The read-only script joins 2070 `Person.csv` and `BenefitUnit.csv` by run and benefit-unit ID. Living persons have `demExitSample=NotYet`. A conservatively defined retired unit has at least one living member aged 18+ and **all** such members recorded as `labC4=Retired`. This is distinct from using reference-person age 65+ as a proxy. Results are equally weighted per complete unit, in 2015-price pounds.

| 2070 measure | Baseline | No ageing |
| --- | ---: | ---: |
| Mean recurring surplus, reference person under 65 | £11.46k | £10.40k |
| Surplus / disposable income, under 65, ratio of sums | 29.9% | 28.8% |
| Mean recurring surplus, reference person 65+ | £37.62k | £32.56k |
| Surplus / disposable income, 65+, ratio of sums | 43.1% | 42.6% |
| Units with all living members aged 18+ retired | 10,300 | 7,616 |
| Mean disposable income, these retired units | £87.94k | £76.23k |
| Mean discretionary consumption, these retired units | £50.17k | £44.18k |
| **Mean recurring surplus, these retired units** | **£37.77k** | **£32.05k** |
| Median recurring surplus, these retired units | £34.31k | £29.16k |
| Share of these retired units with a positive surplus | 89.5% | 88.4% |
| Surplus / disposable income, these retired units, ratio of sums | 43.0% | 42.0% |
| Mean investment income, these retired units | £81.63k | £66.49k |
| Mean private pension income, these retired units | £21.15k | £19.81k |

The ratio is the sum of recurring surpluses divided by the sum of disposable incomes; it is not the mean of individual ratios. It excludes pension contributions/stock changes, retirement lump sums, housing returns and inheritance receipts. Discretionary consumption is the measure used by the inspected wealth update; separately exported childcare and care expenditure should be reconciled before describing this as a comprehensive household saving rate.

**Interpretation:** these older/retired units are net savers on this recurring budget measure. Older units have a higher surplus-to-income ratio than younger units in both saved runs. Across scenarios, the within-group ratios are relatively close, while income levels and group sizes differ. A single year's differences do not explain the full accumulated wealth gap.

Investment income is substantial. For the retired units, disposable income less discretionary consumption **and** recorded investment income is negative: approximately -£43.85k baseline and -£34.44k no ageing. This is an accounting rearrangement, not a counterfactual that removes investment income while holding taxes and behaviour consistent. It shows why a claim that pension annuities alone fund the observed surplus would be unsupported.

## 4. Does earlier death preserve more pension wealth for bequests?

The mechanism is present. Before modelled retirement, a remaining pot can pass to beneficiaries. At pension access, [PrivatePension.projectPensionAccess](D:/GitHub/SimPaths/src/main/java/simpaths/model/person/PrivatePension.java:201) uses the pot to fund a lump sum and annuity, then sets the remaining stock to zero. The annuity's future payments are not capitalised in the model's reported wealth stock. Thus retirement changes the measured household pension stock even without a death; there is no extra remaining pot to destroy at a retired person's death.

| Immediate pension-pot cash payments, 2027–2070 | Baseline | No ageing |
| --- | ---: | ---: |
| Unscaled simulated cash | £738.11m | £760.19m |

Unused-pot cash payments are £22.08m (3.0%) higher without ageing in the **old saved runs**. New pension amounts held in minor trusts are separate: £3.81m baseline and £7.16m no ageing. Non-pension immediate cash over the same period is much larger: £20.40bn baseline and £13.50bn no ageing.

This modest difference is consistent with the proposed pension channel, but it does not isolate earlier death as its cause or imply a higher overall wealth stock. Death counts, recipient/estate counts and pot sizes all contribute. Alignment deaths were excluded from these payments. The corrected reruns should separately report natural and alignment death counts, retirement status, transferable pots and payments before drawing a new scenario conclusion.

## 5. Longer pension receipt and plausibility

The comment that longer-lived people receive annuities for longer refers intuitively to the **baseline**, rather than no ageing. However, a fixed-age-share target does not establish that every individual in no ageing dies younger: mortality and alignment must be examined separately. More older people in a cross-section is different from a matched individual's longer lifetime.

The code carries an already-paying pension forward while the recipient remains alive, with its modelled inflation adjustment. Pension payments enter income. Any part of the overall disposable budget not consumed can add to non-pension wealth. The positive recurring surpluses above demonstrate that this is possible in the saved simulation; they do not attribute the surplus uniquely to pension income or measure extra lifetime annuity receipts.

Positive saving among some retirees is economically possible, but the large aggregate ratios here require validation. [IFS research using ELSA](https://ifs.org.uk/publications/use-financial-wealth-retirement) found slow drawdown of real financial wealth in retirement. That evidence does not validate this model's 42–43% retired-unit recurring-surplus ratio, its high projected investment incomes, or continued saving through 2070. Validation should examine surplus distributions by age and wealth, consumption responses, returns and pension income after the corrected reruns.

## 6. Conclusions and reproduction

1. The colleague's questions identify useful economic channels. They do not by themselves establish a new coding defect.
2. The saved headline difference was affected by an inheritance treatment that excluded alignment deaths. It should be recomputed before assigning a mechanism to the slides.
3. Saving propensities matter, alongside asset composition/returns, income/tax treatment, pension annuitisation, population movements and the benefit-unit denominator.
4. Old results show positive recurring saving among retired units and slightly higher unused-pot bequests without ageing. Both findings need updating after the corrected runs.

Run `inspect_saving_pension_channels.py --study-dir "D:/BaiduSyncdisk/Job CeMPA/CeMPA papers/ageing_and_inequality/step1_wealth_income_inequality" --year 2070` using a Python environment with pandas. It prints JSON results and reads the method recorded in each source run. It does not change or export the microdata. The saved-run folder must be updated deliberately before rerunning this inspection.
