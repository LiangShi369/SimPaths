/*******************************************************************************
* Copy to wealth_validation_config.local.do and edit the paths below.
* Run from the repository root; paths may also be absolute, including spaces.
* Keep raw/licensed survey data and machine-specific configs outside Git.
*******************************************************************************/
global wealth_code_root "./validation/02_simulated_output_validation"
global wealth_output_root "./validation/02_simulated_output_validation/wealth_outputs"
global wealth_runs_file "./validation/02_simulated_output_validation/wealth_runs.local.csv"
global wealth_initial_year 2015

* Enable each module independently; disabled modules do not require their inputs.
global wealth_consumption 1
global wealth_housing 1
global wealth_pensions 1
global wealth_debt 1

* Compiled survey data: see README_wealth_validation.md for required definitions.
global wealth_LCF_dir "/path/to/compiled_LCFS"
* One consolidated WAS input supplies housing, pension rates and high-cost debt.
global wealth_WAS_file "/path/to/compiled_WAS/was_wealthdata_5_8_longitudinal.dta"
* UKHLS microdata for contributor shares/personal wealth; WAS supplies contribution rates.
global wealth_UKHLS_file "/path/to/UKHLS_pooled_ipop.dta"
* No Excel workbook or second WAS compilation is required.
* Optional explicit precomputed UKHLS target .dta; leave blank to rebuild from
* microdata. It must follow the target schema and declare fixed_excel provenance
* when using saved workbook survey values; unavailable counts remain missing.
global wealth_UKHLS_targets_file ""

* Optional comparison windows; defaults below retain the original settings.
global wealth_consumption_first 2016
global wealth_consumption_last 2023
global wealth_housing_first 2015
global wealth_housing_last 2021
global wealth_pensions_first 2015
global wealth_pensions_last 2021
global wealth_pensions_rate_first 2016
global wealth_debt_first 2017
global wealth_debt_last 2021
