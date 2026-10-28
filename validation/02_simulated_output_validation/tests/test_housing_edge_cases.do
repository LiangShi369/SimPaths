* Small SYNTHETIC fixtures, not additional model simulations or reported results.
* Exercise multiple-run arithmetic, missing stocks, ties, nonpositive equity,
* empty mortgage samples and missing transition endpoints.
version 18
clear all
set more off
args validation_root survey_file
if `"`validation_root'"' == "" local validation_root ///
    "./validation/02_simulated_output_validation"
if `"`survey_file'"' == "" local survey_file `"`validation_root'/wealth_outputs/data/was_bu_validation_sample.dta"'
capture mkdir `"`validation_root'/checks"'
local work `"`validation_root'/checks/housing_edge_cases"'
capture mkdir `"`work'"'
capture log close _all
log using `"`work'/edge_case_checks.log"', text replace

set obs 168
gen int run = floor((_n-1)/84) + 1
gen int year = 2015 + mod(floor((_n-1)/12), 7)
gen long idBu = mod(_n-1, 12) + 1
gen double wealthPrptyValue = (100000 + idBu*1000 + (year-2015)*500) * (1 + .1*(run==2))
replace wealthPrptyValue = 0 if idBu >= 11
replace wealthPrptyValue = 100000 if idBu == 11 & year >= 2017
gen double wealthMortgageDebtValue = (40000 + idBu*100) * (1 + .2*(run==2)) if idBu <= 8
replace wealthMortgageDebtValue = 0 if missing(wealthMortgageDebtValue)
replace wealthMortgageDebtValue = 0 if year == 2017
replace wealthMortgageDebtValue = 50000 if year == 2020 & idBu <= 8
replace wealthPrptyValue = 100 if idBu == 1 & year == 2018
replace wealthMortgageDebtValue = 1000 if idBu == 1 & year == 2018
replace wealthPrptyValue = . if idBu == 2 & year == 2016 & run == 1
replace wealthMortgageDebtValue = . if idBu == 2 & year == 2016 & run == 1
drop if year == 2019
save `"`work'/fixture.dta"', replace

do `"`validation_root'/do_files/04_20_plot_housing_mortgage.do"' ///
    `"`survey_file'"' `"`work'/fixture.dta"' `"`work'/figures"' `"`work'"' 2015 2021 2015 "as_recorded"
use `"`work'/housing_annual_statistics.dta"', clear
assert n_total == 11 & n_all == 12 if source == 1 & run == 1 & year == 2016
assert nonpositive_share > 0 if source == 1 & year == 2018 & outcome == 1
assert n_hold == 0 & missing(mean) if source == 1 & year == 2017 & outcome == 2
use `"`work'/housing_annual_comparison.dta"', clear
assert n_runs == 2
assert sim_mean > 0 & high_mean > low_mean if outcome == 1
assert missing(sim_mean) & missing(low_mean) & missing(high_mean) if outcome == 2 & year == 2017
assert abs((high_mean - sim_mean) - (sim_mean - low_mean)) < 1e-8 if !missing(sim_mean)
use `"`work'/housing_quintile_statistics.dta"', clear
assert missing(mean) if n == 0
quietly count if source == 1 & year == 2020 & outcome == 2 & quintile > 0 & n == 0
assert r(N) > 0
use `"`work'/housing_transition_statistics.dta"', clear
assert n_pairs == 0 & missing(rate) if source == 1 & end_year >= 2019
use `"`work'/housing_transition_comparison.dta"', clear
assert missing(sim_rate) & comparable == 0 if end_year >= 2019
assert sim_rate == 0 if end_year == 2017 & inrange(rate_id,3,6)

* A window with no two-year endpoint interval must still produce annual results.
capture mkdir `"`work'/no_intervals"'
do `"`validation_root'/do_files/04_20_plot_housing_mortgage.do"' ///
    `"`survey_file'"' `"`work'/fixture.dta"' `"`work'/no_intervals/figures"' ///
    `"`work'/no_intervals"' 2020 2021 2020 "as_recorded"
use `"`work'/no_intervals/housing_transition_comparison.dta"', clear
assert _N == 0
use `"`work'/no_intervals/housing_annual_comparison.dta"', clear
assert _N == 4 & n_runs == 2
display as result "VERIFIED: synthetic multi-run, missing-stock, empty-sample, tied-quintile and unavailable-endpoint checks."
log close
