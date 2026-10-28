# Wealth-module output validation

These Stata 18+ do-files compare **completed** SimPaths UK simulations with supplied survey data. They prepare validation inputs, calculate statistics and export figures and datasets. They do not run simulations, estimate behavioural equations or rebuild raw WAS/LCFS waves.

The original `do_files/00_master.do` and modules `04_01`–`04_18` remain the default workflow. The optional wealth entry point runs consumption (`04_19`), housing/mortgages (`04_20`), pensions (`04_21`) and high-cost unsecured debt (`04_22`).

## Configure and run

1. From the repository root, copy `wealth_validation_config.example.do` to `wealth_validation_config.local.do` in this validation directory. Edit the code/output roots, survey paths, initial simulation year and enabled modules. Only enabled modules require their survey/CSV inputs. The parent of the output root must already exist.
2. Copy `wealth_runs.example.csv` to `wealth_runs.local.csv`. Supply one row per **independent completed simulation**, with columns `run_id,csv_dir`. `csv_dir` points to that run's CSV directory, not its enclosing run folder. Use unique integer labels and distinct directories; labels are assigned consistently to BU and Person records regardless of their exported `run` value. Quote CSV directory values containing commas. Relative paths are interpreted from Stata's working directory.
3. Supply the survey inputs described below; they are not distributed with these files.
4. Run from the repository root:

```stata
do "validation/02_simulated_output_validation/do_files/00_master_wealth.do" ///
   "validation/02_simulated_output_validation/wealth_validation_config.local.do"
```

Absolute paths and paths containing spaces are supported. Local configuration, private inputs and generated outputs are ignored by Git. The master creates `data`, `graphs` and `checks` within the configured output root. Repeating a run replaces generated files there; choose a separate output root for another scenario.

To run wealth modules after the original modules, set `run_wealth_validation` to `1` and `wealth_validation_config` to your configuration path in `00_master.do`. The original UKHLS and simulation settings are still required for the original modules. Wealth-only validation uses `00_master_wealth.do` directly, without requiring those original modules' inputs.

The wealth master calls `01_prepare_wealth_simulated_data.do` to create the four prepared simulation datasets. This import stage takes the place of the original `01`/`02` person-merged preparation **for these four modules only**: BU wealth must not be repeated and summed across people. The importer uses double precision except for the two legacy float consumption fields, validates within-run identifiers and checks that every supplied run starts in the configured initial year.

## Survey inputs and provenance

| Module | Required external input | Target preparation |
| --- | --- | --- |
| Consumption | `lcfs_sorted.dta`, compiled from LCFS with the Council Tax definitions below | `03_create_LCF_validation_targets.do` |
| Housing/mortgage | Base `was_wealthdata_5_8_longitudinal.dta`, plus the enriched WAS debt file | `03_create_WAS_validation_targets.do` |
| Pension contribution rates | The same WAS files | `03_create_WAS_validation_targets.do` |
| Pension contributor shares and wealth | `UKHLS_pooled_ipop.dta` | `03_create_UKHLS_pension_validation_targets.do` |
| High-cost unsecured debt | Enriched `was_wealthdata_5_8_longitudinal.dta` containing `highcost_debt_proxy` | `03_create_unsecured_debt_validation_targets.do` |

The survey preparation scripts also run individually with explicit path arguments, documented in their headers. Their relative fallback input directory is `survey_inputs`; use the configuration to point to existing files elsewhere. All survey inputs are read-only `.dta` files. Pension shares/wealth are reconstructed from UKHLS microdata; debt targets are reconstructed from enriched WAS microdata. No Excel workbook is required by the wealth workflow.

**LCFS:** the compiled file must contain `year caseno bu age female na nk weighta wght consmpn disp_inc income_scope_ok`, plus string tags `income_definition="personal_net_ct"` and `expenditure_definition="broad_net_ct"`. Both income and broad expenditure deduct the same adult-share allocation of Council Tax/NI domestic rates. Estimation of consumption remains upstream. Targets retain complete-income-scope one/two-adult BUs with positive weights and nominal weekly income at least £10. The statistic is the weighted mean of BU-level C/Y, not the ratio of aggregate means. Simulation retains its original monthly-income £10 screen; these survey/simulation screens differ in units and price basis.

**WAS:** the base file is the person-level, longitudinal R5–R8 compilation used for wealth/pension modelling. It must have unique `was_pid was_round`, household/BU identity, `was_link_key`, one `bu_rp` reference record per BU, demographic/weight fields, wealth stocks, earnings, contribution rates and membership. The target script lists and checks required names. It keeps the full 2014–2022 interview-year range for linked housing transitions. The enriched file supplies only the 14 debt components listed in that script. Its person/round, interview year, household, BU, reference flag, sex and linkage key must match exactly; base wealth/pension values are preserved. If necessary, a debt-component-only extract with those identity fields is sufficient. Do not pass a base file already containing the same debt fields: the compiler deliberately rejects silent overlap.

**UKHLS pension targets:** set `wealth_UKHLS_file` to `UKHLS_pooled_ipop.dta`. Preparation follows `Pension Wealth/pension_wealth_surv.do` (Justin van de Ven, 12 August 2026). It lowercases names and requires `idbenefitunit pno stm dag les_c4 contrateopee contrateoper contratepp total_pensions`. All statistics are unweighted. OP and PP contributor shares use positive combined OP contributions and positive PP contributions, both overall and among `les_c4==1` people; no WAS earnings screen applies.

The reference defines adults as `dag>17` and counts them by `idbenefitunit` over the **full pooled file before selecting years**. It assigns `total_pensions/na` to adults and zero to children, using the original float proxy. The positive-holder mean and annual own-wealth quintile means are calculated from that proxy. Neither the grouping nor the proxy is replaced by another personal-wealth definition. The literal Stata comparisons are preserved, including Stata's ordering of numeric missing values above finite values. Missing contribution/age counts are saved for audit rather than silently changing the reference's classification.

Outputs are `ukhls_pension_targets.dta` (plus CSV), including actual annual/employed/positive-holder/quintile counts, and `ukhls_pension_validation_sample.dta` for auditing the flags and proxy. The pension plotting script reads these computed targets. Its comments explicitly distinguish **UKHLS shares/wealth** from **WAS contribution rates**. It does not import fixed Excel benchmark values.

**Debt targets:** set `wealth_WAS_debt_file` to the enriched longitudinal WAS `.dta`. A debt-only run needs no base pension compilation and no workbook. Required numeric fields are `was_pid was_round case bu bu_rp year dwt dvage17 highcost_debt_proxy`. The earlier pension-only `.dta` lacks the proxy and cannot supply these targets. Preparation selects observed BU reference records with valid positive integer `dwt` and observed nonnegative debt, without regression restrictions. It calculates weighted annual prevalence and unconditional/positive-holder balance statistics, labels rounds 6/7/8 by midpoints 2017/2019/2021, and interpolates adjacent summary estimates for 2018/2020. Observed counts and weight sums are not interpolated. Missing debt is excluded, never treated as zero.

Two-year survey transitions link `was_pid` at adjacent rounds, with reference-person status at both endpoints, an exact two-year interview gap, origin age bands and destination cross-sectional weights. Unknown ages remain in all-age comparisons. This reconstructs the original counts and weighted rates directly from microdata.

To reproduce the existing monetary benchmarks, nominal WAS debt is deflated by **interview-year CPIH (ONS L522)** before round aggregation. The fixed annual 2016–2022 CPIH constants are embedded in the do-file, citing the [ONS historical release](https://www.ons.gov.uk/economy/inflationandpriceindices/timeseries/l522/mm23/previous/v103); no external price-index file or network access is required. The previous simulation **CPI (D7BT)** constants are retained separately, preserving the existing simulation calculations. CPIH and CPI are different series: this inherited distinction is now explicit. Reconstructed amounts can differ from the old workbook's stored values by float/rounding precision. The module does not validate total or low-cost unsecured debt.

## Simulated fields and conventions

| Export | Fields used by enabled modules |
| --- | --- |
| BenefitUnit.csv | `time id_BenefitUnit`; consumption: `ageRefPerson yDispMonth xDiscConsumptionAnnual`; housing: `wealthPrptyValue wealthMortgageDebtValue`; debt: `ageRefPerson wealthUnsecuredDebtHighValue` |
| Person.csv | Pensions only: `time id_Person idBu labC4 contRateOPEe contRateOPEr contRatePP wealthPensValue` |

Pension contribution rates are exported fractions and converted once to percentages in comparisons. Pension wealth uses person-level total private pension wealth, not BU totals or PP-only wealth. Housing amounts retain the original as-recorded convention; WAS stocks are interview-year nominal GBP, with no newly inferred real-price label. Outcome quintiles for housing and pension wealth rank the outcome among holders, not income. Consumption ranks national income within each year/source/run before selecting age bands. Its prepared `yDispMonth` and `xDiscConsumptionAnnual` retain the original float storage: increasing precision before ranking can split formerly tied incomes and move observations between quintiles. Other simulation numerical fields retain double precision.

Every statistic is calculated within run first, then averaged equally across runs. Period summaries average years within run before calculating variation across runs. Bands show mean ±1.96 sample SD **between runs**, not confidence intervals for the simulation mean; a single run has no band. Unavailable groups remain missing. Counts and sample definitions are retained in the outputs. All line figures include zero on the y-axis.

**Initial population for high-cost debt:** the relevant proxy is unavailable in WAS R5 (midpoint 2015), first observed in R6 (2016–2018, midpoint 2017). A corresponding **2017 initial population built via SimPathsStart is recommended for this validation**. The current missing-to-zero donor preparation can understate 2015 initial holdings. A 2015 baseline remains usable for assessing the model with that documented limitation. Dropping early years or changing the displayed initial year does not reset stocks; the importer requires actual first exported years to match the configuration. Verify donor coverage and missing handling even for 2017. Every debt figure carries the availability/recommendation note.

## Outputs

Default comparison windows preserve the original exercise: consumption 2016–2023, housing and pension shares/wealth 2015–2021, pension rates 2016–2021, high-cost debt 2017–2021. Edit the configuration to select supported subwindows; UKHLS years must exist in the supplied pooled file, and the debt comparison covers WAS R6–R8. Keep complete WAS rounds for linked transitions. Consumption excludes the initial simulation year.

| Graph subdirectory | JPGs | Main comparisons |
| --- | ---: | --- |
| `consumption` | 3 | Overall C/Y, age bands, income quintiles |
| `housing_mortgage` | 5 | Holding shares, net housing amounts, mortgage amounts, outcome quintiles, transitions |
| `pensions` | 3 | Contributor shares, wealth mean/top quintile, contribution-rate quintiles |
| `unsecured_debt` | 3 | Holding shares, balances, entry/persistence |

The `data` directory holds prepared inputs and 20 calculation/comparison datasets (3 consumption, 7 housing, 6 pension, 4 debt). Headers list each output. The run log is `checks/wealth_validation.log`. There are 14 reported JPGs in total. Supplementary plotting blocks are not retained; the additional statistics remain calculated and saved. Survey microdata, generated graphs and local paths are not committed.

## Checks

The UKHLS preparation is tested on a synthetic pooled file against all 70 statistics calculated by the reference method, including full-pool adult grouping, mixed-case variable names, missing-value semantics and actual counts. The three-figure pension plotting checks cover ties, empty contributor/quintile groups and correct annual/period run aggregation. No real UKHLS microdata or cloud file is needed for these tests. Real-data validation requires the supplied UKHLS file; a synthetic test is not evidence of fit to UKHLS. The original 18 modules are unchanged and their new master hook is disabled by default. For portable targeted checks, run from the repository root:

```stata
do "validation/02_simulated_output_validation/tests/test_wealth_import.do"
do "validation/02_simulated_output_validation/tests/test_UKHLS_pension_targets.do"
do "validation/02_simulated_output_validation/tests/test_pension_edge_cases.do"
```

The debt target-construction check uses synthetic WAS records and needs no external data:

```stata
do "validation/02_simulated_output_validation/tests/test_unsecured_debt_targets.do"
```

After preparing debt targets from the enriched WAS `.dta` under the default output root:

```stata
do "validation/02_simulated_output_validation/tests/test_unsecured_debt_edge_cases.do"
```

The debt test accepts an optional second argument for another target directory. The housing test additionally needs a prepared WAS BU target (optional second argument). Synthetic checks cover zero-holder and empty-risk groups, ties, missing ages/stocks, origin ages, one-time price conversion and between-run SDs. They write only ignored `checks` outputs. Run manifests/configuration for real simulations remain local.
