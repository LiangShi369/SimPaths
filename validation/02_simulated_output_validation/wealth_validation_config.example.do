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
global wealth_WAS_dir "/path/to/compiled_WAS"
* This enriched file alone is sufficient when only high-cost debt is enabled.
global wealth_WAS_debt_file "/path/to/enriched_WAS/was_wealthdata_5_8_longitudinal.dta"
* UKHLS microdata for contributor shares/personal wealth; WAS supplies contribution rates.
global wealth_UKHLS_file "/path/to/UKHLS_pooled_ipop.dta"
* High-cost debt uses wealth_WAS_debt_file directly; no debt workbook is required.

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
