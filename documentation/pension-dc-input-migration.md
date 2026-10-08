# Modelled pension pot and bequests: starting-input convention

SimPaths treats the benefit unit's imputed starting `wealthPensValue` as **one modelled pension pot**, allocated equally between partners. The pot grows while a person is not retired. At modelled retirement it is converted into a lump sum and pension income; the pension wealth stock becomes zero. A person already retired at the start keeps their recorded pension income, while their imputed starting stock is cleared at the first annual pension update. With `transferDcPensionAtDeath: true`, only a pot remaining **before modelled retirement** is transferred at natural death. Population-alignment exits do not trigger pension bequests. With `false`, pension wealth is not bequeathed; pooled non-pension bequests at natural death continue under either setting.

This is a simulation convention, not a claim that the WAS total is an observed transferable fund. Its source formula includes DC funds, DB rights, capitalised pensions already in payment and former-partner rights. In particular, WAS `dvpinpval` values an income stream rather than reporting a remaining fund. Treating the combined total as a pot for non-retired people can therefore overstate actual transferable balances. The model's `Retired` status is the conversion boundary; it does not model separate drawdown funds held after retirement.

## Data path

1. `input/InitialPopulations/compile/07_was_wealth_data.do` calculates the original total and also separates source DC, DB, pensions in payment and former-partner rights. `08_wealth_to_ukhls.do` and `09_finalise_input_data.do` can carry that split into a newly prepared initial population. These columns are useful for assessing the approximation but are **not required** to run the modelled-pot convention.
2. When a complete source split is present, SimPaths checks that it adds to `wealthPensValue`. It then uses the **total**, including source DB and in-payment valuations, as one modelled pot. When the legacy 2021 input supplies only the total, it does the same. Both paths divide the benefit-unit amount equally between partners.
3. New occupational and personal contributions enter this pot. On retirement the pot is exhausted through the model's lump sum and annuity calculation, and later pension income continues as a flow without a pension wealth stock. The inheritance rule can transfer the pot only if death occurs before this conversion.

## Existing input and reporting limits

The existing 2021 research `input.mv.db` and historical initial-population CSVs contain only the combined total. They can now run with the pension-transfer switch either on or off. The source split is still valuable for validating how much of the imputed starting pot came from pensions already in payment and for a future design that tracks broad pension wealth separately. It is not available in the current 2021 database. WAS donors are matched to UKHLS units on age, employment and other characteristics, but not explicitly on pension receipt; a donor's in-payment valuation can therefore enter a modelled pot. The equal partner split is another approximation.

The two ageing-scenario YAML files set `transferDcPensionAtDeath: true`, so both include modelled pension-pot bequests as well as housing and financial bequests at natural deaths. The archived ageing-scenario results were produced while population-alignment deaths also triggered bequests; rerun both scenarios under the current code. Run metadata and `inheritance_method.txt` record the chosen pension-transfer setting and the alignment-exit rule.

## Model approximations still present

- The occupational participation regression measures membership of any occupational scheme. Its contribution-rate equations are estimated from occupational DC contributions, and the current model treats subsequent occupational contributions as DC-style funds. Re-estimating participation for DC-only membership would improve the match but requires revised regression inputs.
- Source DB rights and source pensions-in-payment valuations enter the modelled pot for people whom SimPaths does not classify as retired. This may make modelled bequests larger than transferable pension funds in practice.
- The model's pension wealth stock is zero after retirement, even though WAS assigns a capital value to pensions in payment. A broad pension-wealth reporting measure that retains such capitalised rights would need to be constructed separately from the bequeathable pot.
- Pension assets are imputed to the benefit unit and divided equally between partners. The input does not identify each partner's exact ownership of each imputed pension component.
