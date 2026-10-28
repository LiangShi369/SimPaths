/*******************************************************************************
* Synthetic WAS target checks. No external survey file or workbook is needed.
* Checks weighted BU coverage, interview-year deflation, origin age, destination
* weights, exact interview gaps, missing stocks/ages and adjacent interpolation.
*******************************************************************************/
version 18
clear all
set more off
args validation_root
if `"`validation_root'"' == "" local validation_root "./validation/02_simulated_output_validation"
capture mkdir `"`validation_root'/checks"'
local work `"`validation_root'/checks/debt_target_test"'
capture mkdir `"`work'"'
capture log close _all
log using `"`work'/target_test.log"', text replace
tempfile source bad_source
set obs 20
gen long was_pid=mod(_n-1,5)+1
gen byte was_round=5+floor((_n-1)/5)
gen long case=was_pid
gen byte bu=1
gen byte bu_rp=1
gen int year=2005+2*was_round
replace year=year-1 if was_pid==2
replace year=2022 if was_pid==4 & was_round==8
gen double dwt=1
replace dwt=3 if was_pid==2
replace dwt=2 if was_pid==3
replace dwt=4 if was_pid==4
gen byte dvage17=5
replace dvage17=13 if was_pid==3
replace dvage17=. if was_pid==4
replace dvage17=8 if was_pid==5 | (was_pid==1 & was_round>=7)
gen double highcost_debt_proxy=0
replace highcost_debt_proxy=. if was_round==5 | was_pid==5
replace highcost_debt_proxy=101 if was_pid==2 & was_round==6
replace highcost_debt_proxy=106 if was_pid==2 & was_round==7
replace highcost_debt_proxy=108.9 if was_pid==2 & was_round==8
replace highcost_debt_proxy=107.8 if was_pid==1 & was_round==7
replace highcost_debt_proxy=120.5 if was_pid==4 & was_round==8
save "`source'", replace
do `"`validation_root'/do_files/03_create_WAS_validation_targets.do"' "`source'" `"`work'"' 2014 2022 "debt"
use `"`work'/unsecured_debt_WAS_levels.dta"', clear
assert _N==20
assert was_n_total==4 & was_weight_sum==10 if age_group==0 & !was_interpolated
assert abs(was_prevalence-.3)<1e-12 & abs(was_mean_all-30)<1e-12 if year==2017 & age_group==0
assert abs(was_mean_all-70)<1e-12 if year==2021 & age_group==0
assert abs(was_mean_all-55)<1e-12 if year==2020 & age_group==0
assert abs(was_mean_all-75)<1e-12 & was_mean==100 if year==2017 & age_group==1
assert abs(was_mean_all-87.5)<1e-12 if year==2018 & age_group==1
assert missing(was_n_total,was_weight_sum,was_round) if was_interpolated
assert was_n_total==0 & missing(was_mean_all) if year==2017 & age_group==2
assert missing(was_mean,was_p50,was_p90,was_p99) if year==2020 & age_group==2
assert was_mean_all==0 & missing(was_mean) if age_group==3
use `"`work'/unsecured_debt_WAS_transitions.dta"', clear
assert _N==8
assert was_n_matched==4 & abs(was_entry-1/7)<1e-12 & was_persistence==1 if end_year==2019 & age_group==0
assert was_n_matched==3 & was_entry==0 & abs(was_persistence-.75)<1e-12 if end_year==2021 & age_group==0
assert was_n_entry_risk==1 & was_entry==1 if end_year==2019 & age_group==1
assert was_persistence==0 if end_year==2021 & age_group==2
assert missing(was_persistence) if age_group==3
* Reject a pension-only dataset that cannot identify high-cost debt.
use "`source'", clear
drop highcost_debt_proxy
save "`bad_source'", replace
capture noisily do `"`validation_root'/do_files/03_create_WAS_validation_targets.do"' "`bad_source'" `"`work'"' 2014 2022 "debt"
assert _rc==111
display as result "WAS DTA TARGET CHECKS PASSED: weighting, prices, coverage, transitions and interpolation."
log close
