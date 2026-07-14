********************************************************************************
* PURPOSE:     Estimate the mortgage debt quantity specification and export
*              reusable CSV artefacts for reporting and SimPaths packaging.
*
* OUTPUTS:     Mortgage debt/continuous_regression/
*              mortgage_continuous_model_summary.csv
*              mortgage_continuous_coefficients.csv
*              mortgage_continuous_simulation_parameters.csv
*              mortgage_debt_continuous_regression.log
*
* NOTES:       The dependent variable is ln(mortgage debt), conditional on
*              positive mortgage debt. The target equations use asinh(net
*              housing wealth) and asinh(net non-pension wealth), following the
*              current Housing-Wealth specification.
********************************************************************************

cap log close
clear all
set more off
set maxvar 10000

global dir_base "/Users/hrushikeshkalakandra/Library/CloudStorage/Box-Box/CeMPA shared area/_SimPaths/_SimPathsUK/modules/wealth/00 exploratory analysis"
global file_input "${dir_base}/was_wealthdata_5_8_longitudinal.dta"
global dir_out "${dir_base}/Mortgage debt/continuous_regression"

capture mkdir "${dir_out}"

capture confirm file "${file_input}"
if _rc {
    di as error "Input file not found: ${file_input}"
    exit 601
}

capture program drop export_vcv
program define export_vcv
    args model outfile
    tempname vmat
    estimates restore `model'
    matrix `vmat' = e(V)
    local cnames : colfullnames `vmat'
    local k = colsof(`vmat')

    preserve
    clear
    set obs `k'
    gen str80 variable = ""

    forvalues i = 1/`k' {
        local row_i : word `i' of `cnames'
        replace variable = "`row_i'" in `i'
    }
    forvalues j = 1/`k' {
        local col_j : word `j' of `cnames'
        local out_j = strtoname("v_`col_j'")
        gen double `out_j' = .
        forvalues i = 1/`k' {
            replace `out_j' = `vmat'[`i',`j'] in `i'
        }
    }

    export delimited using "`outfile'", replace
    restore
end

log using "${dir_out}/mortgage_debt_continuous_regression.log", replace

use "${file_input}", clear

foreach vv in was_pid was_round year bu_rp dwt dvage17 emp na nk nk04 gor2 ///
              grad pct sex wealth tot_open tot_pp dvhvalue main_mort {
    capture confirm variable `vv'
    if _rc {
        di as error "Required variable `vv' not found. Rerun wealth_data_merged.do first."
        exit 498
    }
}

keep if bu_rp == 1

gen single_female = (na == 1 & sex == 2)

foreach vv in was_pid was_round year dwt dvage17 emp na nk nk04 gor2 grad ///
              single_female pct wealth tot_open tot_pp dvhvalue main_mort {
    drop if missing(`vv')
}
drop if dwt <= 0

gen Age29Under = (dvage17 <= 6) if !missing(dvage17)
gen Age30to34 = (dvage17 == 7) if !missing(dvage17)
gen Age35to39 = (dvage17 == 8) if !missing(dvage17)
gen Age40to44 = (dvage17 == 9) if !missing(dvage17)
gen Age45to49 = (dvage17 == 10) if !missing(dvage17)
gen Age50to54 = (dvage17 == 11) if !missing(dvage17)
gen Age55to59 = (dvage17 == 12) if !missing(dvage17)
gen Age60to64 = (dvage17 == 13) if !missing(dvage17)
gen Age65to69 = (dvage17 == 14) if !missing(dvage17)
gen Age40to49 = (dvage17 == 9 | dvage17 == 10) if !missing(dvage17)
gen Age50to59 = (dvage17 == 11 | dvage17 == 12) if !missing(dvage17)

gen inc_quintile = ceil(pct / 2)
label define inc_quintile_lbl 1 "Income quintile 1" 2 "Income quintile 2" ///
    3 "Income quintile 3" 4 "Income quintile 4" 5 "Income quintile 5", replace
label values inc_quintile inc_quintile_lbl
label var inc_quintile "Income quintile derived from income decile"

gen TW = wealth - tot_open - tot_pp
gen house_value = dvhvalue
gen mort_debt = main_mort
gen RW = house_value - mort_debt

gen homeowner_gross = (house_value > 0 & !missing(house_value))
gen has_mortgage = (mort_debt > 0 & !missing(mort_debt))
replace has_mortgage = 0 if homeowner_gross == 0
replace has_mortgage = . if missing(homeowner_gross)

sort was_pid was_round
by was_pid (was_round): gen P_was_round = was_round[_n-1] ///
    if was_round == was_round[_n-1] + 1

local pair_vars year house_value mort_debt RW TW homeowner_gross has_mortgage ///
    dvage17 Age29Under Age30to34 Age35to39 Age40to44 Age45to49 ///
    Age50to54 Age55to59 Age60to64 Age65to69 Age40to49 Age50to59 ///
    emp na nk nk04 gor2 grad single_female pct inc_quintile dwt

foreach vv of local pair_vars {
    by was_pid (was_round): gen P_`vv' = `vv'[_n-1] ///
        if was_round == was_round[_n-1] + 1
}

keep if !missing(P_was_round)

gen new_homeowner = (homeowner_gross == 1 & P_homeowner_gross == 0) ///
    if !missing(homeowner_gross, P_homeowner_gross)
gen new_mortgage = (has_mortgage == 1 & P_has_mortgage == 0) ///
    if !missing(has_mortgage, P_has_mortgage)
gen continuing_mortgage = (has_mortgage == 1 & P_has_mortgage == 1) ///
    if !missing(has_mortgage, P_has_mortgage)

gen ln_mort = ln(mort_debt) if has_mortgage == 1
gen P_ln_mort = ln(P_mort_debt) if P_has_mortgage == 1

gen asinh_RW = .
replace asinh_RW = ln(RW + sqrt(RW^2 + 1)) if RW >= 0 & !missing(RW)
replace asinh_RW = -ln(-RW + sqrt(RW^2 + 1)) if RW < 0 & !missing(RW)

gen asinh_TW = .
replace asinh_TW = ln(TW + sqrt(TW^2 + 1)) if TW >= 0 & !missing(TW)
replace asinh_TW = -ln(-TW + sqrt(TW^2 + 1)) if TW < 0 & !missing(TW)

label var ln_mort "ln(mortgage debt)"
label var asinh_RW "asinh(net housing wealth)"
label var asinh_TW "asinh(net non-pension wealth)"

gen P_asinh_RW = .
replace P_asinh_RW = ln(P_RW + sqrt(P_RW^2 + 1)) if P_RW >= 0 & !missing(P_RW)
replace P_asinh_RW = -ln(-P_RW + sqrt(P_RW^2 + 1)) if P_RW < 0 & !missing(P_RW)

gen P_asinh_TW = .
replace P_asinh_TW = ln(P_TW + sqrt(P_TW^2 + 1)) if P_TW >= 0 & !missing(P_TW)
replace P_asinh_TW = -ln(-P_TW + sqrt(P_TW^2 + 1)) if P_TW < 0 & !missing(P_TW)

foreach rr in 2 4 5 6 7 8 9 10 11 12 {
    gen reg`rr' = (gor2 == `rr') if !missing(gor2)
    gen P_reg`rr' = (P_gor2 == `rr') if !missing(P_gor2)
}
foreach qq in 2 3 4 5 {
    gen inc_quintile`qq' = (inc_quintile == `qq') if !missing(inc_quintile)
    gen P_inc_quintile`qq' = (P_inc_quintile == `qq') if !missing(P_inc_quintile)
}
forvalues yy = 2017/2022 {
    gen year`yy' = (year == `yy') if !missing(year)
    gen P_year`yy' = (P_year == `yy') if !missing(P_year)
}

local cont_age_covars Age29Under Age30to34 Age35to39 Age40to44 Age45to49 ///
    Age50to54 Age55to59 Age60to64 Age65to69
local new_age_covars Age29Under Age30to34 Age35to39 Age40to49 Age50to59
local region_covars reg2 reg4 reg5 reg6 reg7 reg8 reg9 reg10 reg11 reg12
local income_covars inc_quintile2 inc_quintile3 inc_quintile4 inc_quintile5
local year_covars year2017 year2018 year2019 year2020 year2021 year2022
local cont_covars asinh_RW asinh_TW `cont_age_covars' emp na nk nk04 ///
    `region_covars' grad single_female `income_covars' `year_covars'
local new_covars asinh_RW asinh_TW new_homeowner `new_age_covars' emp na nk nk04 ///
    `region_covars' grad single_female `income_covars' `year_covars'

gen target_sample = ///
    has_mortgage == 1 & ///
    !missing(ln_mort, asinh_RW, asinh_TW, new_mortgage, new_homeowner, ///
             dvage17, emp, na, nk, nk04, gor2, grad, single_female, ///
             inc_quintile, year, dwt)
replace target_sample = 0 if missing(target_sample)

gen new_target_sample = target_sample == 1 & new_mortgage == 1
gen cont_target_sample = target_sample == 1 & continuing_mortgage == 1
replace cont_target_sample = 0 if missing(P_ln_mort)
foreach vv of local cont_covars {
    replace cont_target_sample = 0 if missing(`vv', P_`vv')
}

di _newline(2) "========================================================"
di "MORTGAGE DEBT CONTINUOUS-REGRESSION SAMPLES"
di "========================================================"
count if target_sample == 1
di "All positive mortgage holders:       " r(N)
count if new_target_sample == 1
local N_new = r(N)
di "New mortgage holders:                `N_new'"
count if cont_target_sample == 1
local N_cont = r(N)
di "Continuing mortgage holders:         `N_cont'"

di _newline(2) "========================================================"
di "STATIC TARGET EQUATION: NEW MORTGAGE HOLDERS"
di "========================================================"

reg ln_mort `new_covars' ///
    if new_target_sample == 1 [pweight=dwt], vce(cluster was_pid)

estimates store mort_new_target
scalar n_new_target = e(N)
scalar r2_new_target = e(r2)
scalar rmse_new_target = e(rmse)

di _newline(2) "========================================================"
di "TARGET EQUATION: CONTINUING MORTGAGE HOLDERS"
di "========================================================"

reg ln_mort `cont_covars' ///
    if cont_target_sample == 1 [pweight=dwt], vce(cluster was_pid)

estimates store mort_cont_target
scalar n_cont_target = e(N)
scalar r2_cont_target = e(r2)
scalar rmse_cont_target = e(rmse)

predict xb_cont if target_sample == 1, xb
gen uhat_cont = ln_mort - xb_cont if target_sample == 1
by was_pid (was_round): gen P_uhat_cont = uhat_cont[_n-1] ///
    if was_round == was_round[_n-1] + 1

gen persist_sample = cont_target_sample == 1 & !missing(uhat_cont, P_uhat_cont)
replace persist_sample = 0 if missing(persist_sample)

di _newline(2) "========================================================"
di "INITIAL RESIDUAL PERSISTENCE: CONTINUING MORTGAGE HOLDERS"
di "========================================================"
count if persist_sample == 1
local N_persist = r(N)
di "Residual-persistence diagnostic sample: `N_persist'"

reg uhat_cont P_uhat_cont if persist_sample == 1 [pweight=dwt], ///
    noconstant vce(cluster was_pid)

estimates store mort_cont_resid
scalar rho_2yr = _b[P_uhat_cont]
scalar sigma2_2yr = e(rmse)^2
scalar rho_1yr = .
scalar sigma2_1yr = .
scalar annualisation_defined = 0
scalar annualisation_stable = 0
local persist_note "Residual persistence not classified."

if rho_2yr < 0 {
    local persist_note "rho_2yr < 0, so square-root annualisation is undefined."
}
else {
    scalar rho_1yr = sqrt(rho_2yr)
    scalar sigma2_1yr = sigma2_2yr / (1 + rho_1yr^2)
    scalar annualisation_defined = 1

    if rho_1yr < 1 {
        scalar annualisation_stable = 1
        local persist_note "Residual persistence is admissible for a stable annual AR(1) correction."
    }
    else {
        local persist_note "rho_2yr implies annual rho >= 1; treat as diagnostic only."
    }
}

di _newline "--- Annualisation diagnostics ---"
di "  rho_2yr:               " %9.6f rho_2yr
di "  rho_1yr:               " %9.6f rho_1yr
di "  sigma2_2yr:            " %9.6f sigma2_2yr
di "  sigma2_1yr:            " %9.6f sigma2_1yr
di "  Annualisation defined: " %9.0f annualisation_defined
di "  Annualisation stable:  " %9.0f annualisation_stable
di "  Note: `persist_note'"

tempfile coef_tmp summary_tmp sim_tmp
tempname coef_post summary_post sim_post bmat vmat

postfile `summary_post' ///
    str40 model double n r2 rmse residual_persistence_n ///
    rho_2yr rho_annual sigma2_eta sigma2_eps_annual annualisation_stable ///
    str220 note ///
    using `summary_tmp', replace

post `summary_post' ///
    ("continuing_mortgage_target") ///
    (n_cont_target) (r2_cont_target) (rmse_cont_target) ///
    (`N_persist') (rho_2yr) (rho_1yr) (sigma2_2yr) (sigma2_1yr) ///
    (annualisation_stable) ///
    ("Target equation for continuing mortgage holders; residual persistence estimated from linked continuing holders")

post `summary_post' ///
    ("new_mortgage_target") ///
    (n_new_target) (r2_new_target) (rmse_new_target) ///
    (.) (.) (.) (.) (.) (.) ///
    ("Static target equation for new mortgage holders")

postclose `summary_post'

postfile `coef_post' ///
    str40 model ///
    str30 equation ///
    str80 parameter ///
    str80 coefficient_name ///
    str80 current_variable ///
    str80 lagged_variable ///
    double estimate std_error t_stat p_value ///
    using `coef_tmp', replace

foreach m in mort_cont_target mort_new_target mort_cont_resid {
    estimates restore `m'

    matrix `bmat' = e(b)
    matrix `vmat' = e(V)
    local cnames : colfullnames `bmat'
    local k = colsof(`bmat')
    scalar df_m = e(df_r)

    local model_name "`m'"
    local equation_name "target_beta"
    if "`m'" == "mort_new_target" local equation_name "static_beta"
    if "`m'" == "mort_cont_resid" local equation_name "residual_persistence"

    forvalues j = 1/`k' {
        local term_j : word `j' of `cnames'
        local current_j "`term_j'"
        local lagged_j ""
        if "`term_j'" == "_cons" local current_j "constant"
        if "`term_j'" == "P_uhat_cont" {
            local current_j "uhat_cont"
            local lagged_j "P_uhat_cont"
        }

        scalar b_j = `bmat'[1,`j']
        scalar s_j = sqrt(`vmat'[`j',`j'])
        scalar t_j = .
        scalar p_j = .

        if s_j < . & s_j > 0 {
            scalar t_j = b_j / s_j
            scalar p_j = 2 * ttail(df_m, abs(t_j))
        }

        post `coef_post' ///
            ("`model_name'") ///
            ("`equation_name'") ///
            ("`term_j'") ///
            ("`term_j'") ///
            ("`current_j'") ///
            ("`lagged_j'") ///
            (b_j) (s_j) (t_j) (p_j)
    }
}

postclose `coef_post'

postfile `sim_post' ///
    str40 quantity str120 transform ///
    double continuing_n new_n residual_persistence_n ///
    rho_2yr rho_annual sigma2_eta sigma2_eps_annual ///
    annualisation_defined annualisation_stable ///
    str220 note ///
    using `sim_tmp', replace

post `sim_post' ///
    ("mortgage_debt") ///
    ("ln mortgage debt target + residual AR(1) correction") ///
    (n_cont_target) (n_new_target) (`N_persist') ///
    (rho_2yr) (rho_1yr) (sigma2_2yr) (sigma2_1yr) ///
    (annualisation_defined) (annualisation_stable) ///
    ("`persist_note'")

postclose `sim_post'

preserve
use `summary_tmp', clear
export delimited using "${dir_out}/mortgage_continuous_model_summary.csv", replace
restore

preserve
use `coef_tmp', clear
gen str120 term_label = parameter
replace term_label = "asinh(net housing wealth)" if term_label == "asinh_RW"
replace term_label = "asinh(net non-pension wealth)" if term_label == "asinh_TW"
replace term_label = "New homeowner" if term_label == "new_homeowner"
replace term_label = "Lagged residual (two-year)" if term_label == "P_uhat_cont"
replace term_label = "Constant" if term_label == "_cons"
replace term_label = "Employed" if term_label == "emp"
replace term_label = "Number of adults" if term_label == "na"
replace term_label = "Number of children" if term_label == "nk"
replace term_label = "Children aged 0-4" if term_label == "nk04"
replace term_label = "Graduate" if term_label == "grad"
replace term_label = "Single female" if term_label == "single_female"
replace term_label = "Income quintile: base (1)" if regexm(term_label, "^[0-9]+b\.inc_quintile$")
forvalues q = 2/5 {
    replace term_label = "Income quintile: `q'" if term_label == "`q'.inc_quintile"
    replace term_label = "Income quintile: `q'" if term_label == "inc_quintile`q'"
}
replace term_label = "Age 29 and under" if term_label == "Age29Under"
replace term_label = "Age 30-34" if term_label == "Age30to34"
replace term_label = "Age 35-39" if term_label == "Age35to39"
replace term_label = "Age 40-44" if term_label == "Age40to44"
replace term_label = "Age 45-49" if term_label == "Age45to49"
replace term_label = "Age 50-54" if term_label == "Age50to54"
replace term_label = "Age 55-59" if term_label == "Age55to59"
replace term_label = "Age 60-64" if term_label == "Age60to64"
replace term_label = "Age 65-69" if term_label == "Age65to69"
replace term_label = "Age 40-49" if term_label == "Age40to49"
replace term_label = "Age 50-59" if term_label == "Age50to59"
replace term_label = "Region: North West" if term_label == "2.gor2"
replace term_label = "Region: Yorkshire and Humber" if term_label == "4.gor2"
replace term_label = "Region: East Midlands" if term_label == "5.gor2"
replace term_label = "Region: West Midlands" if term_label == "6.gor2"
replace term_label = "Region: East of England" if term_label == "7.gor2"
replace term_label = "Region: London" if term_label == "8.gor2"
replace term_label = "Region: South East" if term_label == "9.gor2"
replace term_label = "Region: South West" if term_label == "10.gor2"
replace term_label = "Region: Wales" if term_label == "11.gor2"
replace term_label = "Region: Scotland" if term_label == "12.gor2"
replace term_label = "Region: North West" if term_label == "reg2"
replace term_label = "Region: Yorkshire and Humber" if term_label == "reg4"
replace term_label = "Region: East Midlands" if term_label == "reg5"
replace term_label = "Region: West Midlands" if term_label == "reg6"
replace term_label = "Region: East of England" if term_label == "reg7"
replace term_label = "Region: London" if term_label == "reg8"
replace term_label = "Region: South East" if term_label == "reg9"
replace term_label = "Region: South West" if term_label == "reg10"
replace term_label = "Region: Wales" if term_label == "reg11"
replace term_label = "Region: Scotland" if term_label == "reg12"
replace term_label = "Year " + regexs(1) if regexm(term_label, "^year([0-9]+)$")
replace term_label = "Year: base (" + regexs(1) + ")" if regexm(term_label, "^([0-9]+)b\.year$")
replace term_label = "Year " + regexs(1) if regexm(term_label, "^([0-9]+)\.year$")
export delimited using "${dir_out}/mortgage_continuous_coefficients.csv", replace
restore

preserve
use `sim_tmp', clear
export delimited using "${dir_out}/mortgage_continuous_simulation_parameters.csv", replace
restore

export_vcv mort_cont_target "${dir_out}/mortgage_continuous_vcv_mort_cont_target.csv"
export_vcv mort_new_target "${dir_out}/mortgage_continuous_vcv_mort_new_target.csv"
export_vcv mort_cont_resid "${dir_out}/mortgage_continuous_vcv_mort_cont_resid.csv"

di _newline(2) "========================================================"
di "DONE"
di "========================================================"
di "Outputs written to ${dir_out}"
di "  mortgage_continuous_model_summary.csv"
di "  mortgage_continuous_coefficients.csv"
di "  mortgage_continuous_simulation_parameters.csv"
di "  mortgage_continuous_vcv_*.csv"

log close
