/*******************************************************************************
* PROJECT:      SimPaths UK - simulated output validation
* OBJECT:       Existing WAS high-cost debt benchmarks and legacy CPI settings
* AUTHOR:       Liang Shi
* LAST UPDATE:  27 October 2026
*
* Run from validation root; optional arguments: workbook_file output_dir
* This reproduces the benchmark selection in Unsecured debt/validate_unsecured_debt.do,
* retaining all four age groups already present in the workbook. The workbook
* is read only; its previous simulated values are NOT imported as survey targets.
* These are existing weighted WAS summary values, not reconstructed microdata.
* R6/R7/R8 midpoint years are 2017/2019/2021. Intermediate years 2018/2020 are
* already interpolated in the workbook; do not interpolate the microdata again.
* Amounts are already in 2015 GBP. Shares/rates are stored here as fractions.
* The separate CPI table copies the original validator's five historical values
* (annual all-items CPI, 2015=100); it converts nominal SIMULATION amounts once.
* No new survey compilation, regressions or simulation execution is needed.
*******************************************************************************/
version 18
set more off
args workbook_file output_dir
if `"`workbook_file'"' == "" local workbook_file "./survey_inputs/high_cost_debt_validation.xlsx"
if `"`output_dir'"' == "" local output_dir "${dir_data}"
if `"`output_dir'"' == "" local output_dir "./data"
confirm file `"`workbook_file'"'
capture mkdir `"`output_dir'"'
import excel using `"`workbook_file'"', sheet("debt level") cellrange(A3:V22) clear
keep A C D F H I K M O Q S U
rename (A C D F H I K M O Q S U) ///
    (year was_interpolated was_round age_group was_n_total was_weight_sum ///
    was_prevalence was_mean_all was_mean was_p50 was_p90 was_p99)
isid year age_group
assert _N == 20 & inrange(year,2017,2021) & inrange(age_group,0,3)
bysort year: assert _N == 4
replace was_prevalence = was_prevalence / 100
assert inrange(was_prevalence,0,1)
assert was_interpolated == inlist(year,2018,2020)
assert missing(was_n_total) if was_interpolated
label define debt_age 0 "All ages" 1 "15-29" 2 "30-54" 3 "55-74", replace
label values age_group debt_age
char _dta[provenance] "Fixed existing WAS workbook summaries, debt level!A3:V22; no microdata rebuild"
char _dta[money_basis] "2015 GBP, as recorded in original validation workbook"
char _dta[time_basis] "Observed round midpoints 2017/2019/2021; interpolated 2018/2020"
sort year age_group
save `"`output_dir'/unsecured_debt_WAS_levels.dta"', replace

import excel using `"`workbook_file'"', sheet("transitions") cellrange(A2:P9) clear
keep A B C D G I K M O
rename (A B C D G I K M O) (from_round to_round end_year age_group ///
    was_n_matched was_n_entry_risk was_n_persist_risk was_entry was_persistence)
gen int start_year = end_year - 2
isid end_year age_group
assert _N == 8 & inlist(end_year,2019,2021) & inrange(age_group,0,3)
assert was_n_matched == was_n_entry_risk + was_n_persist_risk
assert inrange(was_entry,0,1) & inrange(was_persistence,0,1)
label define debt_age 0 "All ages" 1 "15-29" 2 "30-54" 3 "55-74", replace
label values age_group debt_age
char _dta[provenance] "Fixed WAS workbook summaries, transitions!A2:P9; age at origin"
sort end_year age_group
save `"`output_dir'/unsecured_debt_WAS_transitions.dta"', replace

clear
input int year double cpi
2017 103.4
2018 105.9
2019 107.8
2020 108.7
2021 111.6
end
isid year
label var cpi "Legacy annual CPI setting, 2015=100"
char _dta[source] "Historical CPI constants copied from Unsecured debt/validate_unsecured_debt.do"
save `"`output_dir'/unsecured_debt_cpi.dta"', replace
display as result "DEBT TARGETS READY: 20 annual WAS groups; 8 transition groups; legacy CPI table."
