/*******************************************************************************
* PROJECT:      SimPaths UK - simulated output validation
* OBJECT:       UKHLS pension contributor and personal-wealth targets
* AUTHOR:       Liang Shi
* SOURCE METHOD: Justin van de Ven, pension_wealth_surv.do, 12 August 2026
* LAST UPDATE:  28 October 2026
*
* Run: do "do_files/03_create_UKHLS_pension_validation_targets.do" "UKHLS_pooled_ipop.dta" "data"
* Optional arguments: source_file output_dir first_year last_year (2015-2021).
* This reads UKHLS MICRODATA. No Excel workbook or fixed workbook targets are used.
* Source variable names are converted to lower case as in the reference do-file.
*
* PRESERVED REFERENCE DEFINITIONS
* - Unweighted person statistics. OP contributor: contrateopee+contrateoper>0;
*   PP contributor: contratepp>0; employed: les_c4==1. No WAS earnings screen.
* - Adult: dag>17. Count adults by idbenefitunit ACROSS THE FULL POOLED FILE,
*   before year selection, exactly as in the reference. Do not silently change
*   the grouping to idbenefitunit/year or recompute it on a selected subperiod.
* - Personal wealth proxy: total_pensions / adult count for adults; zero for
*   children. The proxy is float, reproducing the reference's default storage.
* - Positive-holder mean and within-year unweighted own-wealth quintile means.
* - The reference's literal >0 and >17 comparisons are retained. In Stata,
*   numeric missing values sort above finite values: missing contributions/age
*   can therefore satisfy those comparisons. Annual missing-input counts are
*   saved for audit; no undocumented missing-to-zero or complete-case change.
* - Shares are saved as percentages for 04_21; wealth remains GBP as recorded.
*   No new deflator, survey weight, estimation restriction or simulation run.
*
* OUTPUTS: ukhls_pension_targets.dta (+ CSV), with annual means and actual counts;
*   ukhls_pension_validation_sample.dta, a thin person-level audit dataset.
* The input is read only. Constants and counts are not obtained from Excel.
*******************************************************************************/
version 18
set more off
args source_file output_dir first_year last_year
if `"`source_file'"' == "" & "${dir_UKHLS_data}" != "" ///
    local source_file "${dir_UKHLS_data}/UKHLS_pooled_ipop.dta"
if `"`source_file'"' == "" local source_file "./survey_inputs/UKHLS/UKHLS_pooled_ipop.dta"
if `"`output_dir'"' == "" local output_dir "${dir_data}"
if `"`output_dir'"' == "" local output_dir "./data"
if "`first_year'" == "" local first_year 2015
if "`last_year'" == "" local last_year 2021
foreach parameter in first_year last_year {
    if missing(real("``parameter''")) | real("``parameter''")!=floor(real("``parameter''")) exit 198
}
if `first_year'>`last_year' exit 198
confirm file `"`source_file'"'
capture mkdir `"`output_dir'"'
use `"`source_file'"', clear
rename *, lower
confirm variable idbenefitunit pno
confirm numeric variable stm dag les_c4 contrateopee contrateoper contratepp total_pensions
keep idbenefitunit pno stm dag les_c4 contrateopee contrateoper contratepp total_pensions
rename stm year
gen byte opMemb=((contrateopee+contrateoper)>0.0)
gen byte ppMemb=(contratepp>0.0)
gen byte emp=(les_c4==1)
gen byte adulth=dag>17
gsort idbenefitunit pno
by idbenefitunit: egen float na=sum(adulth)
gen float wealthpensvalue=0
replace wealthpensvalue=total_pensions/na if adulth
label var wealthpensvalue "Reference proxy: BU total pensions / full-pool adult count"
label var na "Adult count by idbenefitunit across full pooled source"
tempfile sample targets
save "`sample'", replace

tempname results
postfile `results' int year ///
    double op_share double pp_share double op_employed_share ///
    double pp_employed_share double wealth_mean double wealth_q1 ///
    double wealth_q2 double wealth_q3 double wealth_q4 ///
    double wealth_q5 ///
    long n_all long n_employed long n_wealth ///
    long n_wealth_q1 long n_wealth_q2 long n_wealth_q3 ///
    long n_wealth_q4 long n_wealth_q5 long n_missing_op ///
    long n_missing_pp long n_missing_age using "`targets'", replace
forvalues yy=`first_year'/`last_year' {
    quietly count if year==`yy'
    local n_all=r(N)
    if `n_all'==0 {
        display as error "No UKHLS person records for `yy'. Check the source and validation window."
        exit 2000
    }
    quietly count if year==`yy' & emp
    local n_employed=r(N)
    quietly count if year==`yy' & wealthpensvalue>0 & wealthpensvalue<.
    local n_wealth=r(N)
    quietly count if year==`yy' & missing(contrateopee,contrateoper)
    local n_missing_op=r(N)
    quietly count if year==`yy' & missing(contratepp)
    local n_missing_pp=r(N)
    quietly count if year==`yy' & missing(dag)
    local n_missing_age=r(N)
    quietly summarize opMemb if year==`yy', meanonly
    local op=100*r(mean)
    quietly summarize ppMemb if year==`yy', meanonly
    local pp=100*r(mean)
    quietly summarize opMemb if year==`yy' & emp, meanonly
    local op_emp=100*r(mean)
    quietly summarize ppMemb if year==`yy' & emp, meanonly
    local pp_emp=100*r(mean)
    quietly summarize wealthpensvalue if wealthpensvalue>0 & year==`yy', meanonly
    local wealth=r(mean)
    tempvar quintile
    if `n_wealth'>0 {
        quietly xtile `quintile'=wealthpensvalue if wealthpensvalue>0 & year==`yy', nq(5)
    }
    else gen byte `quintile'=.
    forvalues qq=1/5 {
        quietly summarize wealthpensvalue if `quintile'==`qq', meanonly
        local q`qq'=r(mean)
        local n_q`qq'=r(N)
    }
    post `results' (`yy') (`op') (`pp') (`op_emp') (`pp_emp') ///
        (`wealth') (`q1') (`q2') (`q3') (`q4') (`q5') ///
        (`n_all') (`n_employed') (`n_wealth') (`n_q1') (`n_q2') (`n_q3') (`n_q4') (`n_q5') ///
        (`n_missing_op') (`n_missing_pp') (`n_missing_age')
    drop `quintile'
}
postclose `results'
use "`targets'", clear
isid year
gen str16 benchmark_source="UKHLS"
char _dta[provenance] "Rebuilt from UKHLS_pooled_ipop.dta following pension_wealth_surv.do, 12 Aug 2026"
char _dta[input_file] `"`source_file'"'
char _dta[units] "Contributor shares percent; wealth GBP as recorded; unweighted actual person counts"
char _dta[wealth_definition] "Positive holders of float equal-adult proxy; adult counts across full pooled idbenefitunit"
sort year
save `"`output_dir'/ukhls_pension_targets.dta"', replace
export delimited using `"`output_dir'/ukhls_pension_targets.csv"', replace
use "`sample'", clear
keep if inrange(year,`first_year',`last_year')
char _dta[provenance] "Thin UKHLS audit sample; adult counts were computed before year filtering"
char _dta[input_file] `"`source_file'"'
save `"`output_dir'/ukhls_pension_validation_sample.dta"', replace
display as result "UKHLS PENSION TARGETS READY FROM MICRODATA: `first_year'-`last_year'; no workbook required."
