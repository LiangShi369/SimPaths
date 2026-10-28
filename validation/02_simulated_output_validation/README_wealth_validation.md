# Wealth-module output validation

These Stata 18+ do-files compare **completed** SimPaths UK simulations with supplied survey data. They prepare validation inputs, calculate statistics and export figures and datasets. The validation master does not run simulations or estimate behavioural equations. An optional, separate WAS compiler rebuilds the consolidated survey input from licensed raw waves; LCFS compilation remains upstream.

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
| Housing/mortgage | Consolidated `was_wealthdata_5_8_longitudinal.dta` | `03_create_WAS_validation_targets.do` |
| Pension contribution rates | The same consolidated WAS file | `03_create_WAS_validation_targets.do` |
| Pension contributor shares and wealth | `UKHLS_pooled_ipop.dta` | `03_create_UKHLS_validation_targets.do` (`pensions` mode) |
| High-cost unsecured debt | The same consolidated WAS file, including `highcost_debt_proxy` | `03_create_WAS_validation_targets.do` |

The survey preparation scripts also run individually with explicit path arguments, documented in their headers. Their relative fallback input directory is `survey_inputs`; use the configuration to point to existing files elsewhere. Survey inputs are read-only `.dta` files. Pension shares/wealth are reconstructed from UKHLS microdata; debt targets are reconstructed from consolidated WAS microdata. No Excel workbook is required by the wealth workflow.

Preparation is unified **by survey source**, matching the existing UKHLS convention. There are three `03` entry points, not one per wealth process:

- `03_create_UKHLS_validation_targets.do`: the original no-argument call retains demographics/income/care preparation; `pensions` mode reads the full original UKHLS source and prepares pension shares/wealth using the local `pension_wealth_surv.do` definitions. This keeps the reference's raw missing-value behavior and full-pool adult count separate from standard age cleaning/filtering.
- `03_create_WAS_validation_targets.do`: housing and pension-rate microdata plus debt targets from the same consolidated WAS file. Its fifth argument is `all` (default), `microdata` or `debt`; debt-only runs need only debt fields. The master selects the required scope and calls WAS preparation once.
- `03_create_LCF_validation_targets.do`: consumption targets from compiled LCFS.

For example:

```stata
do "validation/02_simulated_output_validation/do_files/03_create_UKHLS_validation_targets.do" ///
    "pensions" "/path/to/UKHLS_pooled_ipop.dta" "/path/to/targets" 2015 2021
do "validation/02_simulated_output_validation/do_files/03_create_WAS_validation_targets.do" ///
    "/path/to/was_wealthdata_5_8_longitudinal.dta" "/path/to/targets" 2014 2022 "all"
```

The UKHLS extension leaves the original no-argument processing body unchanged. Its pension branch exits after saving pension outputs, so a wealth-only run does not require the original demographic variables or the `fre` command.

**LCFS:** the compiled file must contain `year caseno bu age female na nk weighta wght consmpn disp_inc income_scope_ok`, plus string tags `income_definition="personal_net_ct"` and `expenditure_definition="broad_net_ct"`. Both income and broad expenditure deduct the same adult-share allocation of Council Tax/NI domestic rates. Estimation of consumption remains upstream. Targets retain complete-income-scope one/two-adult BUs with positive weights and nominal weekly income at least £10. The statistic is the weighted mean of BU-level C/Y, not the ratio of aggregate means. Simulation retains its original monthly-income £10 screen; these survey/simulation screens differ in units and price basis.

**WAS:** set `wealth_WAS_file` to one consolidated person-level R5–R8 `.dta`. It must contain unique `was_pid was_round`, household/BU identity, `was_link_key`, one `bu_rp` reference row per BU, demographics/weights, finalised debt/wealth stocks, earnings, pension contribution rates and membership. Both housing/pension preparation and debt preparation read this same file; no second-file merge occurs. Older pension-only or debt-only compilations are insufficient for the combined workflow. Target preparation preserves the full 2014–2022 interview-year range for housing transitions.

To rebuild this input from raw WAS, run the separate compiler (the parent of the output folder must exist):

```stata
do "validation/02_simulated_output_validation/do_files/02_compile_WAS_wealth_data.do" ///
    "/path/to/raw_WAS" "/path/to/dedicated_compiled_WAS" "/path/to/logs"
```

The compiler requires the household and person EUL files for rounds 5–8; exact filenames are listed in its two extraction loops. It retains the finalised debt balances and the associated `assets`, `wealth` and `ww` definitions. It additionally constructs `earnings earn_qtl partnered pp_contrate ocdc_contrate_emee ocdc_contrate_emer` using the earlier pension compiler's validation definitions. The percentage-of-earnings validation rates coexist with the finalised income-based contribution measures; they are different denominators, not interchangeable variables. Historical PP reallocation/tie behavior and missing-value arithmetic are documented and retained. The compiled file is saved after linkage/BU checks; this step does not estimate pensions or debt models. Use a dedicated output directory because named compiler intermediates are created and removed there.

**UKHLS pension targets:** set `wealth_UKHLS_file` to `UKHLS_pooled_ipop.dta`. Preparation follows `Pension Wealth/pension_wealth_surv.do` (Justin van de Ven, 12 August 2026). It lowercases names and requires `idbenefitunit pno stm dag les_c4 contrateopee contrateoper contratepp total_pensions`. All statistics are unweighted. OP and PP contributor shares use positive combined OP contributions and positive PP contributions, both overall and among `les_c4==1` people; no WAS earnings screen applies.

The reference defines adults as `dag>17` and counts them by `idbenefitunit` over the **full pooled file before selecting years**. It assigns `total_pensions/na` to adults and zero to children, using the original float proxy. The positive-holder mean and annual own-wealth quintile means are calculated from that proxy. Neither the grouping nor the proxy is replaced by another personal-wealth definition. The literal Stata comparisons are preserved, including Stata's ordering of numeric missing values above finite values. Missing contribution/age counts are saved for audit rather than silently changing the reference's classification.

Outputs are `ukhls_pension_targets.dta` (plus CSV), including actual annual/employed/positive-holder/quintile counts, and `ukhls_pension_validation_sample.dta` for auditing the flags and proxy. The pension plotting script reads these computed targets. Its comments explicitly distinguish **UKHLS shares/wealth** from **WAS contribution rates**. It does not import fixed Excel benchmark values.

The PR has no pension Excel importer, precomputed-target override or workbook fallback. A pooled file missing the pension contribution fields is insufficient for this validation: supply the compiled UKHLS version used by the reference method. Earlier locally verified Excel-based figures remain a separate historical check; they are not the PR's survey-data source.

**Debt targets:** set `wealth_WAS_file` to the consolidated longitudinal WAS `.dta`. A debt-only run uses that single file and no workbook. Required numeric fields are `was_pid was_round case bu bu_rp year dwt dvage17 highcost_debt_proxy`. The earlier pension-only `.dta` lacks the proxy and cannot supply these targets. Preparation selects observed BU reference records with valid positive integer `dwt` and observed nonnegative debt, without regression restrictions. It calculates weighted annual prevalence and unconditional/positive-holder balance statistics, labels rounds 6/7/8 by midpoints 2017/2019/2021, and interpolates adjacent summary estimates for 2018/2020. Observed counts and weight sums are not interpolated. Missing debt is excluded, never treated as zero.

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

After preparing debt targets from the consolidated WAS `.dta` under the default output root:

```stata
do "validation/02_simulated_output_validation/tests/test_unsecured_debt_edge_cases.do"
```

The debt test accepts an optional second argument for another target directory. The housing test additionally needs a prepared WAS BU target (optional second argument). Synthetic checks cover zero-holder and empty-risk groups, ties, missing ages/stocks, origin ages, one-time price conversion and between-run SDs. They write only ignored `checks` outputs. Run manifests/configuration for real simulations remain local.
