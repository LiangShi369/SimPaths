/*******************************************************************************
* PROJECT:      SimPaths UK - simulated output validation
* OBJECT:       Fixed UKHLS pension benchmarks supplied in the validation workbook
* AUTHOR:       Liang Shi
* LAST UPDATE:  28 October 2026
*
* The user has accepted the workbook's UKHLS survey results as given. This file
* imports Sheet1!A19:K25; it does NOT reconstruct UKHLS microdata or estimate any
* process. Do not substitute WAS for these participation/personal-wealth targets.
* Their original definitions are in Pension Wealth/pension_wealth_surv.do:
*   OP: employee + employer rate > 0; PP: PP rate > 0; employment: les_c4 == 1.
*   Survey personal wealth proxy = total_pensions / source adult count (age>17),
*   restricted to positive holders for amount means and own-wealth quintiles.
*   These UKHLS statistics are unweighted. Counts and percentiles cannot be
*   recovered from aggregate means; leave those unavailable rather than inventing.
*
* Run from validation root; optional arguments: workbook_file output_data_dir
* The workbook is read only. Output rates are percentages; wealth is GBP in the
* workbook's recorded units, without a new price adjustment. Stored precision
* is the workbook's precision, not the missing source microdata's precision.
*******************************************************************************/
version 18
set more off
args workbook_file output_dir
if `"`workbook_file'"' == "" local workbook_file "./Pension Wealth/pension validation.xlsx"
if `"`output_dir'"' == "" local output_dir "${dir_data}"
if `"`output_dir'"' == "" local output_dir "./data"
confirm file `"`workbook_file'"'
capture mkdir `"`output_dir'"'
import excel using `"`workbook_file'"', sheet("Sheet1") cellrange(A19:K25) clear
rename (A B C D E F G H I J K) ///
    (year op_share pp_share op_employed_share pp_employed_share wealth_mean wealth_q1 wealth_q2 wealth_q3 wealth_q4 wealth_q5)
confirm numeric variable year op_share pp_share op_employed_share pp_employed_share ///
    wealth_mean wealth_q1 wealth_q2 wealth_q3 wealth_q4 wealth_q5
isid year
assert _N == 7
sort year
assert year == 2014 + _n
foreach v in op_share pp_share op_employed_share pp_employed_share {
    assert inrange(`v',0,1)
    replace `v' = 100 * `v'
    label var `v' "Fixed UKHLS workbook benchmark, percent"
}
foreach v of varlist wealth_* {
    assert `v' > 0 & `v' < .
    label var `v' "Fixed UKHLS personal-wealth benchmark, GBP as recorded"
}
* These aggregate targets cannot supply person counts. Missing means unknown,
* never a fabricated zero or an inferred sample size.
foreach v in n_all n_employed n_wealth n_wealth_q1 n_wealth_q2 n_wealth_q3 n_wealth_q4 n_wealth_q5 n_missing_op n_missing_pp n_missing_age {
    gen long `v' = .
}
gen str16 benchmark_source = "UKHLS"
char _dta[benchmark_basis] "fixed_excel"
char _dta[provenance] "Fixed supplied workbook targets: Sheet1!A19:K25; not rebuilt from microdata"
char _dta[input_workbook] `"`workbook_file'"'
char _dta[wealth_definition] "Legacy unweighted equal-adult allocation proxy, positive personal-wealth holders"
save `"`output_dir'/ukhls_pension_targets_fixed_excel.dta"', replace
export delimited using `"`output_dir'/ukhls_pension_targets_fixed_excel.csv"', replace
display as result "UKHLS EXCEL TARGETS READY: 70 supplied statistics, 2015-2021; workbook unchanged."
