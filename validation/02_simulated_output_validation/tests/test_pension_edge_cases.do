/*******************************************************************************
* Original-settings synthetic test: microdata-target counts, tied/empty quintiles,
* empty contributor years, and between-run SD after within-run period averaging.
* No additional actual SimPaths runs. Argument: validation_root.
*******************************************************************************/
version 18
clear all
set more off
args validation_root
if `"`validation_root'"' == "" local validation_root ///
    "./validation/02_simulated_output_validation"
capture mkdir `"`validation_root'/checks"'
capture log close _all
log using `"`validation_root'/checks/pension_edge_test.log"', text replace
local out `"`validation_root'/checks/pension_edge_cases"'
capture mkdir `"`out'"'
tempfile uk was sim
set obs 2
gen int year = 2015 + _n
foreach v in op_share pp_share op_employed_share pp_employed_share {
    gen double `v' = 100
}
gen double wealth_mean = 100
gen long n_all=12
gen long n_employed=12
gen long n_wealth=12
gen str16 benchmark_source="UKHLS"
forvalues q=1/5 {
    gen double wealth_q`q' = cond(`q'==1,100,.)
    gen long n_wealth_q`q' = cond(`q'==1,12,0)
}
save "`uk'", replace
clear
set obs 24
gen int year = 2016 + (_n>12)
gen long was_pid = mod(_n-1,12)+1
gen byte was_round = year-2010
gen byte sample_person = 1
gen double dwt_valid = 1
gen double valid_contRateOPEePct = 5
gen double valid_contRateOPErPct = 5
gen double valid_contRatePPPct = 5
gen byte valid_opContribFlag = 1
gen byte valid_ppContribFlag = 1
save "`was'", replace
clear
set obs 48
gen double run = 1+(_n>24)
gen int year = 2016+(mod(_n-1,24)>=12)
gen double idPers = mod(_n-1,12)+1
gen byte employed = 1
gen double contRateOPEe = cond((run==1 & year==2016)|(run==2 & year==2017),.01,.03)
gen double contRateOPEr = .05
gen double contRatePP = .04
replace contRatePP = 0 if run==2 & year==2017
gen double wealthPensValue = cond(run==1,100,300)
save "`sim'", replace
do `"`validation_root'/do_files/04_21_plot_pensions.do"' "`uk'" "`was'" "`sim'" "`out'" "`out'" 2016 2017 2016
use `"`out'/pension_annual_statistics.dta"', clear
assert n_all==12 & n_eligible==12 & weighted_n==12 if source==0 & outcome<=4
assert mean==100 if source==0 & outcome==8
use `"`out'/pension_quintile_statistics.dta"', clear
assert n==0 & missing(mean) if source==1 & quintile>1
use `"`out'/pension_annual_comparison.dta"', clear
assert expected_runs==2
assert abs(sim_mean-200)<1e-12 & abs(sim_sd-sqrt(20000))<1e-12 if outcome==8
assert abs(sim_low-(200-1.96*sqrt(20000)))<1e-12 if outcome==8
assert abs(sim_high-(200+1.96*sqrt(20000)))<1e-12 if outcome==8
assert abs(sim_mean-2)<1e-12 & abs(sim_sd-sqrt(2))<1e-12 if outcome==5
assert missing(sim_mean) & n_runs==1 & missing(sim_low) & missing(sim_high) if outcome==7 & year==2017
use `"`out'/pension_quintile_comparison.dta"', clear
assert abs(sim_mean-2)<1e-12 & abs(sim_sd)<1e-12 if outcome==5 & quintile<=1
assert abs(sim_low-2)<1e-12 & abs(sim_high-2)<1e-12 if outcome==5 & quintile<=1
assert missing(sim_mean) & n_runs==0 if quintile>1
assert missing(sim_mean) & n_runs==1 if outcome==7 & quintile<=1
* The PR requires microdata targets. Reject tagged fixed targets and unavailable
* source counts explicitly, without drawing a substitute survey comparison.
tempfile invalid_uk
use "`uk'", clear
char _dta[benchmark_basis] "fixed_excel"
save "`invalid_uk'", replace
capture noisily do `"`validation_root'/do_files/04_21_plot_pensions.do"' ///
    "`invalid_uk'" "`was'" "`sim'" "`out'" "`out'" 2016 2017 2016
assert _rc==198
use "`uk'", clear
replace n_all=.
save "`invalid_uk'", replace
capture noisily do `"`validation_root'/do_files/04_21_plot_pensions.do"' ///
    "`invalid_uk'" "`was'" "`sim'" "`out'" "`out'" 2016 2017 2016
assert _rc==9

display as result "EDGE CASES PASSED: UKHLS microdata/counts required; personal wealth; ties; empty contributors; correct annual/period two-run bands."
log close
