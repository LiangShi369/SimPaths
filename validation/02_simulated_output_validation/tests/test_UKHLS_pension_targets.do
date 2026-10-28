/*******************************************************************************
* UKHLS pension targets: synthetic equivalence to pension_wealth_surv.do.
* No real UKHLS dataset, cloud file or workbook is read. Run from repo root or
* supply validation_root. The fixture includes a BU ID repeated across years,
* mixed-case names, missing inputs, unequal unused weights and tied wealth.
*******************************************************************************/
version 18
clear all
set more off
set type float
args validation_root
if `"`validation_root'"' == "" local validation_root "./validation/02_simulated_output_validation"
capture mkdir `"`validation_root'/checks"'
local work `"`validation_root'/checks/ukhls_pension_targets_test"'
capture mkdir `"`work'"'
capture log close _all
log using `"`work'/target_test.log"', text replace
clear
set obs 108
gen int stm=2014+floor((_n-1)/12)
gen byte pno=mod(_n-1,12)+1
gen double idbenefitunit=stm*100+ceil(pno/2)
replace idbenefitunit=99 if pno<=2
gen int dag=30+pno
replace dag=10 if pno==11
replace dag=17 if pno==12
replace dag=. if pno==11 & stm==2018
gen byte les_c4=cond(pno<=8,1,3)
gen double contrateopee=cond(pno<=8,.05,0)
gen double contrateoper=cond(pno<=8,.06,0)
gen double contratepp=cond(pno<=4,.03,0)
replace contrateopee=. if pno==9 & stm==2016
replace contratepp=. if pno==10 & stm==2017
gen double total_pensions=10000*ceil(pno/2)+500*(stm-2015)
replace total_pensions=. if pno==7 & stm==2019
gen double unused_weight=cond(pno==1,1000,1)
rename contrateopee contRateOPEe
rename total_pensions Total_Pensions
save `"`work'/UKHLS_pooled_ipop.dta"', replace
do `"`validation_root'/do_files/03_create_UKHLS_pension_validation_targets.do"' ///
    `"`work'/UKHLS_pooled_ipop.dta"' `"`work'"' 2015 2021

* Literal reference preparation and ten statistics, without its machine paths.
use `"`work'/UKHLS_pooled_ipop.dta"', clear
rename *, lower
gen opMemb=((contrateopee+contrateoper)>0.0)
gen ppMemb=(contratepp>0.0)
gen emp=(les_c4==1)
rename stm time
gen adulth=dag>17
gsort idbenefitunit pno
by idbenefitunit: egen na=sum(adulth)
gen wealthpensvalue=0
replace wealthpensvalue=total_pensions/na if adulth
matrix expected=J(7,10,.)
forvalues yy=2015/2021 {
    quietly summarize opMemb if time==`yy', meanonly
    matrix expected[`yy'-2014,1]=r(mean)
    quietly summarize ppMemb if time==`yy', meanonly
    matrix expected[`yy'-2014,2]=r(mean)
    quietly summarize opMemb if time==`yy' & emp, meanonly
    matrix expected[`yy'-2014,3]=r(mean)
    quietly summarize ppMemb if time==`yy' & emp, meanonly
    matrix expected[`yy'-2014,4]=r(mean)
    quietly summarize wealthpensvalue if wealthpensvalue>0 & time==`yy', meanonly
    matrix expected[`yy'-2014,5]=r(mean)
    quietly xtile pct1=wealthpensvalue if wealthpensvalue>0 & time==`yy', nq(5)
    forvalues qq=1/5 {
        quietly summarize wealthpensvalue if pct1==`qq', meanonly
        matrix expected[`yy'-2014,`qq'+5]=r(mean)
    }
    drop pct1
}
use `"`work'/ukhls_pension_targets.dta"', clear
assert _N==7 & n_all==12 & n_employed==8
sort year
local variables op_share pp_share op_employed_share pp_employed_share wealth_mean wealth_q1 wealth_q2 wealth_q3 wealth_q4 wealth_q5
forvalues column=1/10 {
    local v : word `column' of `variables'
    forvalues row=1/7 {
        local scale=cond(`column'<=4,100,1)
        if !missing(`v'[`row'],expected[`row',`column']) & ///
            abs(`v'[`row']/`scale'-expected[`row',`column'])>1e-12*max(1,abs(expected[`row',`column'])) ///
            display as error "Mismatch: `v', row `row': " %21.15g `v'[`row']/`scale' " vs " %21.15g expected[`row',`column']
        assert abs(`v'[`row']/`scale'-expected[`row',`column'])<=1e-12*max(1,abs(expected[`row',`column'])) | ///
            (missing(`v'[`row']) & missing(expected[`row',`column']))
    }
}
assert n_missing_op==1 if year==2016
assert n_missing_pp==1 if year==2017
assert n_missing_age==1 if year==2018
assert n_wealth_q1+n_wealth_q2+n_wealth_q3+n_wealth_q4+n_wealth_q5==n_wealth
use `"`work'/ukhls_pension_validation_sample.dta"', clear
assert _N==84
assert na==18 if idbenefitunit==99
assert opMemb==1 if pno==9 & year==2016
assert ppMemb==1 if pno==10 & year==2017
assert adulth==1 if pno==11 & year==2018
display as result "UKHLS MICRODATA CHECKS PASSED: all 70 reference statistics, full-pool grouping, missing semantics and counts."
log close
