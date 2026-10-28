/*******************************************************************************
* PROJECT:      SimPaths UK
* SECTION:      Simulated output validation - survey target preparation
* OBJECT:       Prepare WAS validation microdata
* Authors:      Liang Shi, Justin van der Ven
* LAST UPDATE:  27 October 2026
*
* INPUT:  Base was_wealthdata_5_8_longitudinal.dta, compiled from WAS R5-R8,
*         plus an enriched longitudinal WAS file supplying the debt components.
*         Only debt fields are merged; base wealth/pension variables are retained.
* OUTPUT: was_validation_sample.dta (person records with sample flags), and
*         was_bu_validation_sample.dta (one reference record per benefit unit).
*
* This is the WAS counterpart of 03_create_UKHLS_validation_targets.do.
* It constructs comparable variables; later validation scripts calculate summary
* statistics, read completed simulations, and draw figures. No model is estimated
* or simulation launched here. The source dataset is never overwritten.
*
* RUN: do "03_create_WAS_validation_targets.do" "survey folder" "target folder"
* Optional third/fourth arguments select first/last INTERVIEW year (inclusive).
* Optional fifth argument gives the full enriched debt-data filename.
* With no arguments, use the globals below if defined by a validation master:
*   dir_WAS_data, dir_data, min_sim_year, max_sim_year, file_WAS_debt.
* Otherwise use the portable survey_inputs defaults in section 1.
*
* MEASUREMENT:
* - Person-level pension rates and BU-level wealth coexist in the source.
*   Use sample_person for personal statistics; use the BU output or sample_bu
*   for wealth. Repeated BU wealth on person rows must not be counted twice.
* - dwt is the original positive cross-sectional weight (already rounded/scaled
*   by the upstream compiler). Its scale cancels from means and proportions.
* - year is interview year; roundMidYear is a separate round-midpoint label.
*   No annual scaffold, interpolated records, or imputed ages are created.
* - Monetary levels remain in the source's INTERVIEW-YEAR pounds. Inspection of
*   the supplied compiler found no inflation adjustment. Do not label these as
*   2015 pounds; apply an agreed deflator later if a comparison requires one.
* - Rates have percentage units: 5 means 5%, NOT a fraction of 0.05.
*******************************************************************************/
version 18
set more off
args source_dir target_dir first_year last_year debt_file

/*******************************************************************************
* 1 - Directories and validation years
*******************************************************************************/
* Relative defaults assume the validation root is the working directory.
local validation_root "."
if `"`source_dir'"' == "" local source_dir "${dir_WAS_data}"
if `"`source_dir'"' == "" local source_dir `"`validation_root'/survey_inputs/WAS"'
if `"`target_dir'"' == "" local target_dir "${dir_data}"
if `"`target_dir'"' == "" local target_dir `"`validation_root'/data"'
* The debt file supplies components absent from the base pension compilation.
* Supply the full enriched file through the fifth argument or file_WAS_debt.
if `"`debt_file'"' == "" local debt_file "${file_WAS_debt}"
if `"`debt_file'"' == "" local debt_file ///
    `"`source_dir'/was_debt_components.dta"'
if "`first_year'" == "" local first_year "${min_sim_year}"
if "`last_year'" == "" local last_year "${max_sim_year}"
if "`first_year'" == "" local first_year 2014
if "`last_year'" == "" local last_year 2022
if missing(real("`first_year'"), real("`last_year'")) | ///
    real("`first_year'") > real("`last_year'") {
    display as error "Specify numeric first/last years in ascending order."
    exit 198
}
confirm file `"`source_dir'/was_wealthdata_5_8_longitudinal.dta"'
confirm file `"`debt_file'"'
capture mkdir `"`target_dir'"'
use `"`source_dir'/was_wealthdata_5_8_longitudinal.dta"', clear

/*******************************************************************************
* 2 - Check source structure before constructing any targets
*******************************************************************************/
confirm numeric variable was_pid was_round case bu bu_rp year dwt sex dvage17 ///
    na nk emp wealth tot_pen tot_open tot_pp dvhvalue main_mort ///
    pp_contrate ocdc_contrate_emee ocdc_contrate_emer earnings op_memb
isid was_pid was_round
assert inrange(was_round, 5, 8) & was_round == floor(was_round)
assert !missing(year, case, bu) & year == floor(year)
assert inlist(bu_rp, 0, 1)
* Check before year filtering: each source BU must have exactly one reference row.
* Restricting by interview year can leave some person rows without their reference
* row in that same year. The BU output below intentionally uses reference rows.
tempvar n_ref
bysort was_round case bu: egen `n_ref' = total(bu_rp == 1)
assert `n_ref' == 1
drop `n_ref'

* Attach debt components BEFORE filtering years, so the two complete source
* files must match. Synthetic person IDs are not sufficient on their own:
* verify household/BU identity and the canonical linkage key as well.
confirm string variable was_link_key
local debt_fields cc_debt cc_debt_trans cc_debt_pers hp_debt loan_debt ///
    loan_debt_exc_slc loan_debt_slc mail_debt highcost_loan_debt ///
    highcost_debt_proxy unsec_debt_total loan_doorstep loan_pawnbroker loan_payday
* Prevent overlapping source debt columns from silently taking precedence.
confirm new variable `debt_fields'
tempfile debt_components
preserve
use `"`debt_file'"', clear
confirm numeric variable was_pid was_round year case bu bu_rp sex `debt_fields'
confirm string variable was_link_key
isid was_pid was_round
keep was_pid was_round year case bu bu_rp sex was_link_key `debt_fields'
rename (year case bu bu_rp sex was_link_key) ///
    (debtSourceYear debtSourceCase debtSourceBu debtSourceRef debtSourceSex debtSourceLink)
save "`debt_components'", replace
restore
merge 1:1 was_pid was_round using "`debt_components'", assert(match) nogen
assert year == debtSourceYear & case == debtSourceCase & bu == debtSourceBu
assert bu_rp == debtSourceRef & sex == debtSourceSex
assert was_link_key == debtSourceLink
drop debtSourceYear debtSourceCase debtSourceBu debtSourceRef debtSourceSex debtSourceLink
* Source compilation deliberately leaves the high-cost proxy missing in R5.
assert missing(highcost_debt_proxy) if was_round == 5
keep if inrange(year, `first_year', `last_year')
assert _N > 0

/*******************************************************************************
* 3 - Identifiers, demographics and analysis weights
*******************************************************************************/
clonevar idPers = was_pid
clonevar statCollectionWave = was_round
* These grouped IDs identify cross-sectional units WITHIN this output only.
* For longitudinal checks link was_pid across adjacent was_round values.
* was_pid follows a person, so it is not a stable benefit-unit ID after a split.
egen long idHh = group(was_round case)
egen long idBu = group(was_round case bu)
gen int roundMidYear = 2005 + 2 * was_round
label var roundMidYear "WAS round midpoint: R5=2015, R6=2017, R7=2019, R8=2021"
clonevar dwt_valid = dwt
gen byte sample_person = dwt > 0 & dwt < .
gen byte sample_bu = sample_person & bu_rp == 1
label var sample_person "Valid positive weight; no imposed age or estimation screen"
label var sample_bu "One weighted reference-person row per benefit unit"
* The source supplies grouped age only; do not manufacture an exact age.
clonevar ageGroupWAS = dvage17
gen byte demMaleFlag = sex == 1 if inlist(sex, 1, 2)
clonevar demNAdult = na
clonevar demNChild = nk
gen byte valid_employed = emp if inlist(emp, 0, 1)

/*******************************************************************************
* 4 - Benefit-unit wealth stocks and holding indicators
*******************************************************************************/
* These are already BU totals. Do not sum them across members again.
clonevar valid_wealthTotValue = wealth
clonevar valid_wealthPensValue = tot_pen
clonevar valid_wealthPensOccValue = tot_open
clonevar valid_wealthPensPPValue = tot_pp
clonevar valid_wealthPrptyValue = dvhvalue
clonevar valid_wealthMortgageDebtValue = main_mort
gen double valid_wealthHousingNetValue = dvhvalue - main_mort
label var valid_wealthHousingNetValue "BU main-home value less mortgage, interview-year GBP"
gen byte valid_homeowner = dvhvalue > 0 if dvhvalue < .
gen byte valid_mortgagor = main_mort > 0 if main_mort < .
gen byte valid_pensHolder = tot_pen > 0 if tot_pen < .
* Preserve nonpositive equity among owners; negative net wealth is not a missing
* value code. No trimming, winsorisation, or positivity restriction is applied.
gen byte valid_nonpositiveEquity = valid_wealthHousingNetValue <= 0 ///
    if valid_homeowner == 1 & valid_wealthHousingNetValue < .
* An equal split is an allocation proxy for comparisons requiring personal wealth,
* not observed personal pension holdings. Use only where documented downstream.
gen double valid_pensEqualAdultValue = tot_pen / na if na > 0 & na < .
label var valid_pensEqualAdultValue "Proxy: BU pension stock divided by source adult count"

/*******************************************************************************
* 5 - Personal pension contribution targets (legacy validation definitions)
*******************************************************************************/
* Preserve the raw fields. Existing pension_wealth_surv2.do recodes missing
* contribution rates to zero, caps the PP rate at 100%, and defines a PP
* contributor only above GBP5,000 annual earnings. Make those choices explicit.
clonevar valid_contRatePPPct = pp_contrate
clonevar valid_contRateOPEePct = ocdc_contrate_emee
clonevar valid_contRateOPErPct = ocdc_contrate_emer
gen byte ratePPMissing = missing(pp_contrate)
gen byte rateOPEeMissing = missing(ocdc_contrate_emee)
gen byte rateOPErMissing = missing(ocdc_contrate_emer)
foreach rate in valid_contRatePPPct valid_contRateOPEePct valid_contRateOPErPct {
    replace `rate' = 0 if missing(`rate')
}
replace valid_contRatePPPct = 100 if valid_contRatePPPct > 100
* Occupational rates retain their source values; the legacy file does not cap them.
gen byte valid_ppContribFlag = valid_contRatePPPct > 0 & ///
    earnings > 5000 & earnings < .
gen byte valid_opContribFlag = valid_contRateOPEePct + valid_contRateOPErPct > 0
* Membership is reported membership, distinct from a positive DC contribution.
gen byte valid_opMemberFlag = op_memb if inlist(op_memb, 0, 1)
label var valid_ppContribFlag "PP rate positive and annual earnings >5000 nominal GBP"
label var valid_opContribFlag "Positive sum of employee/employer DC contribution rates"
label var valid_opMemberFlag "Reported occupational membership, including DB schemes"

/*******************************************************************************
* 6 - Unsecured debt: observed component balances and four-state status
*******************************************************************************/
* Use the definitions in 05 unsecured debt/unsecured_debt_incidence.do and
* unsecured_debt_amounts.do, without their regression complete-case screens.
* These are BU balances repeated across members, in source interview-year GBP.
* Student loans are excluded from the low-cost component. Explicit high-cost
* loans are removed from that component to avoid counting them twice.
gen double lowcost_debt = hp_debt + loan_debt_exc_slc - highcost_loan_debt
replace lowcost_debt = 0 if lowcost_debt < 0 & lowcost_debt < .
clonevar highcost_debt = highcost_debt_proxy
label var lowcost_debt "BU HP plus non-student loans less explicit high-cost loans"
label var highcost_debt "BU persistent-card, mail-order and explicit high-cost debt"

* Total debt has a broader boundary (including student loans and transient card
* balances); do not assume total equals the sum of these low/high-cost proxies.
gen byte debtTargetsAvailable = 1
clonevar valid_unsecDebtTotal = unsec_debt_total
clonevar valid_unsecDebtHighProxy = highcost_debt
clonevar valid_unsecDebtLowProxy = lowcost_debt
label var valid_unsecDebtTotal "BU total unsecured debt, interview-year GBP"
label var valid_unsecDebtHighProxy "BU high-cost debt proxy, interview-year GBP; R6-R8"
label var valid_unsecDebtLowProxy "BU low-cost debt proxy, interview-year GBP"

* Missing balances must stay missing, rather than being interpreted as no debt.
* In particular, R5 has no high-cost proxy; its four-state status is unknown.
gen byte valid_lowDebtHolder = lowcost_debt > 0 if lowcost_debt < .
gen byte valid_highDebtHolder = highcost_debt > 0 if highcost_debt < .
gen byte valid_unsecuredState = valid_lowDebtHolder + 2 * valid_highDebtHolder ///
    if !missing(valid_lowDebtHolder, valid_highDebtHolder)
label define was_debt_states 0 "No low/high-cost debt" 1 "Low-cost only" ///
    2 "High-cost only" 3 "Mixed low/high-cost", replace
label values valid_unsecuredState was_debt_states
label var valid_unsecuredState "Observed low/high-cost proxy state; missing if either unknown"

* Select reference-person records with observed outcomes when calculating BU
* statistics. Low-cost and total debt are available in R5 too; the joint state
* and high-cost sample begin in R6. No interpolation or estimation filter applies.
gen byte sample_debtTotal = sample_bu & valid_unsecDebtTotal < .
gen byte sample_debtLow = sample_bu & lowcost_debt < .
gen byte sample_debtHigh = sample_bu & highcost_debt < .
gen byte sample_debt = sample_bu & valid_unsecuredState < .
label var sample_debtTotal "BU reference sample with observed total unsecured debt"
label var sample_debtLow "BU reference sample with observed low-cost balance"
label var sample_debtHigh "BU reference sample with observed high-cost proxy (R6-R8)"
label var sample_debt "BU reference sample with both components and observed debt state"

/*******************************************************************************
* 7 - Save reusable microdata; aggregate statistics belong in comparison scripts
*******************************************************************************/
char _dta[target_source] "Compiled observed WAS R5-R8; no annual interpolation"
char _dta[money_basis] "Source interview-year GBP for core wealth stocks; no deflation here"
char _dta[rate_units] "Percentage points; raw source rates retained"
char _dta[bu_selection] "Use sample_bu==1; person-level wealth repeats within BU"
char _dta[debt_available] "1"
char _dta[debt_source] `"`debt_file'"'
char _dta[debt_match] "Full 1:1 was_pid/was_round; household, BU, year, reference, sex and linkage checked"
char _dta[debt_money_basis] "Interview-year GBP, no deflation; high-cost proxy missing in R5"
char _dta[debt_sample] "Use sample_debtTotal/Low/High or sample_debt for the joint state"
sort idPers statCollectionWave
save `"`target_dir'/was_validation_sample.dta"', replace
preserve
keep if sample_bu == 1
isid statCollectionWave case bu
save `"`target_dir'/was_bu_validation_sample.dta"', replace
restore
display as result "Saved WAS person and benefit-unit validation samples to `target_dir'."
