/*******************************************************************************
* PROJECT: SimPaths UK - wealth-module validation
* AUTHOR: Liang Shi
* LAST UPDATE: 28 October 2026
* Run from the repository root:
*   do "validation/02_simulated_output_validation/do_files/00_master_wealth.do" "config.do"
* Copy wealth_validation_config.example.do to a LOCAL config and edit its paths.
* Reads completed simulations and supplied survey inputs; no model estimation
* or new simulation is launched. Each module can be enabled independently.
* The original 00_master.do can invoke this entry point after its existing work.
*******************************************************************************/
version 18
clear all
set more off
args config_file
if `"`config_file'"' == "" {
    display as error "Supply a configuration do-file; see wealth_validation_config.example.do."
    exit 198
}
confirm file `"`config_file'"'
* Avoid carrying module settings over from a previous invocation in this session.
capture macro drop wealth_*
do `"`config_file'"'
foreach setting in code_root output_root runs_file initial_year {
    if `"${wealth_`setting'}"' == "" {
        display as error "Missing configuration: wealth_`setting'."
        exit 198
    }
}
local code "${wealth_code_root}/do_files"
local output "${wealth_output_root}"
local data `"`output'/data"'
local graphs `"`output'/graphs"'
local enabled = 0
foreach module in consumption housing pensions debt {
    if "${wealth_`module'}" == "" global wealth_`module' 1
    if !inlist("${wealth_`module'}","0","1") {
        display as error "wealth_`module' must be 0 or 1."
        exit 198
    }
    local enabled = `enabled' + ${wealth_`module'}
}
if `enabled' == 0 exit 198
if missing(real("${wealth_initial_year}")) | ///
    real("${wealth_initial_year}") != floor(real("${wealth_initial_year}")) exit 198
* The default windows reproduce the original validation settings. Survey years
* for WAS preparation stay complete (2014-2022), preserving endpoint linkage.
foreach module in housing pensions debt {
    if "${wealth_`module'_last}" == "" global wealth_`module'_last 2021
}
if "${wealth_consumption_first}" == "" global wealth_consumption_first 2016
if "${wealth_consumption_last}" == "" global wealth_consumption_last 2023
if "${wealth_housing_first}" == "" global wealth_housing_first 2015
if "${wealth_pensions_first}" == "" global wealth_pensions_first 2015
if "${wealth_pensions_rate_first}" == "" global wealth_pensions_rate_first 2016
if "${wealth_debt_first}" == "" global wealth_debt_first 2017
confirm file "${wealth_runs_file}"
confirm file `"`code'/01_prepare_wealth_simulated_data.do"'
if ${wealth_consumption} confirm file "${wealth_LCF_dir}/lcfs_sorted.dta"
if ${wealth_housing} | ${wealth_pensions} | ${wealth_debt} ///
    confirm file "${wealth_WAS_file}"
if ${wealth_pensions} {
    if "${wealth_UKHLS_targets_file}" != "" confirm file "${wealth_UKHLS_targets_file}"
    else confirm file "${wealth_UKHLS_file}"
}
capture mkdir `"`output'"'
capture mkdir `"`data'"'
capture mkdir `"`graphs'"'
capture mkdir `"`output'/checks"'
capture log close wealth_validation
log using `"`output'/checks/wealth_validation.log"', text replace name(wealth_validation)

* 03 files prepare supplied survey data; model estimation remains upstream.
if ${wealth_consumption} {
    do `"`code'/03_create_LCF_validation_targets.do"' ///
        "${wealth_LCF_dir}" `"`data'"' 2015 2023
}
if ${wealth_housing} | ${wealth_pensions} {
    do `"`code'/03_create_WAS_validation_targets.do"' ///
        "${wealth_WAS_file}" `"`data'"' 2014 2022
}
local ukhls_targets `"`data'/ukhls_pension_targets.dta"'
if ${wealth_pensions} {
    if "${wealth_UKHLS_targets_file}" != "" {
        local ukhls_targets "${wealth_UKHLS_targets_file}"
    }
    else {
        do `"`code'/03_create_UKHLS_pension_validation_targets.do"' ///
            "${wealth_UKHLS_file}" `"`data'"' ${wealth_pensions_first} ${wealth_pensions_last}
    }
}
if ${wealth_debt} {
    do `"`code'/03_create_unsecured_debt_validation_targets.do"' ///
        "${wealth_WAS_file}" `"`data'"'
}
* BU stocks remain at BU level; Person pension stocks remain at person level.
* The separate importer avoids joining repeated BU wealth onto person records.
do `"`code'/01_prepare_wealth_simulated_data.do"' ///
    "${wealth_runs_file}" `"`data'"' "${wealth_initial_year}" ///
    ${wealth_consumption} ${wealth_housing} ${wealth_pensions} ${wealth_debt}

if ${wealth_consumption} {
    do `"`code'/04_19_plot_consumption.do"' ///
        `"`data'/lcfs_validation_sample.dta"' `"`data'/consumption_simulation_sample.dta"' ///
        `"`graphs'/consumption"' `"`data'"' ///
        ${wealth_consumption_first} ${wealth_consumption_last} ${wealth_initial_year}
}
if ${wealth_housing} {
    do `"`code'/04_20_plot_housing_mortgage.do"' ///
        `"`data'/was_bu_validation_sample.dta"' `"`data'/housing_simulation_sample.dta"' ///
        `"`graphs'/housing_mortgage"' `"`data'"' ///
        ${wealth_housing_first} ${wealth_housing_last} ${wealth_initial_year} "as_recorded"
}
if ${wealth_pensions} {
    do `"`code'/04_21_plot_pensions.do"' ///
        `"`ukhls_targets'"' `"`data'/was_validation_sample.dta"' ///
        `"`data'/pension_person_simulation_sample.dta"' `"`graphs'/pensions"' `"`data'"' ///
        ${wealth_pensions_first} ${wealth_pensions_last} ${wealth_pensions_rate_first}
}
if ${wealth_debt} {
    do `"`code'/04_22_plot_unsecured_debt.do"' ///
        `"`data'/unsecured_debt_WAS_levels.dta"' `"`data'/unsecured_debt_WAS_transitions.dta"' ///
        `"`data'/unsecured_debt_simulation_sample.dta"' `"`data'/unsecured_debt_cpi.dta"' ///
        `"`graphs'/unsecured_debt"' `"`data'"' ///
        ${wealth_debt_first} ${wealth_debt_last} ${wealth_initial_year}
}
display as result "WEALTH VALIDATION COMPLETE: configured modules, supplied survey targets and completed runs."
log close wealth_validation
