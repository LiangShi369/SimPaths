***************************************************************************************
* PROJECT:       SimPaths UK — mortgage holding entry/persistence model
* DO-FILE:       mortgage_entry_persistence.do
* DESCRIPTION:   Estimates separate entry and persistence logits for mortgage holding
*                among homeowners, using WAS rounds 5–8 panel data (biennial transitions).
*
*                Per v04 analysis plan, mortgage entry is asymmetric:
*
*                Entry logit:       P(mortgage at t | no mortgage at t-1, homeowner at t)
*                  — includes a NEW HOMEOWNER indicator for individuals who
*                    transitioned from non-owner to owner between waves.
*                    Near-mechanical: new homeowners almost always take a mortgage.
*
*                Persistence logit: P(mortgage at t | mortgage at t-1, homeowner at t)
*
*                Both models condition on CURRENT homeownership (homeowner=1 at t).
*                This is because mortgage holding is meaningless for non-owners.
*
*                Annual transition probabilities recovered via Markov
*                annualisation algebra (same as homeownership model).
*
* AUTHORS:       Hrushi
* LAST UPDATE:   2026-04-14
*
* REQUIRES:      was_wealthdata_5_8_longitudinal.dta
***************************************************************************************

cap log close
clear all
set more off
set maxvar 10000

*----------------------------------------------------------------------
*  GLOBALS
*----------------------------------------------------------------------
global dir_data "/Users/hrushikeshkalakandra/Library/CloudStorage/Box-Box/CeMPA shared area/_SimPaths/_SimPathsUK/modules/wealth/00 exploratory analysis"
global dir_out  "${dir_data}/Mortgage debt/entry_persistence"
global file_input "${dir_data}/was_wealthdata_5_8_longitudinal.dta"
capture mkdir "${dir_out}"
capture confirm file "${file_input}"
if _rc {
    di as error "Input file not found: ${file_input}"
    exit 601
}

log using "${dir_out}/mortgage_entry_persistence.log", replace

*======================================================================
*  SECTION 1: LOAD DATA AND SET UP PANEL
*======================================================================

use "${file_input}", clear

foreach vv in was_pid was_round year bu_rp dwt dvage17 emp na nk nk04 ///
              gor2 grad pct sex inc dvhvalue main_mort {
    capture confirm variable `vv'
    if _rc {
        di as error "Required variable `vv' not found. Rerun wealth_data_merged.do first."
        exit 498
    }
}

keep if bu_rp == 1

capture confirm variable homeowner
if !_rc {
    capture drop homeowner_legacy_net_equity
    rename homeowner homeowner_legacy_net_equity
    label var homeowner_legacy_net_equity "Legacy homeowner flag: net housing equity != 0"
}

gen homeowner = (dvhvalue > 0 & !missing(dvhvalue))
label var homeowner "=1 if gross main-home value > 0"

gen single_female = (na == 1 & sex == 2)
label var single_female "=1 if single and female"

foreach vv in was_pid was_round year dwt dvage17 emp na nk nk04 gor2 grad ///
              single_female pct inc dvhvalue homeowner main_mort {
    drop if missing(`vv')
}
drop if dwt <= 0

gen inc_quintile = ceil(pct / 2)
label define inc_quintile_lbl 1 "Income quintile 1" 2 "Income quintile 2" ///
    3 "Income quintile 3" 4 "Income quintile 4" 5 "Income quintile 5", replace
label values inc_quintile inc_quintile_lbl
label var inc_quintile "Income quintile derived from income decile"

gen age50plus = dvage17
replace age50plus = 4 if age50plus < 4 & !missing(age50plus)
replace age50plus = 11 if age50plus > 11 & !missing(age50plus)
label define age50plus_lbl 4 "15-19" 5 "20-24" 6 "25-29" ///
    7 "30-34" 8 "35-39" 9 "40-44" 10 "45-49" 11 "50+", replace
label values age50plus age50plus_lbl
label var age50plus "Age band, top-coded at 50+"

* Calendar-year controls, matching the current H1 housing specifications.
tab year

* Panel setup
xtset was_pid was_round

*======================================================================
*  SECTION 2: CONSTRUCT MORTGAGE VARIABLES
*======================================================================

* Mortgage holding indicator.
* Define this for non-homeowners as 0 so new homeowners enter the mortgage
* entry sample with lagged mortgage status equal to 0 rather than missing.
gen has_mortgage = (main_mort > 0 & !missing(main_mort))
replace has_mortgage = 0 if homeowner == 0
replace has_mortgage = . if missing(homeowner)
label var has_mortgage "=1 if mortgage debt > 0; non-homeowners set to 0"

* Lagged mortgage status (biennial)
gen L_has_mortgage = L.has_mortgage
label var L_has_mortgage "Mortgage status at previous WAS wave"

gen L_main_mort = L.main_mort
label var L_main_mort "Mortgage debt at previous WAS wave"

gen L_inc = L.inc
label var L_inc "Annual private income at previous WAS wave"

gen L_mort_income_ratio = L_main_mort / L_inc ///
    if L_has_mortgage == 1 & L_inc > 0 & !missing(L_main_mort, L_inc)
label var L_mort_income_ratio "Lagged mortgage debt / lagged annual private income"

gen asinh_L_mort_income_ratio = ln(L_mort_income_ratio + sqrt(L_mort_income_ratio^2 + 1)) ///
    if !missing(L_mort_income_ratio)
label var asinh_L_mort_income_ratio "asinh(lagged mortgage debt / lagged annual private income)"

* Always regenerate the lag after overwriting the legacy homeownership flag.
capture drop L_homeowner
gen L_homeowner = L.homeowner
label var L_homeowner "Gross homeownership status at previous WAS wave"

* NEW HOMEOWNER indicator: owner now but not owner at previous wave
* This is the key asymmetric feature in v04.
* Near-mechanical link: new homeowners almost always take a mortgage
* simultaneously with purchase.
gen new_homeowner = (homeowner == 1 & L_homeowner == 0) if !missing(L_homeowner)
label var new_homeowner "=1 if transitioned to homeownership between waves"

* Continuing homeowner: owned at both waves
gen cont_homeowner = (homeowner == 1 & L_homeowner == 1) if !missing(L_homeowner)
label var cont_homeowner "=1 if homeowner at both current and previous wave"

*======================================================================
*  SECTION 3: PANEL COVERAGE DIAGNOSTICS
*======================================================================

disp _newline(2) "========================================================"
disp "PANEL COVERAGE DIAGNOSTICS — MORTGAGE MODEL"
disp "========================================================"

* Current homeowners with valid lag
count if homeowner == 1
local N_owners = r(N)
count if homeowner == 1 & !missing(L_homeowner)
local N_linked_owners = r(N)
disp "  All homeowners:                          `N_owners'"
disp "  Homeowners with valid lag (linked):       `N_linked_owners'"
disp "  Linkage rate among homeowners:            " %5.1f (`N_linked_owners'/`N_owners'*100) "%"

* Breakdown of new vs continuing homeowners
count if new_homeowner == 1
local N_new = r(N)
count if cont_homeowner == 1
local N_cont = r(N)
disp _newline "  New homeowners (entered between waves):   `N_new'"
disp "  Continuing homeowners (owned both waves): `N_cont'"

* Entry and persistence sample sizes
count if homeowner == 1 & L_has_mortgage == 0 & !missing(L_has_mortgage)
local N_entry_sample = r(N)
count if homeowner == 1 & L_has_mortgage == 1 & !missing(L_has_mortgage)
local N_persist_sample = r(N)
count if homeowner == 1 & L_has_mortgage == 1 & !missing(L_has_mortgage, asinh_L_mort_income_ratio)
local N_persist_burden_sample = r(N)
disp _newline "  Mortgage entry sample (owner now, no mortgage at t-1):  `N_entry_sample'"
disp "  Mortgage persist sample (owner now, mortgage at t-1):  `N_persist_sample'"
disp "  Mortgage persist sample with lagged burden available:  `N_persist_burden_sample'"

* New homeowners in the entry sample
count if new_homeowner == 1 & L_has_mortgage == 0 & !missing(L_has_mortgage)
local N_new_entry = r(N)
count if new_homeowner == 1 & L_has_mortgage == 1 & !missing(L_has_mortgage)
local N_new_persist = r(N)
disp _newline "  New homeowners in entry sample:    `N_new_entry'"
disp "  New homeowners in persist sample:  `N_new_persist' (should be ~0 — can't have mortgage lag if not owner)"

*======================================================================
*  SECTION 4: BIENNIAL TRANSITION MATRIX
*======================================================================

disp _newline(2) "========================================================"
disp "BIENNIAL TRANSITION MATRIX — MORTGAGE (homeowners only)"
disp "========================================================"

* Among current homeowners with valid lag
disp _newline "--- Unweighted (current homeowners only) ---"
tab L_has_mortgage has_mortgage if homeowner == 1 & !missing(L_has_mortgage), row

disp _newline "--- Weighted ---"
tab L_has_mortgage has_mortgage if homeowner == 1 & !missing(L_has_mortgage) [aweight=dwt], row nofreq

* Transition rates
sum has_mortgage if homeowner == 1 & L_has_mortgage == 0 & !missing(L_has_mortgage) [aweight=dwt]
local pi_e2 = r(mean)
disp _newline "  Biennial mortgage entry rate (no mort → mort):     " %7.4f `pi_e2'

sum has_mortgage if homeowner == 1 & L_has_mortgage == 1 & !missing(L_has_mortgage) [aweight=dwt]
local pi_p2 = r(mean)
disp "  Biennial mortgage persistence rate (mort → mort):  " %7.4f `pi_p2'
disp "  Biennial mortgage exit rate (mort → no mort):      " %7.4f (1 - `pi_p2')

* Entry rate by new vs continuing homeowner
disp _newline "--- Mortgage entry rate: new vs continuing homeowners ---"
tempfile mortgage_nh_check
tempname mortgage_nh_post
postfile `mortgage_nh_post' str35 measure str30 group double value double se double n using `mortgage_nh_check', replace

sum has_mortgage if new_homeowner == 1 & L_has_mortgage == 0 & !missing(L_has_mortgage) [aweight=dwt]
if r(N) > 0 {
    local entry_new = r(mean)
    local entry_new_n = r(N)
    disp "  New homeowners taking mortgage:       " %7.4f `entry_new' " (N=" `entry_new_n' ")"
    post `mortgage_nh_post' ("empirical_weighted_rate") ("new_homeowner") (`entry_new') (.) (`entry_new_n')
}
else {
    disp "  New homeowners in entry sample: 0 obs"
    post `mortgage_nh_post' ("empirical_weighted_rate") ("new_homeowner") (.) (.) (0)
}

sum has_mortgage if cont_homeowner == 1 & L_has_mortgage == 0 & !missing(L_has_mortgage) [aweight=dwt]
if r(N) > 0 {
    local entry_cont = r(mean)
    local entry_cont_n = r(N)
    disp "  Continuing owners taking new mortgage: " %7.4f `entry_cont' " (N=" `entry_cont_n' ")"
    post `mortgage_nh_post' ("empirical_weighted_rate") ("continuing_owner") (`entry_cont') (.) (`entry_cont_n')
}
else {
    disp "  Continuing owners in entry sample: 0 obs"
    post `mortgage_nh_post' ("empirical_weighted_rate") ("continuing_owner") (.) (.) (0)
}

*======================================================================
*  SECTION 5: ANNUALISATION
*======================================================================

disp _newline(2) "========================================================"
disp "ANNUALISATION: BIENNIAL → ANNUAL MORTGAGE TRANSITION RATES"
disp "========================================================"

local lambda_sq = `pi_p2' - `pi_e2'
disp "  λ² = π_p2 - π_e2 = " %7.4f `lambda_sq'

if `lambda_sq' < 0 {
    disp as err "  WARNING: λ² < 0 — entry exceeds persistence. Using fallback."
    local lambda = 0
    local p_e = `pi_e2' / 2
    local p_p = `pi_p2' / 2
}
else {
    local lambda = sqrt(`lambda_sq')
    local p_e = `pi_e2' / (1 + `lambda')
    local p_p = `p_e' + `lambda'
}

disp "  λ = " %7.4f `lambda'
disp _newline "  Annual mortgage entry probability (p_e):       " %7.4f `p_e'
disp "  Annual mortgage persistence probability (p_p): " %7.4f `p_p'
disp "  Annual mortgage exit probability (1-p_p):      " %7.4f (1 - `p_p')

* Verify
local pi_e2_check = `p_e' * (1 + `p_p' - `p_e')
local pi_p2_check = `p_p'^2 + `p_e' * (1 - `p_p')
disp _newline "  Verification:"
disp "  Reconstructed biennial entry:        " %7.4f `pi_e2_check' "  (original: " %7.4f `pi_e2' ")"
disp "  Reconstructed biennial persistence:  " %7.4f `pi_p2_check' "  (original: " %7.4f `pi_p2' ")"

*======================================================================
*  SECTION 6: ENTRY LOGIT — P(mortgage at t | no mortgage at t-1)
*======================================================================
*  Sample: current homeowners who did NOT have a mortgage at t-1.
*  Includes new_homeowner indicator per v04.

disp _newline(2) "========================================================"
disp "ENTRY LOGIT: P(has_mortgage=1 | L.has_mortgage=0, homeowner=1)"
disp "  Includes new_homeowner indicator (v04 specification)"
disp "========================================================"

count if homeowner == 1 & L_has_mortgage == 0 & !missing(L_has_mortgage)
local N_entry = r(N)
disp "  Entry sample N = `N_entry'"

* Check new_homeowner variation in entry sample
tab new_homeowner has_mortgage if homeowner == 1 & L_has_mortgage == 0 & !missing(L_has_mortgage), row

logit has_mortgage ///
    new_homeowner ///
    ib9.age50plus emp na nk nk04 i.gor2 grad single_female i.inc_quintile ///
    i.year ///
    if homeowner == 1 & L_has_mortgage == 0 & !missing(L_has_mortgage) ///
    [pweight=dwt], vce(robust)

estimates store mort_entry_logit

* Fitted probabilities within the actual new/continuing homeowner groups.
* These are the direct model-based counterparts to the empirical rates above.
predict double phat_mort_entry if e(sample), pr

sum phat_mort_entry if cont_homeowner == 1 & e(sample) [aweight=dwt]
if r(N) > 0 {
    post `mortgage_nh_post' ("model_fitted_group_mean") ("continuing_owner") (r(mean)) (.) (r(N))
}
else {
    post `mortgage_nh_post' ("model_fitted_group_mean") ("continuing_owner") (.) (.) (0)
}

sum phat_mort_entry if new_homeowner == 1 & e(sample) [aweight=dwt]
if r(N) > 0 {
    post `mortgage_nh_post' ("model_fitted_group_mean") ("new_homeowner") (r(mean)) (.) (r(N))
}
else {
    post `mortgage_nh_post' ("model_fitted_group_mean") ("new_homeowner") (.) (.) (0)
}

drop phat_mort_entry

* New-homeowner implementation check.
* Counterfactual predicted probabilities set the new-homeowner flag to 0/1
* for the same entry sample, holding the other covariates at observed values.
margins, at(new_homeowner=(0 1)) predict(pr)
matrix NHM = r(table)
local pred_cont = NHM[1,1]
local se_pred_cont = NHM[2,1]
local pred_new = NHM[1,2]
local se_pred_new = NHM[2,2]
post `mortgage_nh_post' ("model_predicted_probability") ("continuing_owner") (`pred_cont') (`se_pred_cont') (.)
post `mortgage_nh_post' ("model_predicted_probability") ("new_homeowner") (`pred_new') (`se_pred_new') (.)
postclose `mortgage_nh_post'

preserve
use `mortgage_nh_check', clear
export delimited using "${dir_out}/mortgage_ep_new_homeowner_check.csv", replace
restore

* Average marginal effects are not exported for the wealth-module tables.

*======================================================================
*  SECTION 7: PERSISTENCE LOGIT — P(mortgage at t | mortgage at t-1)
*======================================================================
*  Sample: current homeowners who HAD a mortgage at t-1.

disp _newline(2) "========================================================"
disp "PERSISTENCE LOGIT: P(has_mortgage=1 | L.has_mortgage=1, homeowner=1)"
disp "========================================================"

count if homeowner == 1 & L_has_mortgage == 1 & !missing(L_has_mortgage, asinh_L_mort_income_ratio)
local N_persist = r(N)
disp "  Persistence sample N = `N_persist'"

logit has_mortgage ///
    asinh_L_mort_income_ratio ///
    ib9.age50plus emp na nk nk04 i.gor2 grad single_female ///
    i.year ///
    if homeowner == 1 & L_has_mortgage == 1 & !missing(L_has_mortgage, asinh_L_mort_income_ratio) ///
    [pweight=dwt], vce(robust)

estimates store mort_persist_logit

* Average marginal effects are not exported for the wealth-module tables.

*======================================================================
*  SECTION 8: TRANSITION RATES BY SUBGROUP
*======================================================================

disp _newline(2) "========================================================"
disp "TRANSITION RATES BY SUBGROUP (weighted, homeowners only)"
disp "========================================================"

* By age band
disp _newline "--- Mortgage entry rate by age band ---"
tabstat has_mortgage if homeowner == 1 & L_has_mortgage == 0 & !missing(L_has_mortgage) [aweight=dwt], ///
    by(age50plus) stats(mean n) format(%9.4f)

disp _newline "--- Mortgage persistence rate by age band ---"
tabstat has_mortgage if homeowner == 1 & L_has_mortgage == 1 & !missing(L_has_mortgage) [aweight=dwt], ///
    by(age50plus) stats(mean n) format(%9.4f)

* By income group / lagged burden
disp _newline "--- Mortgage entry rate by income quintile ---"
tabstat has_mortgage if homeowner == 1 & L_has_mortgage == 0 & !missing(L_has_mortgage) [aweight=dwt], ///
    by(inc_quintile) stats(mean n) format(%9.4f)

disp _newline "--- Mortgage persistence rate by lagged mortgage-income burden quintile ---"
xtile mort_burden_q = L_mort_income_ratio if homeowner == 1 & L_has_mortgage == 1 ///
    & !missing(L_has_mortgage, L_mort_income_ratio), nq(5)
tabstat has_mortgage if homeowner == 1 & L_has_mortgage == 1 ///
    & !missing(L_has_mortgage, L_mort_income_ratio) [aweight=dwt], ///
    by(mort_burden_q) stats(mean n) format(%9.4f)

* By wave
disp _newline "--- Mortgage entry rate by wave ---"
tabstat has_mortgage if homeowner == 1 & L_has_mortgage == 0 & !missing(L_has_mortgage) [aweight=dwt], ///
    by(was_round) stats(mean n) format(%9.4f)

disp _newline "--- Mortgage persistence rate by wave ---"
tabstat has_mortgage if homeowner == 1 & L_has_mortgage == 1 & !missing(L_has_mortgage) [aweight=dwt], ///
    by(was_round) stats(mean n) format(%9.4f)

*======================================================================
*  SECTION 9: MODEL SUMMARY
*======================================================================

disp _newline(2) "========================================================"
disp "MODEL SUMMARY"
disp "========================================================"

estimates table mort_entry_logit mort_persist_logit, ///
    stats(N r2_p) b(%9.4f) se(%9.4f)

*======================================================================
*  SECTION 10: CSV EXPORT — COEFFICIENTS AND MODEL SUMMARY
*======================================================================

tempfile csv_coef_tmp csv_summ_tmp
tempname csv_post csv_summ_post bmat vmat

postfile `csv_post' ///
    str40 model str80 term double coef se z p ci_l ci_u ///
    using `csv_coef_tmp', replace

postfile `csv_summ_post' ///
    str40 model double n r2 log_likelihood chi2_or_F ///
    using `csv_summ_tmp', replace

foreach m in mort_entry_logit mort_persist_logit {
    capture estimates restore `m'
    if !_rc {
        matrix `bmat' = e(b)
        matrix `vmat' = e(V)
        local cnames : colfullnames `bmat'
        local k = colsof(`bmat')

        scalar n_m    = e(N)
        scalar r2_m   = .
        scalar ll_m   = .
        scalar chi2_m = .
        capture scalar r2_m   = e(r2_p)
        capture scalar ll_m   = e(ll)
        capture scalar chi2_m = e(chi2)

        post `csv_summ_post' ("`m'") (n_m) (r2_m) (ll_m) (chi2_m)

        forvalues j = 1/`k' {
            local term_j : word `j' of `cnames'
            scalar b_j  = `bmat'[1,`j']
            scalar s_j  = sqrt(`vmat'[`j',`j'])
            scalar z_j  = .
            scalar p_j  = .
            scalar lo_j = .
            scalar hi_j = .
            if s_j < . & s_j > 0 {
                scalar z_j  = b_j / s_j
                scalar p_j  = 2 * normal(-abs(z_j))
                scalar lo_j = b_j - invnormal(0.975) * s_j
                scalar hi_j = b_j + invnormal(0.975) * s_j
            }
            post `csv_post' ("`m'") ("`term_j'") (b_j) (s_j) (z_j) (p_j) (lo_j) (hi_j)
        }
    }
}

postclose `csv_post'
postclose `csv_summ_post'

* Add human-readable labels
preserve
use `csv_coef_tmp', clear
gen str120 term_label = term

* Strip equation prefix
replace term_label = subinstr(term_label, "has_mortgage:", "", 1)

* Mortgage-specific
replace term_label = "New homeowner (entered between waves)" if term_label == "new_homeowner"

* Age bands
replace term_label = "Age band: base (40-44)"     if regexm(term_label, "^[0-9]+b\.age50plus$")
replace term_label = "Age band: 15-19"            if term_label == "4.age50plus"
replace term_label = "Age band: 20-24"            if term_label == "5.age50plus"
replace term_label = "Age band: 25-29"            if term_label == "6.age50plus"
replace term_label = "Age band: 30-34"            if term_label == "7.age50plus"
replace term_label = "Age band: 35-39"            if term_label == "8.age50plus"
replace term_label = "Age band: 40-44"            if term_label == "9.age50plus"
replace term_label = "Age band: 45-49"            if term_label == "10.age50plus"
replace term_label = "Age band: 50+"              if term_label == "11.age50plus"

* Core covariates
replace term_label = "Employed"                   if term_label == "emp"
replace term_label = "Number of adults"           if term_label == "na"
replace term_label = "Number of children"         if term_label == "nk"
replace term_label = "Children aged 0-4"          if term_label == "nk04"
replace term_label = "Graduate"                   if term_label == "grad"
replace term_label = "Single female"              if term_label == "single_female"
replace term_label = "asinh(lagged mortgage debt / lagged annual private income)" ///
                                                    if term_label == "asinh_L_mort_income_ratio"
replace term_label = "Income quintile: base (1)"  if regexm(term_label, "^[0-9]+b\.inc_quintile$")
forvalues q = 2/5 {
    replace term_label = "Income quintile: `q'"   if term_label == "`q'.inc_quintile"
}
replace term_label = "Year: base (" + regexs(1) + ")" if regexm(term_label, "^([0-9]+)b\.year$")
replace term_label = "Year " + regexs(1)              if regexm(term_label, "^([0-9]+)\.year$")
replace term_label = "Constant"                   if term_label == "_cons"

* Regions
replace term_label = "Region: base (North East)"   if regexm(term_label, "^[0-9]+b\.gor2$")
replace term_label = "Region: North West"          if term_label == "2.gor2"
replace term_label = "Region: Yorkshire & Humber"  if term_label == "4.gor2"
replace term_label = "Region: East Midlands"       if term_label == "5.gor2"
replace term_label = "Region: West Midlands"       if term_label == "6.gor2"
replace term_label = "Region: East of England"     if term_label == "7.gor2"
replace term_label = "Region: London"              if term_label == "8.gor2"
replace term_label = "Region: South East"          if term_label == "9.gor2"
replace term_label = "Region: South West"          if term_label == "10.gor2"
replace term_label = "Region: Wales"               if term_label == "11.gor2"
replace term_label = "Region: Scotland"            if term_label == "12.gor2"
replace term_label = "Region: code " + regexs(1)  if regexm(term_label, "^([0-9]+)\.gor2$")

order model term term_label coef se z p ci_l ci_u
export delimited using "${dir_out}/mortgage_ep_model_results.csv", replace
restore

* Export model summary
preserve
use `csv_summ_tmp', clear
order model n r2 log_likelihood chi2_or_F
export delimited using "${dir_out}/mortgage_ep_model_summary.csv", replace
restore

*======================================================================
*  SECTION 11: VARIANCE-COVARIANCE MATRIX EXPORT
*======================================================================

foreach m in mort_entry_logit mort_persist_logit {
    capture estimates restore `m'
    if !_rc {
        matrix V_`m' = e(V)
        local vnames : colfullnames V_`m'
        local dim = colsof(V_`m')

        preserve
        clear
        set obs `dim'

        gen str80 variable = ""
        forvalues i = 1/`dim' {
            local vn : word `i' of `vnames'
            qui replace variable = "`vn'" in `i'
        }

        replace variable = subinstr(variable, "has_mortgage:", "", 1)

        forvalues j = 1/`dim' {
            local vn : word `j' of `vnames'
            local cname = subinstr("`vn'", "has_mortgage:", "", 1)
            local cname = subinstr("`cname'", ".", "_", .)
            local cname = subinstr("`cname'", ":", "_", .)
            gen double v_`cname' = .
            forvalues i = 1/`dim' {
                qui replace v_`cname' = V_`m'[`i',`j'] in `i'
            }
        }

        export delimited using "${dir_out}/mortgage_ep_vcv_`m'.csv", replace
        restore
    }
}

disp as txt "VCV exports written:"
disp as txt "  Entry logit:       ${dir_out}/mortgage_ep_vcv_mort_entry_logit.csv"
disp as txt "  Persistence logit: ${dir_out}/mortgage_ep_vcv_mort_persist_logit.csv"

*======================================================================
*  SECTION 12: SIMPATH IMPLEMENTATION NOTES
*======================================================================
*  Mortgage holding in SimPaths requires a two-gate check each year:
*
*  Gate 1: Is the individual a homeowner?
*    → Use homeownership entry/persistence model
*
*  Gate 2: Given homeowner, do they hold a mortgage?
*    → Use this model's entry/persistence logits
*    → Apply annualisation algebra (same as homeownership):
*       λ(X) = sqrt(π_p2(X) - π_e2(X))
*       p_e(X) = π_e2(X) / (1 + λ(X))
*       p_p(X) = p_e(X) + λ(X)
*
*  For NEW homeowners (just entered homeownership this period):
*    → The new_homeowner coefficient in the entry logit captures
*      the near-mechanical mortgage uptake at point of purchase.
*    → In SimPaths: if individual just transitioned to homeowner,
*      set new_homeowner=1 when evaluating the mortgage entry logit.
*      In subsequent years, new_homeowner=0.
*
*  Execution order within each annual step:
*    1. Evaluate homeownership (entry or persistence depending on L.homeowner)
*    2. If homeowner: evaluate mortgage (entry or persistence depending on L.mortgage)
*       — If new homeowner (just entered at step 1): use entry logit with new_homeowner=1
*       — If continuing homeowner with no mortgage: use entry logit with new_homeowner=0
*       — If continuing homeowner with mortgage: use persistence logit

disp _newline(2) "========================================================"
disp "IMPLEMENTATION NOTES"
disp "========================================================"
disp "  Mortgage model is conditional on homeownership."
disp "  SimPaths execution order: homeownership → mortgage → amounts"
disp "  New homeowner indicator = 1 only in the year of entry to ownership."
disp "  See Section 12 header comment for full details."

log close
disp "Done."
