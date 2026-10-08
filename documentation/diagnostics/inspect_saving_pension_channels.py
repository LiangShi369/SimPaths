"""Read-only inspection of saved ageing runs for saving and pension channels.

Run with the bundled Python environment (pandas/numpy required). Nothing is
written to the input folders. Results printed as JSON are descriptive and
inherit the treatment recorded in each run's inheritance_method.txt.
"""
import argparse
import json
import math
from pathlib import Path

import pandas as pd


def year_rows(path, columns, year):
    parts = []
    for frame in pd.read_csv(path, usecols=columns, chunksize=150_000,
                             na_values=['null']):
        selected = frame.loc[frame['time'].eq(year)]
        if not selected.empty:
            parts.append(selected)
    if not parts:
        raise ValueError(f'No observations for {year} in {path}')
    return pd.concat(parts, ignore_index=True)


def saving_summary(units):
    # This is the annual recurring budget surplus, before retirement lump sums,
    # housing returns, pension contributions/stock changes and inheritance.
    input_units = len(units)
    units = units.dropna(subset=['yDispMonth', 'xDiscConsumptionAnnual',
                               'yInvestAnnual', 'yPensionAnnual'])
    income = units['yDispMonth'] * 12
    consumption = units['xDiscConsumptionAnnual']
    surplus = income - consumption
    result = {
        'input_units': input_units,
        'units': len(units),
        'missing_flow_units_excluded': input_units-len(units),
        'mean_disposable_annual': float(income.mean()),
        'mean_discretionary_consumption_annual': float(consumption.mean()),
        'mean_income_minus_discretionary_consumption': float(surplus.mean()),
        'median_income_minus_discretionary_consumption': float(surplus.median()),
        'positive_budget_surplus_share': float(surplus.gt(0).mean()),
        'budget_surplus_over_income_ratio_of_sums':
            float(surplus.sum() / income.sum()) if income.sum() != 0 else None,
        'mean_investment_income_annual': float(units['yInvestAnnual'].mean()),
        'mean_private_pension_income_annual': float(units['yPensionAnnual'].mean()),
        'mean_total_net_wealth': float(units['wealthTotValue'].mean()),
        'mean_income_less_investment_and_consumption':
            float((surplus - units['yInvestAnnual']).mean()),
    }
    return {key: (None if isinstance(value, float) and not math.isfinite(value)
                  else value) for key, value in result.items()}


def inspect(folder, year):
    units = year_rows(folder / 'BenefitUnit.csv',
                     ['run', 'time', 'id_BenefitUnit', 'ageRefPerson',
                      'yDispMonth', 'xDiscConsumptionAnnual', 'yInvestAnnual',
                      'yPensionAnnual', 'wealthTotValue'], year)
    people = year_rows(folder / 'Person.csv',
                      ['run', 'time', 'id_Person', 'idBu', 'demAge',
                       'demExitSample', 'labC4'], year)
    if units.duplicated(['run', 'id_BenefitUnit']).any():
        raise ValueError('Duplicate unit snapshots')
    if people.duplicated(['run', 'id_Person']).any():
        raise ValueError('Duplicate person snapshots')
    adults = people.loc[people['demAge'].ge(18) &
                        people['demExitSample'].eq('NotYet')].copy()
    adults['retired'] = adults['labC4'].eq('Retired')
    adults['nonretired'] = adults['labC4'].notna() & adults['labC4'].ne('Retired')
    status = adults.groupby(['run', 'idBu']).agg(
        adult_count=('id_Person', 'size'), retired_count=('retired', 'sum'),
        nonretired_count=('nonretired', 'sum'))
    units = units.merge(status, left_on=['run', 'id_BenefitUnit'],
                        right_index=True, how='left', validate='one_to_one')
    # End-year exports retain exited units until cleanup. Use the same current
    # population denominator as the refreshed presentation analysis.
    units = units.loc[units['adult_count'].ge(1)].copy()
    retired = units['adult_count'].ge(1) & units['adult_count'].eq(units['retired_count'])
    nonretired = units['nonretired_count'].ge(1)
    assert not (retired & nonretired).any()
    result = {
        'inheritance_method': (folder / 'inheritance_method.txt').read_text().strip(),
        'year': year,
        'groups': {
            'reference_age_under_65': saving_summary(units.loc[units['ageRefPerson'].between(18, 64)]),
            'reference_age_65plus': saving_summary(units.loc[units['ageRefPerson'].ge(65)]),
            'reference_age_24_54': saving_summary(units.loc[units['ageRefPerson'].between(24, 54)]),
            'reference_age_55_74': saving_summary(units.loc[units['ageRefPerson'].between(55, 74)]),
            'reference_age_75plus': saving_summary(units.loc[units['ageRefPerson'].ge(75)]),
            # Conservative retired-unit definition, not an inferred reference age.
            'all_living_members_18plus_retired': saving_summary(units.loc[retired]),
            'at_least_one_nonretired_adult': saving_summary(units.loc[nonretired]),
            'retirement_status_unresolved': saving_summary(units.loc[~retired & ~nonretired]),
            'single_living_adult_retired': saving_summary(
                units.loc[retired & units['adult_count'].eq(1)]),
        },
    }
    transfers = pd.read_csv(folder / 'inheritance_transfers.csv')
    period = transfers.loc[transfers['year'].between(2027, 2070)]
    pension = period['reason'].str.startswith('unaccessed_pension')
    cash = period['asset'].eq('cash') & period['timing'].eq('immediate')
    trusts = period['asset'].eq('minor_trust') & period['timing'].eq('deferred')
    result['payments_2027_2070'] = {
        'pension_immediate_cash': float(period.loc[pension & cash, 'value_2015_gbp'].sum()),
        'pension_new_minor_trust': float(period.loc[pension & trusts, 'value_2015_gbp'].sum()),
        'nonpension_immediate_cash': float(period.loc[~pension & cash, 'value_2015_gbp'].sum()),
    }
    return result


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--study-dir', type=Path, default=Path(
        'D:/BaiduSyncdisk/Job CeMPA/CeMPA papers/ageing_and_inequality/'
        'step1_wealth_income_inequality'))
    parser.add_argument('--year', type=int, default=2070)
    parser.add_argument('--outcomes-dir', default='simulation outcomes',
                        help='Folder within the study, including an archived version if needed')
    args = parser.parse_args()
    results = {}
    outcomes = args.study_dir / args.outcomes_dir
    baseline_names = [name for name in ['baseline', 'benchmark'] if (outcomes / name / 'csv').exists()]
    if len(baseline_names) != 1:
        raise ValueError(f'Expected one baseline/benchmark folder, found {baseline_names}')
    for scenario in [baseline_names[0], 'counterfactual_no_ageing']:
        results[scenario] = inspect(outcomes / scenario / 'csv', args.year)
        print(json.dumps({scenario: results[scenario]}, indent=2), flush=True)
