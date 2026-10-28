/*******************************************************************************
* PROJECT:      SimPaths UK - simulated output validation
* OBJECT:       Fixed UKHLS pension benchmarks supplied in the validation workbook
* AUTHOR:       Liang Shi
* LAST UPDATE:  27 October 2026
*
* The workbook's existing UKHLS survey results are treated as fixed benchmarks. This file
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
if `"`workbook_file'"' == "" local workbook_file "./survey_inputs/pension_validation.xlsx"
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
gen str16 benchmark_source = "UKHLS (Excel)"
char _dta[provenance] "Fixed supplied workbook targets: Sheet1!A19:K25; not rebuilt from microdata"
char _dta[input_workbook] `"`workbook_file'"'
char _dta[wealth_definition] "Legacy unweighted equal-adult allocation proxy, positive personal-wealth holders"
save `"`output_dir'/ukhls_pension_excel_targets.dta"', replace
export delimited using `"`output_dir'/ukhls_pension_excel_targets.csv"', replace
display as result "UKHLS EXCEL TARGETS READY: 70 supplied statistics, 2015-2021; workbook unchanged."
