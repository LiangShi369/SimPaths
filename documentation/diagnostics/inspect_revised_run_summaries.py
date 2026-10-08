"""Compare new inheritance runs with archived v3; write inspection JSON only.

Uses equally weighted benefit units and unscaled 2015-price amounts. Reads source
CSVs without modifying them. Annual flow/stock results remain descriptive.
"""
import argparse
import json
from pathlib import Path

import numpy as np
import pandas as pd


BU_COLUMNS = ['run', 'time', 'id_BenefitUnit', 'ageRefPerson', 'wealthTotValue',
              'wealthPrptyValue', 'wealthMortgageDebtValue', 'wealthPensValue',
              'wealthUnsecuredDebtHighValue', 'wealthUnsecuredDebtLowValue',
              'yDispMonth', 'xDiscConsumptionAnnual', 'yInvestAnnual', 'yPensionAnnual']


def summarize_units(frame):
    w = frame['wealthTotValue'].dropna().to_numpy()
    ordered = np.sort(w)
    n = len(ordered)
    total = ordered.sum()
    gini = (2 * np.dot(np.arange(1, n + 1), ordered) / (n * total) - (n + 1) / n) if total > 0 else None
    housing = frame['wealthPrptyValue'] - frame['wealthMortgageDebtValue']
    financial_residual = frame['wealthTotValue'] - housing - frame['wealthPensValue']
    income = frame['yDispMonth'] * 12
    consumption = frame['xDiscConsumptionAnnual']
    return {
        'units': n, 'mean_wealth': float(ordered.mean()), 'total_wealth': float(total),
        'wealth_percentiles': dict(zip(['P10', 'P25', 'P50', 'P75', 'P90'],
                                      map(float, np.quantile(ordered, [.1, .25, .5, .75, .9])))),
        'gini': float(gini) if gini is not None else None,
        'mean_housing_equity': float(housing.mean()),
        'mean_pension_pot': float(frame['wealthPensValue'].mean()),
        # This includes minor trusts until they are subtracted separately.
        'mean_financial_residual_including_trusts': float(financial_residual.mean()),
        'mean_unsecured_debt': float((frame['wealthUnsecuredDebtHighValue'] +
                                     frame['wealthUnsecuredDebtLowValue']).mean()),
        'mean_disposable_income': float(income.mean()),
        'mean_discretionary_consumption': float(consumption.mean()),
        'mean_recurring_budget_surplus': float((income - consumption).mean()),
        'mean_investment_income': float(frame['yInvestAnnual'].mean()),
        'mean_private_pension_income': float(frame['yPensionAnnual'].mean()),
        'age_known_units': int(frame['ageRefPerson'].notna().sum()),
    }


def inspect(folder):
    years = {}
    chunks_2070 = []
    all_years = []
    duplicate_keys = set()
    negative_stocks = {}
    for frame in pd.read_csv(folder / 'BenefitUnit.csv', usecols=BU_COLUMNS,
                             chunksize=150_000, na_values=['null']):
        amounts = frame[['wealthTotValue', 'wealthPensValue', 'wealthPrptyValue',
                         'wealthMortgageDebtValue', 'wealthUnsecuredDebtHighValue',
                         'wealthUnsecuredDebtLowValue']].to_numpy()
        assert np.isfinite(amounts).all(), 'Nonfinite or missing stock'
        for column in ['wealthPensValue', 'wealthPrptyValue', 'wealthMortgageDebtValue',
                       'wealthUnsecuredDebtHighValue', 'wealthUnsecuredDebtLowValue']:
            bad = frame.loc[frame[column].lt(-1e-6)]
            if len(bad):
                details = negative_stocks.setdefault(column, {'unit_years': 0, 'minimum': 0., 'sum': 0., 'by_year': {}})
                details['unit_years'] += len(bad)
                details['minimum'] = min(details['minimum'], float(bad[column].min()))
                details['sum'] += float(bad[column].sum())
                for year, group in bad.groupby('time'):
                    key = str(int(year))
                    row = details['by_year'].setdefault(key, {'units': 0, 'sum': 0., 'minimum': 0.})
                    row['units'] += len(group)
                    row['sum'] += float(group[column].sum())
                    row['minimum'] = min(row['minimum'], float(group[column].min()))
        assert not frame.duplicated(['run', 'time', 'id_BenefitUnit']).any()
        keys = set(zip(frame['run'], frame['time'], frame['id_BenefitUnit']))
        assert not (keys & duplicate_keys), 'Duplicate unit-year across chunks'
        duplicate_keys.update(keys)
        for year, group in frame.groupby('time'):
            accumulator = years.setdefault(int(year), {'n': 0, 'wealth': 0., 'pension': 0.,
                                                       'income': 0., 'consumption': 0.})
            accumulator['n'] += len(group)
            accumulator['wealth'] += float(group['wealthTotValue'].sum())
            accumulator['pension'] += float(group['wealthPensValue'].sum())
            accumulator['income'] += float((group['yDispMonth'] * 12).sum())
            accumulator['consumption'] += float(group['xDiscConsumptionAnnual'].sum())
        group = frame.loc[frame['time'].eq(2070)]
        if len(group):
            chunks_2070.append(group)
        selected = frame.loc[frame['time'].isin([2025, 2050])]
        if len(selected):
            all_years.append(selected)
    assert sorted(years) == list(range(2021, 2071)), 'Incomplete year coverage'
    units = pd.concat(chunks_2070, ignore_index=True)
    selected = pd.concat(all_years, ignore_index=True)
    result = {'csv_folder': str(folder),
              'negative_gross_stock_observations': negative_stocks,
              'method': (folder / 'inheritance_method.txt').read_text().strip(),
              'annual': {year: {'units': value['n'],
                               'mean_wealth': value['wealth'] / value['n'],
                               'mean_pension': value['pension'] / value['n'],
                               'total_wealth': value['wealth']} for year, value in sorted(years.items())},
              'selected_years': {str(year): summarize_units(group) for year, group in selected.groupby('time')},
              '2070_all_units': summarize_units(units)}
    result['2070_age_groups'] = {}
    age = units['ageRefPerson']
    definitions = {'24-54': age.between(24, 54), '55-74': age.between(55, 74),
                   'under65': age.lt(65), '65plus': age.ge(65)}
    for low, high in [(24,34), (35,44), (45,54), (55,64), (65,74), (75,84), (85,999)]:
        definitions[f'{low}-{high}' if high != 999 else '85plus'] = age.between(low, high)
    for name, mask in definitions.items():
        group = units.loc[mask]
        result['2070_age_groups'][name] = summarize_units(group)
        result['2070_age_groups'][name]['wealth_share_all_units'] = float(group['wealthTotValue'].sum() / units['wealthTotValue'].sum())
    balances = pd.read_csv(folder / 'inheritance_balances.csv')
    assert not balances.duplicated(['year', 'benefit_unit_id']).any()
    stocks = balances[['claim_assets_2015_gbp', 'claim_liabilities_2015_gbp', 'minor_trust_assets_2015_gbp', 'total_net_wealth_2015_gbp']]
    assert np.isfinite(stocks.to_numpy()).all()
    assert balances['minor_trust_assets_2015_gbp'].ge(-1e-6).all()
    closing = balances.loc[balances['year'].eq(2070)]
    joined = units.merge(closing, left_on='id_BenefitUnit', right_on='benefit_unit_id',
                         how='left', validate='one_to_one')
    assert joined['total_net_wealth_2015_gbp'].notna().all(), 'Unit absent from closing ledger'
    difference = joined['wealthTotValue'] - joined['total_net_wealth_2015_gbp']
    assert difference.abs().max() < 1e-5, 'Closing ledger disagrees with exported unit stock'
    result['closing_ledger_check'] = {'max_unit_wealth_difference': float(difference.abs().max()),
                                    'mean_minor_trust_per_exported_unit': float(joined['minor_trust_assets_2015_gbp'].mean())}
    transfers = pd.read_csv(folder / 'inheritance_transfers.csv')
    receipts = transfers.loc[transfers['asset'].eq('cash') & transfers['timing'].eq('immediate')]
    recent = receipts.loc[receipts['year'].between(2027, 2070)]
    result['cash_2027_2070'] = {'total': float(recent['value_2015_gbp'].sum()),
                               'pension': float(recent.loc[recent['reason'].str.startswith('unaccessed_pension'), 'value_2015_gbp'].sum())}
    if 'death_cause' in receipts.columns:
        result['cash_2027_2070']['by_cause'] = {name: float(group['value_2015_gbp'].sum()) for name, group in recent.groupby('death_cause')}
    unit_year = pd.MultiIndex.from_frame(units[['time', 'id_BenefitUnit']])
    result['_unit_keys'] = len(unit_year)
    return result


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--study-dir', type=Path, default=Path(
        'D:/BaiduSyncdisk/Job CeMPA/CeMPA papers/ageing_and_inequality/step1_wealth_income_inequality'))
    parser.add_argument('--output-json', type=Path, required=True)
    args = parser.parse_args()
    report = {}
    for version in ['simulation outcomes', 'simulation outcomes bequest v3']:
        root = args.study_dir / version
        baseline = [name for name in ['baseline', 'benchmark'] if (root / name / 'csv').exists()]
        if len(baseline) != 1:
            raise ValueError(f'Ambiguous/missing baseline in {root}: {baseline}')
        report[version] = {}
        for scenario in [baseline[0], 'counterfactual_no_ageing']:
            report[version][scenario] = inspect(root / scenario / 'csv')
            print(f'Inspected {version}/{scenario}', flush=True)
    args.output_json.write_text(json.dumps(report, indent=2), encoding='utf-8')
    print(f'Saved summary: {args.output_json}', flush=True)
