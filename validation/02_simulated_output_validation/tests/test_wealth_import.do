/*******************************************************************************
* Synthetic integration checks for the manifest importer; no survey/model runs.
* Run from the repository root, or supply the validation-root argument.
* Covers same IDs across runs, explicit run labels, spaces/commas in paths,
* numerical precision, missing ages, person/BU separation and rejected manifests.
*******************************************************************************/
version 18
clear all
set more off
args validation_root
if `"`validation_root'"' == "" local validation_root "./validation/02_simulated_output_validation"
capture mkdir `"`validation_root'/checks"'
local work `"`validation_root'/checks/import_test"'
capture mkdir `"`work'"'
forvalues rr=1/2 {
    local directory`rr' `"`work'/run `rr', fixture"'
    capture mkdir `"`directory`rr''"'
    tempname f
    file open `f' using `"`directory`rr''/BenefitUnit.csv"', write text replace
    file write `f' "time,id_BenefitUnit,ageRefPerson,yDispMonth,xDiscConsumptionAnnual,wealthPrptyValue,wealthMortgageDebtValue,wealthUnsecuredDebtHighValue" _n
    file write `f' "2015,1,null,1234.123456789,0,200000,100000,0" _n
    file write `f' "2017,1,35,1500,12000,210000,90000,100" _n
    file close `f'
    file open `f' using `"`directory`rr''/Person.csv"', write text replace
    file write `f' "time,id_Person,idBu,labC4,contRateOPEe,contRateOPEr,contRatePP,wealthPensValue" _n
    file write `f' "2015,11,1,EmployedOrSelfEmployed,0.05,0.06,0,12345.123456789" _n
    file write `f' "2017,11,1,NotEmployed,0,0,0,15000" _n
    file close `f'
}
tempname f
file open `f' using `"`work'/runs.csv"', write text replace
file write `f' "run_id,csv_dir" _n
file write `f' `"101,"`directory1'""' _n
file write `f' `"202,"`directory2'""' _n
file close `f'
do `"`validation_root'/do_files/01_prepare_wealth_simulated_data.do"' ///
    `"`work'/runs.csv"' `"`work'"' 2015 1 1 1 1
use `"`work'/consumption_simulation_sample.dta"', clear
assert _N == 4 & inlist(run,101,202)
isid run year idBu
assert yDispMonth == float(1234.123456789) if year==2015
assert missing(ageRefPerson) if year==2015
use `"`work'/housing_simulation_sample.dta"', clear
assert _N==4
assert wealthPrptyValue == 200000 if year==2015
use `"`work'/pension_person_simulation_sample.dta"', clear
assert _N==4 & idPers==11
assert employed == (year==2015)
assert abs(wealthPensValue-12345.123456789) < 1e-10 if year==2015
assert contRateOPEe==0.05 if year==2015
use `"`work'/unsecured_debt_simulation_sample.dta"', clear
assert _N==4 & wealthUnsecuredDebtHighValue==100 if year==2017
capture noisily do `"`validation_root'/do_files/01_prepare_wealth_simulated_data.do"' ///
    `"`work'/runs.csv"' `"`work'"' 2017 0 0 0 1
assert _rc == 9
* Duplicate directory labels cannot masquerade as independent simulations.
file open `f' using `"`work'/duplicate.csv"', write text replace
file write `f' "run_id,csv_dir" _n
file write `f' `"101,"`directory1'""' _n
file write `f' `"202,"`directory1'""' _n
file close `f'
capture noisily do `"`validation_root'/do_files/01_prepare_wealth_simulated_data.do"' ///
    `"`work'/duplicate.csv"' `"`work'"' 2015 0 0 0 1
assert _rc == 459
display as result "WEALTH IMPORT CHECKS PASSED: labels, precision, missing ages, separate units and invalid manifests."
