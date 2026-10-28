/*******************************************************************************
* PROJECT:      SimPaths UK - simulated output validation
* OBJECT:       Rebuild high-cost unsecured-debt targets from longitudinal WAS
* AUTHOR:       Liang Shi
* LAST UPDATE:  28 October 2026
*
* Run: do "do_files/03_create_unsecured_debt_validation_targets.do" "WAS.dta" "data"
* Arguments: source_file output_dir. No Excel workbook is read or required.
* Supply the ENRICHED was_wealthdata_5_8_longitudinal.dta containing the BU-level
* highcost_debt_proxy. The earlier pension-only compilation lacks this field.
* Optional globals: file_WAS_debt, dir_data; otherwise use relative defaults.
*
* DEFINITIONS
* - One observed BU reference person per round (bu_rp==1), positive integer dwt,
*   observed nonnegative high-cost proxy. Missing debt is excluded, never zero.
* - Proxy: persistent card balances + mail order + explicit high-cost loans,
*   already compiled at BU level. Do not sum repeated stocks across people.
* - R6/R7/R8 are labelled by midpoints 2017/2019/2021. Age groups: all BUs;
*   reference-person ages 15-29, 30-54, 55-74, directly from dvage17 bands.
* - Means, prevalence and empirical quantiles use dwt. Amount distributions
*   condition on positive holders; prevalence/unconditional means include zeros.
* - Reconstruct the original survey price convention: nominal source balances
*   divided by INTERVIEW-year CPIH (ONS L522, 2015=100). Annual CPIH constants
*   are embedded below; no external price-index file is needed.
* - Only the annual summary statistics are interpolated for 2018/2020, as the
*   midpoint of adjacent observed-round estimates. Counts/weights stay missing.
* - Transitions link the same was_pid at adjacent rounds, with both records BU
*   reference persons and interviews exactly two years apart. Use origin age,
*   destination cross-sectional dwt; no estimation-sample restrictions.
* - The simulation's existing annual CPI (D7BT) constants are retained separately,
*   preserving the historical validator. Survey CPIH and simulation CPI are
*   different series; both conventions are explicit rather than silently changed.
*
* OUTPUTS: unsecured_debt_WAS_levels.dta (20 annual/age groups),
*   unsecured_debt_WAS_transitions.dta (8 interval/origin-age groups), and
*   unsecured_debt_cpi.dta (legacy simulation deflator). The source is read only.
*******************************************************************************/
version 18
set more off
args source_file output_dir
if `"`source_file'"' == "" local source_file "${file_WAS_debt}"
if `"`source_file'"' == "" local source_file "./survey_inputs/WAS/was_wealthdata_5_8_longitudinal.dta"
if `"`output_dir'"' == "" local output_dir "${dir_data}"
if `"`output_dir'"' == "" local output_dir "./data"
confirm file `"`source_file'"'
capture mkdir `"`output_dir'"'

* Survey CPIH annual averages (2015=100), ONS series L522, frozen historical
* values from the 20 March 2024 release; these reproduce the old WAS targets.
* https://www.ons.gov.uk/economy/inflationandpriceindices/timeseries/l522/mm23/previous/v103
tempfile survey_prices sample observed_levels transitions
clear
input int year double cpih
2016 101.0
2017 103.6
2018 106.0
2019 107.8
2020 108.9
2021 111.6
2022 120.5
end
save "`survey_prices'", replace

use `"`source_file'"', clear
confirm numeric variable was_pid was_round case bu bu_rp year dwt dvage17 highcost_debt_proxy
isid was_pid was_round
assert inlist(bu_rp,0,1)
assert missing(highcost_debt_proxy) if was_round==5
keep if inrange(was_round,6,8) & bu_rp==1
isid was_round case bu
assert year==floor(year) & year<.
assert highcost_debt_proxy>=0 if highcost_debt_proxy<.
* Eligibility here concerns validation coverage, not the regression sample.
keep if dwt>0 & dwt<. & highcost_debt_proxy<.
assert dwt==floor(dwt)
quietly levelsof was_round, local(rounds)
if "`rounds'" != "6 7 8" {
    display as error "Supply observed WAS rounds 6, 7 and 8 with valid weighted BU records."
    exit 2000
}
merge m:1 year using "`survey_prices'", keep(master match) assert(match) nogen
gen double debt_2015 = highcost_debt_proxy*100/cpih
gen byte holder = highcost_debt_proxy>0
gen byte age_group = .
replace age_group=1 if inrange(dvage17,4,6)
replace age_group=2 if inrange(dvage17,7,11)
replace age_group=3 if inrange(dvage17,12,15)
* All-age statistics include other/unknown ages; do not invent exact ages.
save "`sample'", replace

tempname annual transition
postfile `annual' int year byte was_round byte age_group long was_n_total ///
    double was_weight_sum double was_prevalence double was_mean_all ///
    double was_mean double was_p50 double was_p90 double was_p99 using "`observed_levels'", replace
forvalues rr=6/8 {
    local yy=2005+2*`rr'
    forvalues ag=0/3 {
        local selected "was_round==`rr'"
        if `ag'>0 local selected "`selected' & age_group==`ag'"
        quietly count if `selected'
        local n=r(N)
        local weights=0
        foreach v in prevalence mean_all mean p50 p90 p99 {
            local `v'=.
        }
        if `n'>0 {
            quietly summarize dwt if `selected', meanonly
            local weights=r(sum)
            quietly summarize holder [fw=dwt] if `selected', meanonly
            local prevalence=r(mean)
            quietly summarize debt_2015 [fw=dwt] if `selected', meanonly
            local mean_all=r(mean)
            quietly count if `selected' & holder
            if r(N)>0 {
                quietly summarize debt_2015 [fw=dwt] if `selected' & holder, meanonly
                local mean=r(mean)
                quietly _pctile debt_2015 [fw=dwt] if `selected' & holder, p(50 90 99)
                local p50=r(r1)
                local p90=r(r2)
                local p99=r(r3)
            }
        }
        post `annual' (`yy') (`rr') (`ag') (`n') (`weights') ///
            (`prevalence') (`mean_all') (`mean') (`p50') (`p90') (`p99')
    }
}
postclose `annual'

* Use a complete year/age grid so interpolation never fills observed missing
* groups from distant rounds. Each interpolated value needs both adjacent rounds.
clear
set obs 20
gen int year=2017+floor((_n-1)/4)
gen byte age_group=mod(_n-1,4)
merge 1:1 year age_group using "`observed_levels'", assert(master match) nogen
gen byte was_interpolated=inlist(year,2018,2020)
foreach metric in prevalence mean_all mean p50 p90 p99 {
    bysort age_group (year): replace was_`metric' = ///
        (was_`metric'[_n-1]+was_`metric'[_n+1])/2 if was_interpolated
}
label define debt_age 0 "All ages" 1 "15-29" 2 "30-54" 3 "55-74", replace
label values age_group debt_age
char _dta[provenance] "Weighted observed WAS BU reference persons; rebuilt from longitudinal .dta, not Excel"
char _dta[input_file] `"`source_file'"'
char _dta[money_basis] "2015 GBP: interview-year nominal debt *100 / ONS L522 annual CPIH"
char _dta[time_basis] "R6/R7/R8 midpoints 2017/2019/2021; adjacent-summary interpolation 2018/2020"
sort year age_group
save `"`output_dir'/unsecured_debt_WAS_levels.dta"', replace

use "`sample'", clear
xtset was_pid was_round
gen byte previous_holder=L.holder
gen byte previous_age_group=L.age_group
gen int previous_year=L.year
postfile `transition' byte from_round byte to_round int start_year int end_year byte age_group ///
    long was_n_matched long was_n_entry_risk long was_n_persist_risk ///
    double was_entry double was_persistence using "`transitions'", replace
forvalues rr=7/8 {
    local first=2005+2*(`rr'-1)
    local last=2005+2*`rr'
    forvalues ag=0/3 {
        local selected "was_round==`rr' & !missing(previous_holder) & year-previous_year==2"
        if `ag'>0 local selected "`selected' & previous_age_group==`ag'"
        quietly count if `selected'
        local n=r(N)
        quietly count if `selected' & previous_holder==0
        local ne=r(N)
        quietly count if `selected' & previous_holder==1
        local np=r(N)
        local entry=.
        local persistence=.
        if `ne'>0 {
            quietly summarize holder [fw=dwt] if `selected' & previous_holder==0, meanonly
            local entry=r(mean)
        }
        if `np'>0 {
            quietly summarize holder [fw=dwt] if `selected' & previous_holder==1, meanonly
            local persistence=r(mean)
        }
        post `transition' (`rr'-1) (`rr') (`first') (`last') (`ag') ///
            (`n') (`ne') (`np') (`entry') (`persistence')
    }
}
postclose `transition'
use "`transitions'", clear
label define debt_age 0 "All ages" 1 "15-29" 2 "30-54" 3 "55-74", replace
label values age_group debt_age
assert was_n_matched==was_n_entry_risk+was_n_persist_risk
char _dta[provenance] "Adjacent observed WAS rounds linked by was_pid; BU reference records at both endpoints"
char _dta[input_file] `"`source_file'"'
char _dta[weight_basis] "Destination cross-sectional dwt; origin reference-person age; exact two-year interview gap"
sort end_year age_group
save `"`output_dir'/unsecured_debt_WAS_transitions.dta"', replace

* Preserve the original SIMULATION deflator, annual CPI (ONS D7BT, 2015=100).
* This is intentionally separate from the legacy survey CPIH convention above.
clear
input int year double cpi
2017 103.4
2018 105.9
2019 107.8
2020 108.7
2021 111.6
end
isid year
label var cpi "Legacy simulation annual CPI setting, 2015=100"
char _dta[source] "Annual CPI D7BT constants from historical simulation validator; survey uses CPIH L522"
save `"`output_dir'/unsecured_debt_cpi.dta"', replace
display as result "DEBT TARGETS READY FROM WAS DTA: 20 annual groups; 8 transition groups; no workbook required."
