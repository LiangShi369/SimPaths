/*******************************************************************************
* Synthetic checks, not additional real model runs. Covers zero-holder groups,
* empty transition risk sets, origin ages, missing ages in all-BU denominators,
* one-time price conversion and two-run sample-SD bands.
* Argument: validation_root (run the target-preparation file first).
*******************************************************************************/
version 18
clear all
set more off
args validation_root target_dir
if `"`validation_root'"' == "" local validation_root ///
    "./validation/02_simulated_output_validation"
capture mkdir `"`validation_root'/checks"'
if `"`target_dir'"' == "" local target_dir `"`validation_root'/wealth_outputs/data"'
capture log close _all
log using `"`validation_root'/checks/unsecured_debt_edge_test.log"', text replace
local out `"`validation_root'/checks/unsecured_debt_edge_cases"'
capture mkdir `"`out'"'
tempfile simulation
set obs 112
gen double run = 1 + floor((_n-1)/56)
gen int year = 2015 + mod(floor((_n-1)/8),7)
gen long idBu = mod(_n-1,8) + 1
gen double ageRefPerson = .
replace ageRefPerson = 25 if idBu == 1
replace ageRefPerson = 35 if idBu == 2
replace ageRefPerson = cond(year<2019,45,55) if idBu == 3
replace ageRefPerson = 65 if idBu == 4
replace ageRefPerson = 75 if idBu == 5
replace ageRefPerson = 10 if idBu == 6
replace ageRefPerson = 55 if idBu == 8
gen double wealthUnsecuredDebtHighValue = 0
replace wealthUnsecuredDebtHighValue = cond(run==1,100,300) if inlist(idBu,2,5,7,8) | (idBu==3 & year>=2019)
isid run year idBu
save "`simulation'", replace
do `"`validation_root'/do_files/04_22_plot_unsecured_debt.do"' ///
    `"`target_dir'/unsecured_debt_WAS_levels.dta"' ///
    `"`target_dir'/unsecured_debt_WAS_transitions.dta"' ///
    "`simulation'" `"`target_dir'/unsecured_debt_cpi.dta"' ///
    "`out'" "`out'" 2017 2021 2015
use `"`out'/unsecured_debt_annual_statistics.dta"', clear
assert _N==40
assert n_total==8 if age_group==0
assert n_hold==4 & abs(prevalence-.5)<1e-12 if age_group==0 & year==2017
assert n_hold==5 & abs(prevalence-.625)<1e-12 if age_group==0 & year>=2019
assert n_total==1 & n_hold==0 & mean_all==0 & prevalence==0 if age_group==1
assert missing(mean) & missing(p50) & missing(p90) & missing(p99) if age_group==1
use `"`out'/unsecured_debt_annual_comparison.dta"', clear
assert expected_runs==2
assert nr_mean==0 & missing(sim_mean) & missing(low_mean) & missing(high_mean) if age_group==1
assert nr_prevalence==2 & sim_prevalence==0 if age_group==1
merge m:1 year using `"`target_dir'/unsecured_debt_cpi.dta"', assert(match) nogen
assert abs(sim_mean-200*100/cpi)<1e-12 if age_group!=1
assert abs(sd_mean-sqrt(20000)*100/cpi)<1e-12 if age_group!=1
assert abs(low_mean-(200-1.96*sqrt(20000))*100/cpi)<1e-12 if age_group!=1
assert abs(high_mean-(200+1.96*sqrt(20000))*100/cpi)<1e-12 if age_group!=1
use `"`out'/unsecured_debt_transition_statistics.dta"', clear
assert _N==16
assert n_matched==8 if age_group==0
* ID 3 changes age group at destination: entry must remain in origin group 2.
assert n_entry_risk==1 & n_entry_event==1 & entry==1 if end_year==2019 & age_group==2
assert n_entry_risk==0 & missing(entry) if end_year==2021 & age_group==2
assert n_persist_risk==0 & missing(persistence) & entry==0 if age_group==1
use `"`out'/unsecured_debt_transition_comparison.dta"', clear
assert expected_runs==2
assert nr_entry==0 & missing(sim_entry) & missing(low_entry) & missing(high_entry) if end_year==2021 & age_group==2
assert sim_entry==1 & sd_entry==0 & low_entry==1 & high_entry==1 if end_year==2019 & age_group==2
* A narrower valid window must filter the CPI table and transition endpoints,
* rather than failing on unused CPI years from the default target table.
local shorter `"`out'/short_window"'
capture mkdir `"`shorter'"'
do `"`validation_root'/do_files/04_22_plot_unsecured_debt.do"' ///
    `"`target_dir'/unsecured_debt_WAS_levels.dta"' ///
    `"`target_dir'/unsecured_debt_WAS_transitions.dta"' ///
    "`simulation'" `"`target_dir'/unsecured_debt_cpi.dta"' ///
    "`shorter'" "`shorter'" 2019 2021 2015
use `"`shorter'/unsecured_debt_annual_statistics.dta"', clear
assert _N==24 & inrange(year,2019,2021)
use `"`shorter'/unsecured_debt_transition_statistics.dta"', clear
assert _N==8 & start_year==2019 & end_year==2021
display as result "EDGE CASES PASSED: zero holders, empty risk sets, origin age groups, missing ages, CPI once, two-run SD bands."
log close
